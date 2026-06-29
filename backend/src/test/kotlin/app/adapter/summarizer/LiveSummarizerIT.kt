package app.adapter.summarizer

import app.adapter.out.summarizer.LlmChatClient
import app.adapter.out.summarizer.LlmEditionSummarizer
import app.adapter.out.summarizer.OpenAiCompatChatClient
import app.domain.model.Language
import app.domain.model.RawArticle
import app.application.port.out.SummarizeInput
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 실 LLM 연동 검증(라이브, GLM) — GLM_API_KEY 가 있을 때만 실행되고, 없으면 건너뛴다.
 * Docker/DB/Kafka 불필요 — 실제 요약 경로(프롬프트→LLM→JSON 파싱→ContentQuality)만 1회 호출.
 *
 *   GLM: GLM_API_KEY=... [GLM_BASE_URL=...] [GLM_MODEL=glm-5.2]
 *
 * 실행:  GLM_API_KEY=... GLM_MODEL=glm-5.2 ./gradlew cleanTest test --tests "app.adapter.summarizer.LiveSummarizerIT" -i
 */
class LiveSummarizerIT {

    private val om = ObjectMapper().registerKotlinModule()

    @Test
    fun `실제 LLM 호출로 EditionContent를 생성하고 품질을 통과한다`() {
        val glmKey = System.getenv("GLM_API_KEY").orEmpty()
        assumeTrue(glmKey.isNotBlank(), "GLM_API_KEY 미설정 — 라이브 테스트 건너뜀")

        val base = System.getenv("GLM_BASE_URL")?.ifBlank { null } ?: "https://api.z.ai/api/paas/v4"
        val model = System.getenv("GLM_MODEL")?.ifBlank { null } ?: "glm-5.2"
        val provider = "GLM($model @ $base)"
        val client: LlmChatClient = OpenAiCompatChatClient(glmKey, base, model, 8192, om, disableThinking = true)
        val summarizer = LlmEditionSummarizer(client, om)

        // 정치+경제 2개 카테고리 → 교차 종합(crossInsight) 근거가 있으면 생성되는지도 함께 본다
        val input = SummarizeInput(
            categoryCodes = listOf("politics", "economy"),
            language = Language.KO,
            articles = listOf(
                RawArticle("정부, 부동산 대출 규제 추가 검토", "https://news.example/politics/1", "예시일보", Language.KO, "politics", Instant.now()),
                RawArticle("한국은행 기준금리 동결 결정", "https://news.example/economy/1", "예시경제", Language.KO, "economy", Instant.now()),
                RawArticle("BOK holds base rate steady", "https://news.example/economy/2", "Example Wire", Language.EN, "economy", Instant.now()),
            ),
        )

        val content = summarizer.summarize(input)

        println("=== provider: $provider ===")
        println(om.writerWithDefaultPrettyPrinter().writeValueAsString(content))

        assertDoesNotThrow { content.validate() }
        val inputUrls = input.articles.map { it.url }.toSet()
        check(content.allItems().isNotEmpty()) { "items 가 비어 있음" }
        content.allItems().forEach { check(it.url in inputUrls) { "지어낸 URL 발견: ${it.url}" } }
    }
}
