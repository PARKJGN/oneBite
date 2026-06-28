package app.adapter.out.summarizer

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * OpenAI 호환 Chat Completions 호출 — GLM(Zhipu z.ai/bigmodel)·Gemini(OpenAI 호환 레이어) 등.
 * SummarizerConfig가 onebite.summarizer=glm 일 때 생성한다. 키/엔드포인트/모델은 설정으로 주입.
 * 엔드포인트는 {baseUrl}/chat/completions, 인증은 Authorization: Bearer.
 *
 * disableThinking=true 면 추론형 모델(GLM-5.2 등)의 사고를 끈다 — 요약엔 불필요하고,
 * 켜두면 토큰 한도를 길게 채우며 생성 시간이 급증(타임아웃)하므로 배치에선 끄는 것이 안전.
 */
class OpenAiCompatChatClient(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String,
    private val maxTokens: Int,
    private val om: ObjectMapper,
    private val disableThinking: Boolean = false,
) : LlmChatClient {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun complete(prompt: String): String {
        check(apiKey.isNotBlank()) { "LLM API 키가 설정되지 않았습니다 (예: GLM_API_KEY)" }
        val payload = buildMap<String, Any> {
            put("model", model)
            put("max_tokens", maxTokens)
            put("temperature", 0.2)                                 // 요약 일관성
            put("response_format", mapOf("type" to "json_object"))   // JSON 강제(모델 미지원 시 제거 가능)
            if (disableThinking) put("thinking", mapOf("type" to "disabled")) // 추론형 모델 사고 비활성화
            put("messages", listOf(mapOf("role" to "user", "content" to prompt)))
        }
        val req = HttpRequest.newBuilder(URI("${baseUrl.trimEnd('/')}/chat/completions"))
            .timeout(Duration.ofSeconds(120))                       // 추론형 모델 여유(배치 허용)
            .header("Authorization", "Bearer $apiKey")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(payload)))
            .build()
        val res = client.send(req, HttpResponse.BodyHandlers.ofString())
        check(res.statusCode() in 200..299) { "LLM API 오류 ${res.statusCode()}: ${res.body()}" }
        val node = om.readTree(res.body())
        val choice = node.path("choices").firstOrNull() ?: error("응답에 choices 없음: ${res.body()}")

        // finish_reason 선검사: 잘림을 명확한 실패로 → 폴백 사다리로 연결
        check(choice.path("finish_reason").asText() != "length") {
            "LLM 응답이 max_tokens(${maxTokens})로 잘림 — 한도를 늘리거나 입력 기사를 줄이세요"
        }
        val text = choice.path("message").path("content").asText().orEmpty()
        check(text.isNotBlank()) { "LLM 응답 본문이 비어 있음: ${res.body()}" }
        return text
    }
}
