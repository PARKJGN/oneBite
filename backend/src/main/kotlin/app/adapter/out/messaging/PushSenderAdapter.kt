package app.adapter.out.messaging

import app.application.port.out.PushJob
import app.application.port.out.PushSender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 기본 푸시 발송(로깅) — dev/test. onebite.push=fcm 으로 전환하면 FcmPushSender가 대신 활성화된다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.push"], havingValue = "logging", matchIfMissing = true)
class LoggingPushSender : PushSender {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun send(job: PushJob): Boolean {
        log.info("PUSH-SEND user={} date={} editions={}", job.userId, job.issueDate, job.editionIds)
        return true
    }
}
