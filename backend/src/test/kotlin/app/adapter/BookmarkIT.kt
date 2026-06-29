package app.adapter

import app.adapter.`in`.web.AuthController
import app.application.port.out.EditionRepository
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.service.ComboKey
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.ZoneOffset

/** 책갈피(FR-011b): 설정→목록·상세 플래그→해제 통합 테스트(HTTP). */
class BookmarkIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController       // arrange(가입)는 직접
    @Autowired lateinit var editions: EditionRepository // arrange(에디션 시드)는 직접

    @Test
    fun `책갈피 설정·목록·상세 플래그·해제`() {
        val user = auth.signup(AuthController.SignupRequest("daisy", "password123", "데이지"))
        val token = bearer(user.userId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val ed = editions.save(
            Edition(
                id = null, comboKey = ComboKey.of(listOf("politics", "economy")), language = Language.KO, issueDate = today,
                content = EditionContent("정치+경제 핵심", listOf("요약"), null, listOf(EditionItem("t", "s", "u", "politics")), listOf("s")),
            ),
        )
        val id = ed.id!!

        // 초기: 책갈피 없음, 상세 false
        mockMvc.perform(get("/bookmarks").header("Authorization", token))
            .andExpect(status().isOk).andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/editions/$id").header("Authorization", token))
            .andExpect(status().isOk).andExpect(jsonPath("$.bookmarked").value(false))

        // 설정(PUT) → 목록 1개 + 상세 true
        mockMvc.perform(put("/editions/$id/bookmark").header("Authorization", token)).andExpect(status().isNoContent)
        mockMvc.perform(get("/bookmarks").header("Authorization", token))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].editionId").value(id.toInt()))
        mockMvc.perform(get("/editions/$id").header("Authorization", token))
            .andExpect(jsonPath("$.bookmarked").value(true))

        // 해제(DELETE) → 목록 비고 + 상세 false
        mockMvc.perform(delete("/editions/$id/bookmark").header("Authorization", token)).andExpect(status().isNoContent)
        mockMvc.perform(get("/bookmarks").header("Authorization", token))
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/editions/$id").header("Authorization", token))
            .andExpect(jsonPath("$.bookmarked").value(false))
    }
}
