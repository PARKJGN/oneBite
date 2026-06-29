package app.adapter.out.summarizer

import app.application.port.out.SummarizerPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 요약 제공사 선택(onebite.summarizer): placeholder(기본) | glm.
 * 제공사별로 LlmChatClient 빈을 하나만 만들고, 그 빈이 있으면 LlmEditionSummarizer를 활성화한다.
 * (placeholder/미설정이면 LlmChatClient 빈이 없어 PlaceholderSummarizer가 그대로 쓰인다.)
 */
@Configuration
class SummarizerConfig {

    @Bean
    @ConditionalOnProperty(name = ["onebite.summarizer"], havingValue = "glm")
    fun glmChatClient(
        @Value("\${glm.api-key:}") apiKey: String,
        @Value("\${glm.base-url:https://api.z.ai/api/paas/v4}") baseUrl: String,
        @Value("\${glm.model:glm-5.2}") model: String,
        @Value("\${glm.max-tokens:8192}") maxTokens: Int,
        @Value("\${glm.disable-thinking:true}") disableThinking: Boolean,
        om: ObjectMapper,
    ): LlmChatClient = OpenAiCompatChatClient(apiKey, baseUrl, model, maxTokens, om, disableThinking)

    // LlmChatClient 빈이 존재할 때만(=claude 또는 glm) 실 요약기 활성화.
    @Bean
    @ConditionalOnBean(LlmChatClient::class)
    fun llmEditionSummarizer(client: LlmChatClient, om: ObjectMapper): SummarizerPort =
        LlmEditionSummarizer(client, om)
}
