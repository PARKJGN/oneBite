package app.adapter.out.summarizer

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
 * 환각 방지: 항목은 제공된 기사(제목/출처/URL)만 사용하도록 지시하고, JSON으로만 응답받는다.
 * 품질 검증(ContentQuality)은 호출측(EditionGenerationService)에서 수행.
 */
class LlmEditionSummarizer(
    private val client: LlmChatClient,
    private val om: ObjectMapper,
) : SummarizerPort {

    override fun summarize(input: SummarizeInput): EditionContent {
        val raw = client.complete(buildPrompt(input))
        val json = stripFences(raw)
        val dto = om.readValue<LlmEditionDto>(json)
        return EditionContent(
            oneLine = dto.oneLine.trim(),
            marketSummary = dto.marketSummary.map { it.trim() }.filter { it.isNotBlank() },
            crossInsight = dto.crossInsight?.trim()?.takeIf { it.isNotBlank() },
            items = dto.items.map { EditionItem(it.title.trim(), it.source.trim(), it.url.trim(), it.categoryCode.trim()) },
            references = dto.references.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { dto.items.map { it.source }.distinct() },
        )
    }

    private fun buildPrompt(input: SummarizeInput): String {
        val lang = if (input.language == Language.EN) "English" else "Korean"
        val articles = input.articles.joinToString("\n") { "- [${it.categoryCode}] ${it.title} | ${it.source} | ${it.url}" }
        return """
            너는 뉴스 요약 에디터다. 아래 기사들로 한 슬롯의 뉴스레터를 작성한다.
            출력 언어: $lang. 반드시 아래 JSON 스키마로만 응답하라(설명·코드펜스 없이 JSON만):
            {
              "oneLine": "핵심을 한 문장으로 — '왜 중요한가'",
              "marketSummary": ["맥락과 영향을 설명하는 1~3개 단락"],
              "crossInsight": "카테고리가 2개 이상이고 실제 근거가 있을 때만 카테고리 간 연결. 근거 없으면 null. 억지 연결 금지",
              "items": [{"title":"기사 제목($lang)","source":"출처명","url":"원문 URL","categoryCode":"카테고리 코드"}],
              "references": ["출처명 목록"]
            }
            규칙:
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
        val crossInsight: String? = null,
        val items: List<ItemDto> = emptyList(),
        val references: List<String> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ItemDto(
        val title: String = "",
        val source: String = "",
        val url: String = "",
        val categoryCode: String = "",
    )
}
