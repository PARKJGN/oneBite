package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.DeviceController
import app.application.port.out.DeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/** 기기 토큰 등록·조회 통합 테스트: 등록(멱등) 후 userId로 활성 토큰을 찾는다. */
class DeviceTokenIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var device: DeviceController
    @Autowired lateinit var tokens: DeviceTokenRepository

    @Test
    fun `기기 토큰을 등록하면 userId로 조회되고, 같은 토큰 재등록은 멱등이다`() {
        val user = auth.signup(AuthController.SignupRequest("frank", "password123", "프랭크"))
        device.register(user.userId, DeviceController.RegisterRequest("fcm-token-abc", "android"))
        device.register(user.userId, DeviceController.RegisterRequest("fcm-token-abc", "ios")) // 멱등(플랫폼만 갱신)

        val list = tokens.findActiveTokens(user.userId)
        assertEquals(listOf("fcm-token-abc"), list)
    }
}
