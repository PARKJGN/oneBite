package app.adapter

import app.adapter.`in`.web.AuthController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * JWT 인증(회귀): 보호 경로는 토큰 없으면 401, 유효 JWT면 200. 공개 경로는 토큰 없이 200.
 * (X-User-Id 헤더 무방비 갭이 막혔는지 HTTP 레벨로 검증)
 */
class JwtAuthIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController

    @Test
    fun `보호 경로는 토큰 없으면 401, 유효 JWT면 200`() {
        val user = auth.signup(AuthController.SignupRequest("hank", "password123", "행크"))

        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized)

        mockMvc.perform(get("/me").header("Authorization", bearer(user.userId))).andExpect(status().isOk)
    }

    @Test
    fun `공개 경로(categories)는 토큰 없이 200`() {
        mockMvc.perform(get("/categories")).andExpect(status().isOk)
    }
}
