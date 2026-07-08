package app.application

import app.domain.model.CrossInsight
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.EditionStatus
import app.domain.model.Language
import app.domain.model.RawArticle
import app.domain.service.Dedup
import app.domain.service.Ranking
import app.application.port.`in`.GenerateEditionsUseCase
import app.application.port.`in`.GenerationSummary
import app.application.port.out.DeliveryTargetQuery
import app.application.port.out.EditionRepository
import app.application.port.out.EventPublisher
import app.application.port.out.RawArticleStore
import app.application.port.out.SummarizeInput
import app.application.port.out.SummarizerPort
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 매일 1회 배치 생성(VIII·XIII): 구독 중인 (comboKey, language) 조합만 생성하고,
 * 이미 같은 키의 에디션이 있으면 재사용한다(중복 생성 금지, FR-015a/SC-008).
 *
 * 폴백 사다리(원칙 X): 조합별로 격리(try/catch)해 한 조합 실패가 배치 전체를 막지 않는다("성공 슬롯" 보존).
 *  AI 요약(+품질검증) → 실패 시 직전 발송분(FALLBACK_PREV) → 그것도 없으면 비-AI 헤드라인(FALLBACK_NON_AI)
 *  → 기사도 없으면 생성 생략(읽기 경로가 직전분/"준비 중"으로 처리). 각 단계는 관측 이벤트로 로깅.
 */
@Service
class EditionGenerationService(
    private val targets: DeliveryTargetQuery,
    private val editions: EditionRepository,
    private val rawArticles: RawArticleStore,
    private val summarizer: SummarizerPort,
    private val events: EventPublisher,
) : GenerateEditionsUseCase {

    override fun runForDate(issueDate: LocalDate): GenerationSummary {
        // 구독된 (comboKey, language) 조합만 — 수요 기반(XIII). 생성은 '구독(활성 슬롯)' 기준:
        // 푸시 동의 여부와 무관하게 생성해야 인앱 열람이 가능(발송만 동의 게이트).
        val needed: Set<Pair<String, Language>> = targets.findSubscribedTargets()
            .flatMap { t -> t.comboKeys.map { it to t.language } }
            .toSet()

        var generated = 0
        var reused = 0
        var fallback = 0
        // 뉴스 창: [어제 07:30 KST, 오늘 07:30 KST] — 8시 발송 다이제스트가 '직전 24h'를 다룬다.
        // 컷오프=07:30(content-ready-deadline). 에디션은 조합별 공유라 기준 TZ(한국 뉴스=KST)에 고정.
        val newsZone = java.time.ZoneId.of("Asia/Seoul")
        val cutoff = java.time.LocalTime.of(7, 30)
        val until = issueDate.atTime(cutoff).atZone(newsZone).toInstant()
        val since = issueDate.minusDays(1).atTime(cutoff).atZone(newsZone).toInstant()

        for ((comboKey, language) in needed) {
            try {
                if (editions.findByKey(comboKey, language, issueDate) != null) {
                    reused++; events.emit("cache_hit", mapOf("combo" to comboKey, "lang" to language.name)); continue
                }
                val categoryCodes = comboKey.split("+").filter { it.isNotBlank() }
                val articles = categoryCodes.flatMap { rawArticles.findWindow(it, since, until) }
                val top = Ranking.balancedTopClusters(Dedup.clusterByEvent(articles), categoryCodes)

                // 1) AI 요약 시도(+품질검증). 실패(호출 오류·품질 미달) 시 null → 폴백 사다리로.
                val aiContent: EditionContent? =
                    if (top.isEmpty()) null
                    else runCatching {
                        val c = summarizer.summarize(SummarizeInput(categoryCodes, language, top.map { it.representative }))
                        c.validate()
                        c
                    }.getOrElse { e ->
                        events.emit("ai_failure", mapOf("combo" to comboKey, "reason" to (e.message ?: "unknown")))
                        null
                    }

                // 2) 폴백 사다리: AI 성공 → 직전 발송분 → 비-AI 헤드라인 → (없으면 skip)
                val chosen: Pair<EditionContent, EditionStatus>? = when {
                    aiContent != null -> aiContent to EditionStatus.READY
                    else -> editions.findLatestBefore(comboKey, language, issueDate)
                        ?.let { it.content to EditionStatus.FALLBACK_PREV }
                        ?: top.takeIf { it.isNotEmpty() }
                            ?.let { nonAiHeadlines(it.map { c -> c.representative }, language) to EditionStatus.FALLBACK_NON_AI }
                }
                if (chosen == null) {
                    events.emit("failure", mapOf("reason" to "no_content", "combo" to comboKey)); continue
                }

                val (content, status) = chosen
                editions.save(Edition(null, comboKey, language, issueDate, content, status))
                if (status == EditionStatus.READY) {
                    generated++; events.emit("generation", mapOf("combo" to comboKey, "lang" to language.name))
                } else {
                    fallback++; events.emit("fallback", mapOf("combo" to comboKey, "status" to status.name))
                }
            } catch (e: Exception) {
                // 한 조합의 예기치 못한 실패가 나머지 조합 생성을 막지 않게 — 격리·로깅 후 계속
                events.emit("failure", mapOf("combo" to comboKey, "reason" to (e.message ?: "unknown")))
            }
        }
        return GenerationSummary(generated = generated, reused = reused, fallback = fallback)
    }

    /** 비-AI 폴백: 수집·랭킹된 기사 헤드라인만으로 콘텐츠 구성(요약 엔진 실패 시). 품질검증 대상 아님(의도된 저하). */
    private fun nonAiHeadlines(articles: List<RawArticle>, language: Language): EditionContent {
        val items = articles.map { EditionItem(it.title, it.source, it.url, it.categoryCode) }
        val note = if (language == Language.EN) {
            "AI summary is temporarily unavailable; showing source headlines only."
        } else {
            "요약 생성이 일시적으로 불가하여 출처 헤드라인만 제공합니다."
        }
        return EditionContent(
            oneLine = items.firstOrNull()?.title ?: note,
            marketSummary = listOf(note),
            // 폴백도 비지 않게 단일 '주요 소식' 섹션으로 헤드라인을 담는다(품질검증 대상 아님).
            crossInsights = listOf(CrossInsight(headline = "주요 소식", body = note, items = items)),
            references = items.map { it.source }.distinct(),
        )
    }
}
