package app.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

/**
 * Kafka 컨슈머 에러 처리(원칙 X): 지수 백오프 재시도. 최대 재시도 후엔 로깅 후 스킵
 * (DLT 연결은 후속). onebite.messaging.kafka=true 일 때만 구성.
 */
@Configuration
@ConditionalOnProperty(name = ["onebite.messaging.kafka"], havingValue = "true")
class KafkaConfig {

    @Bean
    fun pushErrorHandler(): DefaultErrorHandler {
        val backOff = ExponentialBackOff(1000L, 2.0).apply {
            maxInterval = 30_000
            maxElapsedTime = 120_000 // 약 4~5회 재시도 후 중단
        }
        return DefaultErrorHandler(backOff)
    }
}
