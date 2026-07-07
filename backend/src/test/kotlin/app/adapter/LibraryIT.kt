package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.SlotController
import app.application.port.`in`.LibraryEditionView
import app.application.port.`in`.LibrarySlotView
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.CrossInsight
import app.domain.model.EditionItem
import app.domain.model.Language
import app.application.port.out.EditionRepository
import app.domain.service.ComboKey
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.ZoneOffset

/** 라이브러리(FR-011c) + 소프트 삭제(FR-003b) + 읽음 노출(FR-020) 통합 테스트(HTTP). */
class LibraryIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController   // arrange
    @Autowired lateinit var slot: SlotController   // arrange
    @Autowired lateinit var editions: EditionRepository // arrange

    private fun saveEdition(combo: String, date: LocalDate, oneLine: String) = editions.save(
        Edition(
            id = null, comboKey = combo, language = Language.KO, issueDate = date,
            content = EditionContent(oneLine, listOf("요약"), listOf(CrossInsight("주요 소식", "본문", listOf(EditionItem("t", "s", "u", "politics")))), listOf("s")),
        ),
    )

    /** GET /library/editions?comboKey=... — comboKey 의 '+' 가 쿼리에서 공백으로 디코딩되지 않도록 .param 사용. */
    private fun libraryEditions(comboKey: String, userId: Long): List<LibraryEditionView> = objectMapper.readValue(
        objectMapper.readTree(
            mockMvc.perform(get("/library/editions").param("comboKey", comboKey).header("Authorization", bearer(userId)))
                .andExpect(status().isOk).andReturn().response.getContentAsString(Charsets.UTF_8),
        ).get("data").toString(), // 표준 응답 봉투 언래핑
    )

    @Test
    fun `삭제한 슬롯도 라이브러리에 남고, 읽음 상태가 노출된다`() {
        val user = auth.signup(AuthController.SignupRequest("carol", "password123", "캐럴"))
        val s1 = slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        slot.create(user.userId, SlotController.CreateSlotRequest(listOf("tech")))
        // 히스토리는 '과거 받은' 에디션 = 구독 다음날~어제. 슬롯을 이틀 전 등록으로 백데이트하고 에디션은 어제자로.
        jdbc.update("UPDATE slots SET created_at = now() - interval '2 day' WHERE user_id = ?", user.userId)

        val day = LocalDate.now(ZoneOffset.UTC).minusDays(1) // 어제(과거 발송분)
        val combo1 = ComboKey.of(listOf("politics", "economy"))
        val combo2 = ComboKey.of(listOf("tech"))
        val ed1 = saveEdition(combo1, day, "정치+경제 핵심")
        saveEdition(combo2, day, "테크 핵심")

        // 슬롯 1 삭제(arrange) → 활성 목록에서 빠지지만 라이브러리에는 유지되어야 함
        slot.delete(user.userId, s1.id)
        assertEquals(1, slot.list(user.userId).size) // 활성 슬롯은 1개(tech)

        val libSlots = objectMapper.readValue<List<LibrarySlotView>>(getBody("/library/slots", user.userId))
        assertEquals(2, libSlots.size) // 삭제 슬롯 포함 2개
        val deleted = libSlots.first { it.comboKey == combo1 }
        assertFalse(deleted.active) // 삭제되었지만 라이브러리에 존재
        assertEquals(1, deleted.editionCount)

        // 읽음 노출: 상세 열람 전 false → 열람(HTTP) 후 true
        assertFalse(libraryEditions(combo1, user.userId).first().read)
        getBody("/editions/${ed1.id}", user.userId) // 상세 열람으로 읽음 처리
        assertTrue(libraryEditions(combo1, user.userId).first().read)
    }
}
