package app.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 소셜 로그인(FR-001c, HTTP): POST /auth/social 가입(upsert)·재로그인 idempotency. */
class SocialLoginIT : IntegrationTest() {

    private fun socialLogin(accessToken: String) = objectMapper.readTree(
        mockMvc.perform(post("/auth/social").contentType(APPLICATION_JSON)
            .content("""{"provider":"kakao","accessToken":"$accessToken"}"""))
            .andExpect(status().isOk).andReturn().response.getContentAsString(Charsets.UTF_8),
    )

    @Test
    fun `소셜 로그인은 최초 가입 후 재로그인 시 같은 계정을 재사용한다`() {
        val first = socialLogin("kakao-uid-1:앨리스")
        assertTrue(first.get("isNew").asBoolean())

        val second = socialLogin("kakao-uid-1")
        assertFalse(second.get("isNew").asBoolean())
        assertEquals(first.get("userId").asLong(), second.get("userId").asLong()) // (provider, providerId) UNIQUE → 동일 계정
    }
}
