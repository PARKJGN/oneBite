package app.adapter.out.messaging

import app.adapter.out.metrics.MetricsRegistry
import app.domain.port.out.EventPublisher
import app.domain.port.out.PushJob
import app.domain.port.out.PushJobPublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 기본(비-Kafka) 푸시 발행: 로깅. onebite.messaging.kafka=false(기본)에서 사용 — dev/테스트.
 * 운영에서는 onebite.messaging.kafka=true로 KafkaPushJobPublisher 활성화.
 */
@Component
@ConditionalOnProperty(name = ["onebite.messaging.kafka"], havingValue = "false", matchIfMissing = true)
class LoggingPushJobPublisher : PushJobPublisher {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun publish(job: PushJob) {
        log.info("PUSH-JOB user={} date={} editions={}", job.userId, job.issueDate, job.editionIds)
    }
}

/** 관측 이벤트(FR-017) — 로깅 + 인메모리 집계(MetricsRegistry). TODO: Kafka 토픽/시계열 저장소 연결. */
@Component
class LoggingEventPublisher(private val metrics: MetricsRegistry) : EventPublisher {
    private val log = LoggerFactory.getLogger("metrics")
    override fun emit(type: String, attributes: Map<String, Any?>) {
        log.info("EVENT type={} attrs={}", type, attributes)
        metrics.record(type, attributes)
    }
}
