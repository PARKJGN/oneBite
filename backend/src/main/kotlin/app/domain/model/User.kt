package app.domain.model

/**
 * 사용자. 인증 수단은 둘 중 하나 이상(FR-001/001c):
 *  ① 아이디·비밀번호(해시)(+선택적 복구 이메일)  — username/passwordHash 보유
 *  ② 소셜 연동(provider, providerId)             — username/passwordHash 없음
 */
data class User(
    val id: Long?,
    val username: String?,          // 소셜 사용자는 null
    val passwordHash: String?,      // 소셜 사용자는 null
    val nickname: String,
    val recoveryEmail: String? = null,
    val timezone: String = "Asia/Seoul",
    val outputLanguage: Language = Language.KO,
    val pushPermission: PushPermission = PushPermission.UNKNOWN,
    val provider: String? = null,   // google|naver|kakao
    val providerId: String? = null,
)
