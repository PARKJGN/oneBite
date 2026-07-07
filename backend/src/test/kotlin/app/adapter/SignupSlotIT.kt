package app.adapter

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * US1 통합 테스트(T024): 가입 → 슬롯 생성/조회가 실제 엔드포인트(HTTP) + PostgreSQL + Flyway 에서 동작.
 * 직렬화·상태코드·시큐리티 필터(JWT)까지 포함해 검증한다. Docker 없으면 클래스 전체 스킵.
 */
class SignupSlotIT : IntegrationTest() {

    @Test
    fun `가입 후 슬롯을 생성하고 조회한다`() {
        val res = mockMvc.perform(
            post("/auth/signup").contentType(APPLICATION_JSON)
                .content("""{"username":"alice","password":"password123","nickname":"앨리스"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val userId = objectMapper.readTree(res).get("data").get("userId").asLong() // 표준 응답 봉투 언래핑
        assertTrue(userId > 0)

        mockMvc.perform(
            post("/slots").header("Authorization", bearer(userId))
                .contentType(APPLICATION_JSON).content("""{"categoryCodes":["politics","economy"]}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/slots").header("Authorization", bearer(userId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].categoryLine").value("정치 · 경제"))
    }

    @Test
    fun `카테고리 목록은 시드된 10개를 반환한다`() {
        mockMvc.perform(get("/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(10))
    }
}
