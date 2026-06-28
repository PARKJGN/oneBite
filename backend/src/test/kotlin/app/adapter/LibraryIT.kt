package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.EditionController
import app.adapter.`in`.web.LibraryController
import app.adapter.`in`.web.SlotController
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.port.out.EditionRepository
import app.domain.service.ComboKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import java.time.ZoneOffset

/** 라이브러리(FR-011c) + 소프트 삭제(FR-003b) + 읽음 노출(FR-020) 통합 테스트. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class LibraryIT {

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
    @Autowired lateinit var library: LibraryController
    @Autowired lateinit var editionApi: EditionController
    @Autowired lateinit var editions: EditionRepository
    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate

    private fun saveEdition(combo: String, date: LocalDate, oneLine: String) = editions.save(
        Edition(
            id = null, comboKey = combo, language = Language.KO, issueDate = date,
            content = EditionContent(oneLine, listOf("요약"), null, listOf(EditionItem("t", "s", "u", "politics")), listOf("s")),
        ),
    )

    @Test
    fun `삭제한 슬롯도 라이브러리에 남고, 읽음 상태가 노출된다`() {
        val user = auth.signup(AuthController.SignupRequest("carol", "password123", "캐럴"))
        val s1 = slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        slot.create(user.userId, SlotController.CreateSlotRequest(listOf("tech")))
        // 히스토리는 '과거 받은' 에디션 = 구독 다음날~어제. 슬롯을 이틀 전 등록으로 백데이트하고 에디션은 어제자로.
        // (오늘자는 '읽은 것만' 히스토리 노출이라, 읽음 무관 검증을 위해 어제자 사용)
        jdbc.update("UPDATE slots SET created_at = now() - interval '2 day' WHERE user_id = ?", user.userId)

        val day = LocalDate.now(ZoneOffset.UTC).minusDays(1) // 어제(과거 발송분)
        val combo1 = ComboKey.of(listOf("politics", "economy"))
        val combo2 = ComboKey.of(listOf("tech"))
        val ed1 = saveEdition(combo1, day, "정치+경제 핵심")
        saveEdition(combo2, day, "테크 핵심")

        // 슬롯 1 삭제 → 활성 목록에서 빠지지만 라이브러리에는 유지되어야 함
        slot.delete(user.userId, s1.id)
        assertEquals(1, slot.list(user.userId).size) // 활성 슬롯은 1개(tech)

        val libSlots = library.slots(user.userId)
        assertEquals(2, libSlots.size) // 삭제 슬롯 포함 2개
        val deleted = libSlots.first { it.comboKey == combo1 }
        assertFalse(deleted.active) // 삭제되었지만 라이브러리에 존재
        assertEquals(1, deleted.editionCount)

        // 읽음 노출: 상세 열람 전 false → 열람 후 true
        val before = library.editions(user.userId, combo1).first()
        assertFalse(before.read)
        editionApi.edition(user.userId, ed1.id!!)
        val after = library.editions(user.userId, combo1).first()
        assertTrue(after.read)
    }
}
