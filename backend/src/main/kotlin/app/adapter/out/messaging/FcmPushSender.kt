package app.adapter.out.messaging

import app.domain.port.out.DeviceTokenRepository
import app.domain.port.out.PushJob
import app.domain.port.out.PushSender
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.FileInputStream

/**
 * 실 FCM 전송(원칙 V). onebite.push=fcm 일 때 활성화. 서비스 계정 JSON(fcm.credentials, env FCM_CREDENTIALS)으로 인증.
 * job.userId의 등록 기기 토큰을 조회해 묶음 알림을 보낸다. 토큰이 없으면 skip(재시도 불필요 → true).
 */
@Component
@ConditionalOnProperty(name = ["onebite.push"], havingValue = "fcm")
class FcmPushSender(
    private val deviceTokens: DeviceTokenRepository,
    @Value("\${fcm.credentials:}") credentialsPath: String,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)
    private val messaging: FirebaseMessaging

    init {
        check(credentialsPath.isNotBlank()) { "FCM_CREDENTIALS(서비스 계정 JSON 경로)가 설정되지 않았습니다" }
        val options = FirebaseOptions.builder()
            .setCredentials(FileInputStream(credentialsPath).use { GoogleCredentials.fromStream(it) })
            .build()
        val app = if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options) else FirebaseApp.getInstance()
        messaging = FirebaseMessaging.getInstance(app)
    }

    override fun send(job: PushJob): Boolean {
        val tokens = deviceTokens.findActiveTokens(job.userId)
        if (tokens.isEmpty()) {
            log.info("FCM skip(등록 토큰 없음) user={}", job.userId)
            return true // 보낼 대상 없음 — 재시도해도 의미 없으므로 성공 처리
        }
        val title = "오늘의 한 입"
        val body = "${job.editionIds.size}개의 뉴스레터가 도착했어요"
        var anyOk = false
        for (t in tokens) {
            try {
                messaging.send(
                    Message.builder()
                        .setToken(t)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putData("issueDate", job.issueDate.toString())
                        .build(),
                )
                anyOk = true
            } catch (e: Exception) {
                log.warn("FCM 전송 실패 user={} token=…{} : {}", job.userId, t.takeLast(6), e.message)
            }
        }
        return anyOk // 하나도 못 보내면 false → Kafka 재시도(원칙 X)
    }
}
