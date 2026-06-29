package app.adapter

import app.application.port.out.RawArticleStore
import app.domain.model.Language
import app.domain.model.RawArticle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

/** raw_articles 적재: (category_code, url) 중복 무시 + 창 조회 검증(실 PostgreSQL). */
class RawArticleStoreIT : IntegrationTest() {

    @Autowired lateinit var store: RawArticleStore

    @Test
    fun `중복(category+url)은 무시하고 창으로 조회한다`() {
        val now = Instant.parse("2026-06-29T06:00:00Z")
        val a = RawArticle("제목", "https://x/1", "출처", Language.KO, "politics", now)

        assertEquals(1, store.saveNew(listOf(a)))   // 신규 1건
        assertEquals(0, store.saveNew(listOf(a)))   // 같은 (politics, url) → 중복 무시 0

        val inWindow = store.findWindow("politics", now.minusSeconds(3600), now.plusSeconds(3600))
        assertEquals(listOf("제목"), inWindow.map { it.title })

        // 창 밖(발행 이후)이면 조회 안 됨
        assertEquals(0, store.findWindow("politics", now.plusSeconds(7200), now.plusSeconds(9999)).size)
    }
}
