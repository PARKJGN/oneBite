package app.adapter.out.summarizer

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Anthropic Messages API 호출(Claude). SummarizerConfig가 onebite.summarizer=claude 일 때 생성한다.
 * ANTHROPIC_API_KEY 필요. 모델 기본값은 Opus 4.8(요약 품질 우선) — anthropic.model 로 조절.
 */
class HttpAnthropicClient(
    private val apiKey: String,
    private val model: String,
    private val maxTokens: Int,
    private val om: ObjectMapper,
) : LlmChatClient {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun complete(prompt: String): String {
        check(apiKey.isNotBlank()) { "ANTHROPIC_API_KEY가 설정되지 않았습니다" }
        val body = om.writeValueAsString(
            mapOf(
                "model" to model,
                "max_tokens" to maxTokens,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            ),
        )
        val req = HttpRequest.newBuilder(URI("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(60))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val res = client.send(req, HttpResponse.BodyHandlers.ofString())
        check(res.statusCode() in 200..299) { "Anthropic API 오류 ${res.statusCode()}: ${res.body()}" }
        val node = om.readTree(res.body())

        // stop_reason 선검사: 잘림/거부를 모호한 JSON 파싱 오류 대신 명확한 실패로 처리 → 폴백 사다리로 연결
        when (node.path("stop_reason").asText()) {
            "max_tokens" -> error("Claude 응답이 max_tokens(${maxTokens})로 잘림 — 한도를 늘리거나 입력 기사를 줄이세요")
            "refusal" -> error("Claude가 요청을 거부함: ${node.path("stop_details").path("explanation").asText("(사유 없음)")}")
        }
        val text = node.path("content").firstOrNull()?.path("text")?.asText().orEmpty()
        check(text.isNotBlank()) { "Claude 응답 본문이 비어 있음: ${res.body()}" }
        return text
    }
}
