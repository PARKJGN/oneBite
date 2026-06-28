package app.adapter.out.security

import app.domain.TooManyLoginAttemptsException
import app.domain.port.out.EmailSender
import app.domain.port.out.LoginAttemptGuard
import app.domain.port.out.PasswordResetTokenStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 인메모리 재설정 토큰 저장(단일 사용·30분 만료). MVP용.
 * TODO: 다중 인스턴스/재시작 대응을 위해 DB(또는 Redis) 저장으로 교체.
 */
@Component
class InMemoryResetTokenStore : PasswordResetTokenStore {
    private data class Entry(val userId: Long, val expiresAt: Instant)
    private val store = ConcurrentHashMap<String, Entry>()
    private val ttl = Duration.ofMinutes(30)

    override fun issue(userId: Long): String {
        val token = UUID.randomUUID().toString()
        store[token] = Entry(userId, Instant.now().plus(ttl))
        return token
    }

    override fun consume(token: String): Long? {
        val e = store.remove(token) ?: return null
        return if (e.expiresAt.isAfter(Instant.now())) e.userId else null
    }
}

/**
 * 인메모리 로그인 시도 가드(연속 실패 임계 초과 시 일시 잠금). MVP·단일 인스턴스용.
 * TODO: 다중 인스턴스에서는 Redis 등 공유 저장소로 교체. 키=아이디(아이디 잠금 → 피해자 DoS 우려는
 *       운영 시 IP 결합/캡차로 보완).
 */
@Component
class InMemoryLoginAttemptGuard(
    @Value("\${onebite.security.max-login-attempts:5}") private val maxAttempts: Int,
    @Value("\${onebite.security.lock-minutes:15}") private val lockMinutes: Long,
) : LoginAttemptGuard {
    private data class State(val failures: Int, val lockedUntil: Instant?)
    private val states = ConcurrentHashMap<String, State>()

    override fun assertNotLocked(key: String) {
        val until = states[key]?.lockedUntil ?: return
        val now = Instant.now()
        if (until.isAfter(now)) {
            throw TooManyLoginAttemptsException(Duration.between(now, until).seconds.coerceAtLeast(1))
        }
    }

    override fun recordFailure(key: String) {
        val now = Instant.now()
        states.compute(key) { _, prev ->
            // 만료된 잠금은 리셋 후 카운트
            val base = if (prev?.lockedUntil != null && !prev.lockedUntil.isAfter(now)) null else prev
            val failures = (base?.failures ?: 0) + 1
            if (failures >= maxAttempts) State(failures, now.plus(Duration.ofMinutes(lockMinutes)))
            else State(failures, null)
        }
    }

    override fun recordSuccess(key: String) {
        states.remove(key) // 성공 시 카운트 초기화
    }
}

/**
 * 비밀번호 재설정 메일 발송(dev 기본). 재설정 링크를 로깅만 한다.
 * onebite.email=smtp 이면 SmtpEmailSender 가 대신 활성화된다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.email"], havingValue = "logging", matchIfMissing = true)
class LoggingEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun sendPasswordReset(email: String, resetToken: String) {
        log.info("PASSWORD-RESET email={} link=/auth/password-reset/confirm?token={}", email, resetToken)
    }
}

/**
 * 실제 SMTP 발송(onebite.email=smtp). spring.mail.* 설정으로 JavaMailSender 가 구성돼야 한다.
 * 평문 메일(SimpleMailMessage)로 재설정 링크를 보낸다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.email"], havingValue = "smtp")
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${onebite.email-from:no-reply@onebite.app}") private val fromAddress: String,
    @Value("\${onebite.reset-url:http://localhost:3000/reset}") private val resetUrl: String,
) : EmailSender {
    override fun sendPasswordReset(email: String, resetToken: String) {
        val msg = SimpleMailMessage()
        msg.setFrom(fromAddress)
        msg.setTo(email)
        msg.subject = "[oneBite] 비밀번호 재설정 안내"
        msg.text = buildString {
            appendLine("아래 링크에서 비밀번호를 재설정해 주세요. (링크는 30분간 유효)")
            appendLine()
            appendLine("$resetUrl?token=$resetToken")
            appendLine()
            append("본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.")
        }
        mailSender.send(msg)
    }
}
