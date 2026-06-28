package app.adapter.`in`.web

import app.domain.port.`in`.RegisterDeviceTokenUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 기기 푸시 토큰 등록(US4). MVP: 사용자 식별 X-User-Id 헤더(후속 토큰 인증으로 대체). */
@RestController
class DeviceController(private val devices: RegisterDeviceTokenUseCase) {

    data class RegisterRequest(val token: String, val platform: String = "android")

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @CurrentUserId userId: Long,
        @RequestBody req: RegisterRequest,
    ) {
        devices.register(userId, req.token, req.platform)
    }
}
