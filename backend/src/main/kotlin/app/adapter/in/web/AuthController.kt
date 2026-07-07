package app.adapter.`in`.web

import app.application.port.`in`.AuthUseCase
import app.application.port.`in`.LoginCommand
import app.application.port.`in`.PasswordResetUseCase
import app.application.port.`in`.SignupCommand
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val auth: AuthUseCase,
    private val passwordReset: PasswordResetUseCase,
) {

    data class SignupRequest(
        val username: String,
        val password: String,
        val nickname: String,
        val recoveryEmail: String? = null,
    )
    data class SignupResponse(val userId: Long, val nickname: String, val token: String, val refreshToken: String)

    data class LoginRequest(val username: String, val password: String)
    data class LoginResponse(val token: String, val refreshToken: String, val userId: Long)
    data class RefreshRequest(val refreshToken: String)
    data class TokenResponse(val token: String, val refreshToken: String)

    data class CheckUsernameResponse(val available: Boolean)
    data class CheckNicknameResponse(val available: Boolean)

    @GetMapping("/check-username")
    fun checkUsername(@RequestParam username: String) = CheckUsernameResponse(auth.isUsernameAvailable(username))

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam nickname: String) = CheckNicknameResponse(auth.isNicknameAvailable(nickname))

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@RequestBody req: SignupRequest): SignupResponse {
        val r = auth.signup(SignupCommand(req.username, req.password, req.nickname, req.recoveryEmail))
        return SignupResponse(r.userId, r.nickname, r.token, r.refreshToken)
    }

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): LoginResponse {
        val r = auth.login(LoginCommand(req.username, req.password))
        return LoginResponse(r.token, r.refreshToken, r.userId)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshRequest): TokenResponse {
        val r = auth.refresh(req.refreshToken) // 회전: 옛 refresh 폐기·새 쌍 발급
        return TokenResponse(r.token, r.refreshToken)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestBody req: RefreshRequest) {
        auth.logout(req.refreshToken)
    }

    data class ResetRequest(val username: String)
    data class ResetConfirm(val resetToken: String, val newPassword: String)

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun resetRequest(@RequestBody req: ResetRequest) {
        passwordReset.requestPasswordReset(req.username) // 존재 여부 비노출, 항상 202
    }

    @PostMapping("/password-reset/confirm")
    fun resetConfirm(@RequestBody req: ResetConfirm) {
        passwordReset.confirmPasswordReset(req.resetToken, req.newPassword)
    }
}
