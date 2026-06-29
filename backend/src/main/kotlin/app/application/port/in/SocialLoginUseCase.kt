package app.application.port.`in`

/** 소셜 로그인(FR-001c) — provider 토큰 또는 웹 인가코드로 로그인/가입(upsert). */
interface SocialLoginUseCase {
    fun login(cmd: SocialLoginCommand): SocialLoginResult            // 모바일: 네이티브 SDK access 토큰
    fun loginWithCode(cmd: SocialCodeLoginCommand): SocialLoginResult // 웹: authorization code 교환
}

data class SocialLoginCommand(val provider: String, val accessToken: String)
data class SocialCodeLoginCommand(val provider: String, val code: String, val redirectUri: String, val state: String? = null)

data class SocialLoginResult(
    val token: String,
    val refreshToken: String,
    val userId: Long,
    val isNew: Boolean, // 신규 가입이면 온보딩으로 라우팅
)
