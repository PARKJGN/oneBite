package app.adapter

import app.adapter.`in`.web.AuthController
import app.application.port.out.DeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 기기 토큰 등록·조회(HTTP): POST /devices 등록(멱등) 후 userId로 활성 토큰을 찾는다. */
class DeviceTokenIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController        // arrange
    @Autowired lateinit var tokens: DeviceTokenRepository // assert

    @Test
    fun `기기 토큰을 등록하면 userId로 조회되고, 같은 토큰 재등록은 멱등이다`() {
        val user = auth.signup(AuthController.SignupRequest("frank", "password123", "프랭크"))
        val token = bearer(user.userId)

        mockMvc.perform(post("/devices").header("Authorization", token)
            .contentType(APPLICATION_JSON).content("""{"token":"fcm-token-abc","platform":"android"}"""))
            .andExpect(status().isCreated)
        // 멱등(플랫폼만 갱신)
        mockMvc.perform(post("/devices").header("Authorization", token)
            .contentType(APPLICATION_JSON).content("""{"token":"fcm-token-abc","platform":"ios"}"""))
            .andExpect(status().isCreated)

        assertEquals(listOf("fcm-token-abc"), tokens.findActiveTokens(user.userId))
    }
}
