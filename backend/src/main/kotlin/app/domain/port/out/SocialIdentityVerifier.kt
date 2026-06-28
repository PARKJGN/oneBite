package app.domain.port.out

/**
 * 소셜 제공자 토큰 검증(FR-001c) — provider access token을 검증해 제공자 사용자 식별 정보를 돌려준다.
 * 어댑터가 Google/Naver/Kakao 토큰 검증을 구현. 저장 정보는 provider·providerId·nickname으로 최소화(원칙 VI).
 */
fun interface SocialIdentityVerifier {
    fun verify(provider: String, accessToken: String): SocialIdentity
}

data class SocialIdentity(
    val provider: String,
    val providerId: String,
    val nickname: String,
)

/**
 * 웹 OAuth authorization code → access token 교환(FR-001c).
 * client_secret 등 앱 시크릿은 서버(어댑터)에만 두고, 프런트는 code만 보낸다.
 */
fun interface SocialCodeExchanger {
    fun exchange(provider: String, code: String, redirectUri: String, state: String?): String // accessToken
}
