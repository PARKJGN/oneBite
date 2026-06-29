package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.SlotController
import app.application.port.`in`.TodayView
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.application.port.out.EditionRepository
import app.domain.service.ComboKey
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 회귀(타임존, 원칙 XI): /today의 '오늘'은 사용자 타임존 기준이어야 한다(HTTP).
 * 고정 Clock: UTC 2026-06-24T20:00 → Asia/Seoul 2026-06-25T05:00 (KST 날짜 06-25, UTC 날짜 06-24).
 * today()가 UTC를 쓰면 06-24로 조회 → 에디션(06-25) 못 찾아 실패. 사용자 TZ면 06-25로 찾아 성공.
 */
class TodayTimezoneIT : IntegrationTest() {

    @TestConfiguration
    class FixedClockConfig {
        @Bean @Primary
        fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-06-24T20:00:00Z"), ZoneOffset.UTC)
    }

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var slot: SlotController
    @Autowired lateinit var editions: EditionRepository

    @Test
    fun `오늘은 사용자 타임존(Asia Seoul) 기준으로 판정한다`() {
        val user = auth.signup(AuthController.SignupRequest("zoey", "password123", "조이")) // tz 기본 Asia/Seoul
        val s = slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        // 에디션은 등록 다음날부터 노출 → 고정 Clock(KST 06-25) 이전으로 백데이트해야 06-25 발송분이 보인다.
        jdbc.update("UPDATE slots SET created_at = TIMESTAMP WITH TIME ZONE '2026-06-23 00:00:00+09' WHERE id = ?", s.id)

        val kstToday = LocalDate.of(2026, 6, 25) // 고정 Clock의 Asia/Seoul 날짜
        val saved = editions.save(
            Edition(
                id = null, comboKey = ComboKey.of(listOf("politics", "economy")),
                language = Language.KO, issueDate = kstToday,
                content = EditionContent(
                    "핵심", listOf("요약"), null,
                    listOf(EditionItem("t", "한국은행", "https://x", "economy")), listOf("한국은행"),
                ),
            ),
        )

        val today = objectMapper.readValue<TodayView>(getBody("/today", user.userId))
        assertEquals(kstToday, today.issueDate)                       // UTC면 06-24가 되어 실패
        assertEquals(saved.id, today.slots.single().editionId)
        assertNull(today.banner)
    }
}
