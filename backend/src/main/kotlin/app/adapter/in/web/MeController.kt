package app.adapter.`in`.web

import app.domain.port.`in`.ProfileUseCase
import app.domain.port.`in`.ProfileView
import app.domain.port.`in`.UpdateProfileCommand
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 프로필(/me). 타임존 캡처(T036a)·언어 변경(FR-002a)·권한 상태 동기화(US4)를 지원.
 * MVP: 사용자 식별은 X-User-Id 헤더(후속 토큰 인증으로 대체).
 */
@RestController
@RequestMapping("/me")
class MeController(private val profile: ProfileUseCase) {

    data class UpdateRequest(
        val nickname: String? = null,
        val timezone: String? = null,
        val outputLanguage: String? = null,
        val pushPermission: String? = null,
    )

    @GetMapping
    fun get(@CurrentUserId userId: Long): ProfileView = profile.get(userId)

    @PatchMapping
    fun update(
        @CurrentUserId userId: Long,
        @RequestBody req: UpdateRequest,
    ): ProfileView = profile.update(
        userId,
        UpdateProfileCommand(req.nickname, req.timezone, req.outputLanguage, req.pushPermission),
    )
}
