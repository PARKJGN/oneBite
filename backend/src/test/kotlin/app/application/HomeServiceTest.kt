package app.application

import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.model.Slot
import app.domain.model.User
import app.domain.port.out.EditionRepository
import app.domain.port.out.SlotRepository
import app.domain.port.out.UserRepository
import app.domain.service.ComboKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HomeServiceTest {

    private val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)

    private val users = object : UserRepository {
        override fun save(user: User) = user
        override fun findByUsername(username: String): User? = null
        override fun existsByUsername(username: String) = false
        override fun findById(id: Long) = User(id, "u", "h", "닉", outputLanguage = Language.KO)
        override fun findByProvider(provider: String, providerId: String): User? = null
        override fun delete(userId: Long) {}
    }

    private val slots = object : SlotRepository {
        override fun save(slot: Slot) = slot
        override fun findById(slotId: Long): Slot? = null
        override fun findActiveByUserId(userId: Long) = findAllByUserId(userId)
        override fun findAllByUserId(userId: Long) = listOf(
            Slot(1, userId, listOf("politics", "economy")),
            Slot(2, userId, listOf("tech")),
        )
        override fun countActiveByUserId(userId: Long) = 2
        override fun deactivate(slotId: Long, userId: Long) = true
    }

    private fun editionWith(combo: String, n: Int) = Edition(
        id = combo.hashCode().toLong(), comboKey = combo, language = Language.KO, issueDate = yesterday,
        content = EditionContent("핵심", listOf("요약"), null,
            (1..n).map { EditionItem("t$it", "s", "u", "politics") }, listOf("s")),
    )

    private val editions = object : EditionRepository {
        val map = mapOf(
            ComboKey.of(listOf("politics", "economy")) to editionWith(ComboKey.of(listOf("politics", "economy")), 3),
            ComboKey.of(listOf("tech")) to editionWith(ComboKey.of(listOf("tech")), 3),
        )
        override fun findByKey(comboKey: String, language: Language, issueDate: LocalDate) =
            if (issueDate == yesterday) map[comboKey] else null
        override fun findLatestBefore(comboKey: String, language: Language, issueDate: LocalDate): Edition? = null
        override fun findById(id: Long): Edition? = null
        override fun save(edition: Edition) = edition
        override fun findByComboAndLanguage(comboKey: String, language: Language) = emptyList<Edition>()
    }

    @Test
    fun `어제 핵심 뉴스는 슬롯을 합쳐 5개씩 페이지네이션한다 (FR-021)`() {
        val svc = HomeService(users, slots, editions)
        val p0 = svc.yesterdayHighlights(userId = 1, page = 0, size = 5)
        assertEquals(6, p0.totalItems)   // 3 + 3
        assertEquals(2, p0.totalPages)   // ceil(6/5)
        assertEquals(5, p0.items.size)

        val p1 = svc.yesterdayHighlights(userId = 1, page = 1, size = 5)
        assertEquals(1, p1.items.size)   // 남은 1개
    }
}
