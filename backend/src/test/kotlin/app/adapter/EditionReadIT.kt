package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.EditionController
import app.adapter.`in`.web.SlotController
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.port.out.EditionRepository
import app.domain.service.ComboKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.ZoneId

/** US2 통합 테스트(T040): 실제 Postgres+Flyway에서 오늘 발송분 열람·읽음 기록. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class EditionReadIT {

    companion object {
        @Container @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("onebite").withUsername("onebite").withPassword("onebite")

        @JvmStatic @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.autoconfigure.exclude") {
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
            }
        }
    }

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var slot: SlotController
    @Autowired lateinit var editionApi: EditionController
    @Autowired lateinit var editions: EditionRepository
    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate

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
                    crossInsight = "정치+경제 연결 인사이트",
                    items = listOf(EditionItem("기준금리 동결", "한국은행", "https://x", "economy")),
                    references = listOf("한국은행"),
                ),
            ),
        )

        val today1 = editionApi.today(user.userId)
        assertEquals(today, today1.issueDate)
        assertNull(today1.banner) // 오늘 발송분 존재 → 폴백 배너 없음
        assertEquals(saved.id, today1.slots.single().editionId)

        // 읽기 전 today read=false
        assertFalse(today1.slots.single().read)

        val detail = editionApi.edition(user.userId, saved.id!!)
        assertNotNull(detail.crossInsight)
        assertEquals("금리 동결과 거래 회복이 맞물린 하루", detail.oneLine)
        assertEquals(1, detail.items.size)

        // 상세 열람 후 today read=true (FR-020 노출)
        assertTrue(editionApi.today(user.userId).slots.single().read)
    }
}
