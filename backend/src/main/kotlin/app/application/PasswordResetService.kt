package app.application

import app.domain.port.`in`.PasswordResetUseCase
import app.domain.port.out.EmailSender
import app.domain.port.out.PasswordHasher
import app.domain.port.out.PasswordResetTokenStore
import app.domain.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 비밀번호 재설정(FR-001b): 복구 이메일 발송 → 토큰 검증 → 비밀번호 변경. 인증(AuthService)과 분리된 책임. */
@Service
class PasswordResetService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val resetTokens: PasswordResetTokenStore,
    private val emailSender: EmailSender,
) : PasswordResetUseCase {

    @Transactional(readOnly = true)
    override fun requestPasswordReset(username: String) {
        // 존재 여부를 노출하지 않도록 항상 정상 반환. 복구 이메일이 있을 때만 발송.
        val user = users.findByUsername(username) ?: return
        val email = user.recoveryEmail ?: return
        val token = resetTokens.issue(user.id!!)
        emailSender.sendPasswordReset(email, token)
    }

    @Transactional
    override fun confirmPasswordReset(resetToken: String, newPassword: String) {
        require(newPassword.length >= 8) { "비밀번호는 8자 이상이어야 한다" }
        val userId = resetTokens.consume(resetToken)
            ?: throw IllegalArgumentException("유효하지 않거나 만료된 재설정 토큰입니다")
        val user = users.findById(userId) ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다")
        users.save(user.copy(passwordHash = hasher.hash(newPassword)))
    }
}
