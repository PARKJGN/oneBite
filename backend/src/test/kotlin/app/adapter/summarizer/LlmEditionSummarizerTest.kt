package app.adapter.summarizer

import app.adapter.out.summarizer.LlmChatClient
import app.adapter.out.summarizer.LlmEditionSummarizer
import app.domain.model.Language
import app.domain.model.RawArticle
import app.application.port.out.SummarizeInput
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class LlmEditionSummarizerTest {

    private val om = ObjectMapper().registerKotlinModule()

    private val input = SummarizeInput(
        categoryCodes = listOf("economy"),
        language = Language.KO,
        articles = listOf(RawArticle("Rate frozen", "https://x/a", "Yonhap", Language.EN, "economy", Instant.now())),
    )

    @Test
    fun `LLM JSON 응답을 EditionContent로 파싱하고 품질을 통과한다`() {
        val json = """
            {
              "oneLine": "한국은행이 기준금리를 동결했다",
              "marketSummary": ["이자 부담의 추가 상승이 멈췄다", "시장은 연내 인하 시점에 주목한다"],
              "crossInsights": [
                {
                  "headline": "금리 동결의 파장",
                  "body": "이자 부담 상승이 멈추며 시장이 인하 시점에 주목한다",
                  "items": [{"title":"기준금리 동결","source":"연합뉴스","url":"https://x/a","categoryCode":"economy"}]
                }
              ],
              "references": ["연합뉴스"]
            }
        """.trimIndent()
        val summarizer = LlmEditionSummarizer(LlmChatClient { json }, om)

        val content = summarizer.summarize(input)

        assertEquals("한국은행이 기준금리를 동결했다", content.oneLine)
        assertEquals(1, content.crossInsights.size)
        assertEquals(1, content.allItems().size)
        assertEquals("https://x/a", content.allItems().first().url)
        assertDoesNotThrow { content.validate() }
    }

    @Test
    fun `코드펜스로 감싼 JSON도 처리한다`() {
        val fenced = "```json\n{\"oneLine\":\"x\",\"marketSummary\":[\"y\"]," +
            "\"crossInsights\":[{\"headline\":\"h\",\"body\":\"b\"," +
            "\"items\":[{\"title\":\"t\",\"source\":\"s\",\"url\":\"u\",\"categoryCode\":\"economy\"}]}],\"references\":[\"s\"]}\n```"
        val summarizer = LlmEditionSummarizer(LlmChatClient { fenced }, om)
        val content = summarizer.summarize(input)
        assertEquals("x", content.oneLine)
    }
}
