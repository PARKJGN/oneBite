package app.adapter.`in`.web

import app.domain.port.`in`.SocialCodeLoginCommand
import app.domain.port.`in`.SocialLoginCommand
import app.domain.port.`in`.SocialLoginUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 소셜 로그인(FR-001c).
 *  - POST /auth/social      : 모바일 — 네이티브 SDK로 얻은 accessToken
 *  - POST /auth/social/code : 웹 — authorization code(서버가 secret으로 교환)
 */
@RestController
@RequestMapping("/auth/social")
class SocialAuthController(private val social: SocialLoginUseCase) {

    data class SocialLoginRequest(val provider: String, val accessToken: String)
    data class SocialCodeRequest(val provider: String, val code: String, val redirectUri: String, val state: String? = null)
    data class SocialLoginResponse(val token: String, val refreshToken: String, val userId: Long, val isNew: Boolean)

    @PostMapping
    fun login(@RequestBody req: SocialLoginRequest): SocialLoginResponse {
        val r = social.login(SocialLoginCommand(req.provider, req.accessToken))
        return SocialLoginResponse(r.token, r.refreshToken, r.userId, r.isNew)
    }

    @PostMapping("/code")
    fun loginWithCode(@RequestBody req: SocialCodeRequest): SocialLoginResponse {
        val r = social.loginWithCode(SocialCodeLoginCommand(req.provider, req.code, req.redirectUri, req.state))
        return SocialLoginResponse(r.token, r.refreshToken, r.userId, r.isNew)
    }
}
