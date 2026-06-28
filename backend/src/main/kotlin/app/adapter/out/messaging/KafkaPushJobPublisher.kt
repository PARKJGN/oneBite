package app.adapter.out.messaging

import app.domain.port.out.PushJob
import app.domain.port.out.PushJobPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Kafka 푸시 팬아웃(원칙 X): 발송 작업을 토픽에 발행 → consumer가 APNs/FCM 디스패치(재시도·백오프).
 * onebite.messaging.kafka=true 일 때 활성화(운영). consumer/PushSender는 후속 작업(T048 잔여).
 */
@Component
@ConditionalOnProperty(name = ["onebite.messaging.kafka"], havingValue = "true")
class KafkaPushJobPublisher(
    private val kafka: KafkaTemplate<String, String>,
    private val om: ObjectMapper,
) : PushJobPublisher {
    override fun publish(job: PushJob) {
        kafka.send(TOPIC, job.userId.toString(), om.writeValueAsString(job))
    }

    companion object { const val TOPIC = "onebite.push.jobs" }
}
