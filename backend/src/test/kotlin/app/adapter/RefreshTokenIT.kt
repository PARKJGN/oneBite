package app.adapter

import app.adapter.`in`.web.AuthController
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Refresh 토큰(HTTP): 로그인→회전(새 쌍, 옛 토큰 폐기)→재사용 거부(401)→로그아웃(204) 후 거부(401).
 */
class RefreshTokenIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController // arrange(가입)는 직접

    private fun refreshTokenFrom(body: String): String = objectMapper.readTree(body).get("refreshToken").asText()

    private fun refresh(token: String) = mockMvc.perform(
        post("/auth/refresh").contentType(APPLICATION_JSON).content("""{"refreshToken":"$token"}"""),
    )

    @Test
    fun `로그인 후 refresh 회전·재사용 거부·로그아웃`() {
        auth.signup(AuthController.SignupRequest("rita", "password123", "리타"))

        val login = mockMvc.perform(
            post("/auth/login").contentType(APPLICATION_JSON).content("""{"username":"rita","password":"password123"}"""),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val r1 = refreshTokenFrom(login)

        // 회전: r1 → 새 쌍(r2), r1은 폐기
        val r2 = refreshTokenFrom(refresh(r1).andExpect(status().isOk).andReturn().response.contentAsString)
        assertNotEquals(r1, r2)

        // 옛 refresh(r1) 재사용 거부 → 401
        refresh(r1).andExpect(status().isUnauthorized)

        // 새 refresh(r2)는 동작 → r3
        val r3 = refreshTokenFrom(refresh(r2).andExpect(status().isOk).andReturn().response.contentAsString)

        // 로그아웃(r3 폐기, 204) 후 거부 → 401
        mockMvc.perform(
            post("/auth/logout").contentType(APPLICATION_JSON).content("""{"refreshToken":"$r3"}"""),
        ).andExpect(status().isNoContent)
        refresh(r3).andExpect(status().isUnauthorized)
    }
}
