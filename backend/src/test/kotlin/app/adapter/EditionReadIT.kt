package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.SlotController
import app.application.port.`in`.EditionDetailView
import app.application.port.`in`.TodayView
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZoneId

/** US2 통합 테스트(T040, HTTP): /today 오늘 발송분 열람·/editions/{id} 상세·읽음 기록. */
class EditionReadIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var slot: SlotController
    @Autowired lateinit var editions: EditionRepository

    @Test
    fun `오늘 발송분을 슬롯별로 보여주고 상세에서 3요소를 반환한다`() {
        val user = auth.signup(AuthController.SignupRequest("bobby", "password123", "밥"))
        val s = slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        // 에디션은 슬롯 등록 '다음날'부터 노출 → 어제 등록한 것으로 백데이트해야 오늘 발송분이 보인다.
        jdbc.update("UPDATE slots SET created_at = now() - interval '1 day' WHERE id = ?", s.id)

        val today = LocalDate.now(ZoneId.of("Asia/Seoul")) // 기본 사용자 타임존(원칙 XI) — today()와 동일 기준
        val comboKey = ComboKey.of(listOf("politics", "economy"))
        val saved = editions.save(
            Edition(
                id = null, comboKey = comboKey, language = Language.KO, issueDate = today,
                content = EditionContent(
                    oneLine = "금리 동결과 거래 회복이 맞물린 하루",
                    marketSummary = listOf("맥락·영향 설명 단락"),
                    crossInsights = listOf(
                        CrossInsight(
                            headline = "정치+경제 연결 인사이트",
                            body = "원구성 지연과 금리 동결이 맞물린 흐름",
                            items = listOf(EditionItem("기준금리 동결", "한국은행", "https://x", "economy")),
                        ),
                    ),
                    references = listOf("한국은행"),
                ),
            ),
        )

        val today1 = objectMapper.readValue<TodayView>(getBody("/today", user.userId))
        assertEquals(today, today1.issueDate)
        assertNull(today1.banner) // 오늘 발송분 존재 → 폴백 배너 없음
        assertEquals(saved.id, today1.slots.single().editionId)
        assertFalse(today1.slots.single().read) // 읽기 전 read=false

        val detail = objectMapper.readValue<EditionDetailView>(getBody("/editions/${saved.id}", user.userId))
        assertEquals(1, detail.crossInsights.size)
        assertEquals("정치+경제 연결 인사이트", detail.crossInsights.single().headline)
        assertEquals("금리 동결과 거래 회복이 맞물린 하루", detail.oneLine)
        assertEquals(1, detail.crossInsights.single().items.size)

        // 상세 열람 후 today read=true (FR-020 노출)
        val today2 = objectMapper.readValue<TodayView>(getBody("/today", user.userId))
        assertTrue(today2.slots.single().read)
    }
}
