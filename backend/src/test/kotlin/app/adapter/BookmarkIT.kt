package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.BookmarkController
import app.adapter.`in`.web.EditionController
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.application.port.out.EditionRepository
import app.domain.service.ComboKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZoneOffset

/** 책갈피(FR-011b): 설정→목록·상세 플래그→해제 통합 테스트. */
class BookmarkIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var bookmarkApi: BookmarkController
    @Autowired lateinit var editionApi: EditionController
    @Autowired lateinit var editions: EditionRepository

    @Test
    fun `책갈피 설정·목록·상세 플래그·해제`() {
        val user = auth.signup(AuthController.SignupRequest("daisy", "password123", "데이지"))
        val today = LocalDate.now(ZoneOffset.UTC)
        val ed = editions.save(
            Edition(
                id = null, comboKey = ComboKey.of(listOf("politics", "economy")), language = Language.KO, issueDate = today,
                content = EditionContent("정치+경제 핵심", listOf("요약"), null, listOf(EditionItem("t", "s", "u", "politics")), listOf("s")),
            ),
        )

        // 초기: 책갈피 없음, 상세 false
        assertTrue(bookmarkApi.list(user.userId).isEmpty())
        assertFalse(editionApi.edition(user.userId, ed.id!!).bookmarked)

        // 설정 → 목록 1개 + 상세 true
        bookmarkApi.add(user.userId, ed.id!!)
        val list = bookmarkApi.list(user.userId)
        assertEquals(1, list.size)
        assertEquals(ed.id, list.first().editionId)
        assertTrue(editionApi.edition(user.userId, ed.id!!).bookmarked)

        // 해제 → 목록 비고 + 상세 false
        bookmarkApi.remove(user.userId, ed.id!!)
        assertTrue(bookmarkApi.list(user.userId).isEmpty())
        assertFalse(editionApi.edition(user.userId, ed.id!!).bookmarked)
    }
}
