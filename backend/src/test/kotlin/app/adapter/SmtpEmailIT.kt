package app.adapter

import app.adapter.out.security.SmtpEmailSender
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.mail.javamail.JavaMailSenderImpl

/**
 * SmtpEmailSender가 실제 SMTP(인프로세스 GreenMail)로 재설정 메일을 발송하는지 검증.
 * Spring 컨텍스트 없이 어댑터를 직접 구성해 발송 경로만 확인한다.
 */
class SmtpEmailIT {

    companion object {
        @JvmField
        @RegisterExtension
        val greenMail = GreenMailExtension(ServerSetupTest.SMTP) // 127.0.0.1:3025, 인증 없음
    }

    @Test
    fun `비밀번호 재설정 메일이 실제 SMTP로 발송된다`() {
        val mailSender = JavaMailSenderImpl().apply {
            host = "127.0.0.1"
            port = ServerSetupTest.SMTP.port
        }
        val sender = SmtpEmailSender(
            mailSender,
            fromAddress = "no-reply@onebite.app",
            resetUrl = "http://localhost:3000/reset",
        )

        sender.sendPasswordReset("user@example.com", "tok-abc-123")

        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "메일이 SMTP 서버에 도착해야 한다")
        val messages = greenMail.receivedMessages
        assertEquals(1, messages.size)

        val m = messages[0]
        assertEquals("user@example.com", m.allRecipients[0].toString())
        assertTrue(m.subject.contains("비밀번호 재설정"), "제목에 안내 문구가 있어야 한다")
        // text/plain → getContent()는 디코딩된 본문(String) 반환(quoted-printable 인코딩 영향 없음)
        val body = m.content as String
        assertTrue(body.contains("/reset?token=tok-abc-123"), "본문에 토큰이 붙은 재설정 URL이 포함되어야 한다")
    }
}
