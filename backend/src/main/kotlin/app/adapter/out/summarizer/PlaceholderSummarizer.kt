package app.adapter.out.summarizer

import app.domain.model.CrossInsight
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.application.port.out.SummarizeInput
import app.application.port.out.SummarizerPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 기본 요약기(키 불필요) — 입력 기사로 결정적 콘텐츠를 구성하는 플레이스홀더(dev/test).
 * onebite.summarizer=glm 으로 전환하면 LlmEditionSummarizer(GLM)가 대신 활성화된다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.summarizer"], havingValue = "placeholder", matchIfMissing = true)
class PlaceholderSummarizer : SummarizerPort {
    override fun summarize(input: SummarizeInput): EditionContent {
        val items = input.articles.map {
            EditionItem(title = it.title, source = it.source, url = it.url, categoryCode = it.categoryCode)
        }
        val head = items.firstOrNull()?.title ?: "주요 소식"
        return EditionContent(
            oneLine = "오늘의 핵심: $head",
            marketSummary = listOf("선정된 ${items.size}건의 소식을 요약했습니다. (요약 엔진 연결 전 플레이스홀더)"),
            // 교차 종합은 LLM(GLM) 연결 시 근거 기반으로 생성 — 플레이스홀더는 단일 섹션으로 비지 않게 한다.
            crossInsights = listOf(
                CrossInsight(headline = "주요 소식", body = "선정된 소식 모음 (요약 엔진 연결 전 플레이스홀더)", items = items),
            ),
            references = items.map { it.source }.distinct(),
        )
    }
}
