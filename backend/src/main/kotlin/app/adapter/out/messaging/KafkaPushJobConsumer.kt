package app.adapter.out.messaging

import app.domain.port.out.PushJob
import app.domain.port.out.PushSender
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka 푸시 잡 컨슈머(원칙 X): 발송 작업을 받아 PushSender(APNs/FCM)로 디스패치한다.
 * 실패 시 예외를 던져 Kafka 에러 핸들러의 지수 백오프 재시도를 트리거한다.
 * onebite.messaging.kafka=true 일 때 활성화.
 */
@Component
@ConditionalOnProperty(name = ["onebite.messaging.kafka"], havingValue = "true")
class KafkaPushJobConsumer(
    private val sender: PushSender,
    private val om: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaPushJobPublisher.TOPIC], groupId = "onebite-push")
    fun onMessage(payload: String) {
        val job = om.readValue<PushJob>(payload)
        val ok = sender.send(job)
        if (!ok) {
            log.warn("푸시 전송 실패 → 재시도 대상 user={}", job.userId)
            throw IllegalStateException("push send failed for user=${job.userId}")
        }
    }
}
