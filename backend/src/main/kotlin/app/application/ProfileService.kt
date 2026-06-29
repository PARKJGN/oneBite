package app.application

import app.domain.UserNotFoundException
import app.domain.model.Language
import app.domain.model.PushPermission
import app.application.port.`in`.ProfileUseCase
import app.application.port.`in`.ProfileView
import app.application.port.`in`.UpdateProfileCommand
import app.application.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(private val users: UserRepository) : ProfileUseCase {

    @Transactional(readOnly = true)
    override fun get(userId: Long): ProfileView =
        (users.findById(userId) ?: throw UserNotFoundException(userId)).toView()

    @Transactional
    override fun update(userId: Long, cmd: UpdateProfileCommand): ProfileView {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        val updated = user.copy(
            nickname = cmd.nickname?.takeIf { it.isNotBlank() } ?: user.nickname,
            timezone = cmd.timezone?.takeIf { it.isNotBlank() } ?: user.timezone,
            outputLanguage = cmd.outputLanguage?.let { Language.valueOf(it.uppercase()) } ?: user.outputLanguage,
            pushPermission = cmd.pushPermission?.let { PushPermission.valueOf(it.uppercase()) } ?: user.pushPermission,
        )
        return users.save(updated).toView()
    }

    private fun app.domain.model.User.toView() = ProfileView(
        userId = id!!,
        username = username,
        nickname = nickname,
        timezone = timezone,
        outputLanguage = outputLanguage.name.lowercase(),
        pushPermission = pushPermission.name.lowercase(),
        recoveryEmail = recoveryEmail,
    )
}
