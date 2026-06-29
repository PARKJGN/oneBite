package app.application.port.`in`

interface ProfileUseCase {
    fun get(userId: Long): ProfileView
    fun update(userId: Long, cmd: UpdateProfileCommand): ProfileView
}

data class UpdateProfileCommand(
    val nickname: String? = null,
    val timezone: String? = null,
    val outputLanguage: String? = null,   // ko|en
    val pushPermission: String? = null,    // granted|denied|unknown
)

data class ProfileView(
    val userId: Long,
    val username: String?,   // 소셜 사용자는 null
    val nickname: String,
    val timezone: String,
    val outputLanguage: String,
    val pushPermission: String,
    val recoveryEmail: String?,
)
