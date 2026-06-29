package app.adapter.out.summarizer

/** LLM 메시지 호출 추상화(제공사 중립, 테스트 격리용). 구현: OpenAiCompatChatClient(GLM·Gemini 등 OpenAI 호환). */
fun interface LlmChatClient {
    fun complete(prompt: String): String
}
