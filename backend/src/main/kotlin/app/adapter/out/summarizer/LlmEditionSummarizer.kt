package app.adapter.out.summarizer

import app.domain.model.CrossInsight
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.application.port.out.SummarizeInput
import app.application.port.out.SummarizerPort
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * LLM 기반 요약·번역·교차 종합(II·XII·XV) — 제공사 중립. SummarizerConfig가 LlmChatClient 빈이 있을 때 생성한다.
 * 복합정보(crossInsights): 카테고리 간 근거 있는 연결을 0~3개. 각 복합정보는 자기가 쓴 기사만 items 로 가진다.
 * 환각 방지: 항목은 제공된 기사(제목/출처/URL)만 사용. JSON 으로만 응답받는다.
 */
class LlmEditionSummarizer(
    private val client: LlmChatClient,
    private val om: ObjectMapper,
) : SummarizerPort {

    override fun summarize(input: SummarizeInput): EditionContent {
        val raw = client.complete(buildPrompt(input))
        val dto = om.readValue<LlmEditionDto>(stripFences(raw))
        val crossInsights = dto.crossInsights.map { ci ->
            CrossInsight(
                headline = ci.headline.trim(),
                body = ci.body.trim(),
                items = ci.items.map { EditionItem(it.title.trim(), it.source.trim(), it.url.trim(), it.categoryCode.trim()) },
            )
        }
        return EditionContent(
            oneLine = dto.oneLine.trim(),
            marketSummary = dto.marketSummary.map { it.trim() }.filter { it.isNotBlank() },
            crossInsights = crossInsights,
            references = dto.references.map { it.trim() }.filter { it.isNotBlank() }
                .ifEmpty { crossInsights.flatMap { it.items }.map { it.source }.distinct() },
        )
    }

    private fun buildPrompt(input: SummarizeInput): String {
        val lang = if (input.language == Language.EN) "English" else "Korean"
        val articles = input.articles.joinToString("\n") { "- [${it.categoryCode}] ${it.title} | ${it.source} | ${it.url}" }
        return """
            너는 뉴스 요약 에디터다. 아래 기사들로 한 슬롯의 뉴스레터를 작성한다.
            출력 언어: $lang. 반드시 아래 JSON 스키마로만 응답하라(설명·코드펜스 없이 JSON만):
            {
              "oneLine": "오늘 전체를 한 문장으로 — '왜 중요한가'",
              "marketSummary": ["전체 맥락과 영향을 설명하는 1~3개 단락"],
              "crossInsights": [
                {
                  "headline": "복합 테마 한 줄",
                  "body": "카테고리 간 '실제 근거 있는' 연결 설명",
                  "items": [{"title":"기사 제목($lang)","source":"출처명","url":"원문 URL","categoryCode":"카테고리 코드"}]
                }
              ],
              "references": ["출처명 목록"]
            }
            규칙:
            - crossInsights 는 주제별 인사이트 섹션을 최대 3개 만든다(각자 관련 기사 보유). 비어 있으면 안 된다(최소 1개).
              · 카테고리가 2개 이상이면: '실제 근거 있는' 카테고리 간 연결(복합정보)을 우선한다. 억지 연결 금지.
              · 카테고리가 1개면: 그 안에서 주제별로 묶어 섹션을 만든다(예: 정치 → '원구성 정국' / '외교·안보').
              · 의미 있는 묶음이 없으면 가장 중요한 기사들로 1개 섹션을 만든다.
            - 각 섹션의 items 에는 '그 섹션에 실제로 쓰인 기사'만 넣는다. 어느 섹션에도 안 쓰인 기사는 출력하지 않는다.
            - items 는 제공된 기사만 사용하고 url·source 는 그대로 유지한다(없는 기사·출처를 지어내지 말 것).
            - "큰 영향을 미칠 것으로 예상됩니다" 같은 공허한 상투구 금지. 구체적·검증 가능하게.
            - 제공된 기사가 타 언어면 $lang 로 번역해 작성한다.

            기사:
            $articles
        """.trimIndent()
    }

    private fun stripFences(s: String): String {
        val t = s.trim()
        if (!t.startsWith("```")) return t
        return t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LlmEditionDto(
        val oneLine: String = "",
        val marketSummary: List<String> = emptyList(),
        val crossInsights: List<CrossInsightDto> = emptyList(),
        val references: List<String> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CrossInsightDto(
        val headline: String = "",
        val body: String = "",
        val items: List<ItemDto> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ItemDto(
        val title: String = "",
        val source: String = "",
        val url: String = "",
        val categoryCode: String = "",
    )
}
