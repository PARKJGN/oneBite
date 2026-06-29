package app.application.port.out

/** 비밀번호 재설정 토큰 발급·소비(단일 사용·만료). FR-001b. */
interface PasswordResetTokenStore {
    fun issue(userId: Long): String
    fun consume(token: String): Long? // 유효하면 userId 반환 후 무효화, 아니면 null
}

/** 이메일 발송(복구 메일). 실 SMTP 연동은 어댑터에서. */
fun interface EmailSender {
    fun sendPasswordReset(email: String, resetToken: String)
}

/** 로그인 무차별 대입 방어: 키(아이디)별 연속 실패 누적·임계 초과 시 일시 잠금. */
interface LoginAttemptGuard {
    fun assertNotLocked(key: String) // 잠겨 있으면 TooManyLoginAttemptsException
    fun recordFailure(key: String)
    fun recordSuccess(key: String)
}
