package app.domain.port.`in`

/** 인바운드(유스케이스) 포트 + 커맨드/결과. */

interface AuthUseCase {
    fun isUsernameAvailable(username: String): Boolean               // 아이디 중복/형식 확인(가입 전)
    fun signup(cmd: SignupCommand): SignupResult
    fun login(cmd: LoginCommand): LoginResult
    fun refresh(refreshToken: String): TokenPair                      // refresh 토큰 회전 → 새 access/refresh
    fun logout(refreshToken: String)                                 // 제시한 refresh 토큰 폐기
}

interface PasswordResetUseCase {
    fun requestPasswordReset(username: String)                       // 복구 이메일로 재설정 링크 발송
    fun confirmPasswordReset(resetToken: String, newPassword: String) // 토큰 검증 후 비밀번호 변경
}

data class TokenPair(val token: String, val refreshToken: String)

data class SignupCommand(
    val username: String,
    val password: String,
    val nickname: String,
    val recoveryEmail: String? = null,
)
// 가입 즉시 로그인 상태가 되도록 토큰을 함께 반환(로그인과 동일 형태).
data class SignupResult(val userId: Long, val nickname: String, val token: String, val refreshToken: String)

data class LoginCommand(val username: String, val password: String)
data class LoginResult(val token: String, val refreshToken: String, val userId: Long)

interface SlotUseCase {
    fun create(userId: Long, categoryCodes: List<String>): SlotView
    fun update(userId: Long, slotId: Long, categoryCodes: List<String>): SlotView // in-place 편집
    fun list(userId: Long): List<SlotView>          // 활성 슬롯(내 슬롯 화면)
    fun delete(userId: Long, slotId: Long)          // 소프트 삭제(라이브러리 유지)
}

data class SlotView(val id: Long, val categoryCodes: List<String>, val categoryLine: String)
