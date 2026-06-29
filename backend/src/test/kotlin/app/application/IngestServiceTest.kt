package app.application

import app.application.port.out.CategoryRepository
import app.application.port.out.FeedPort
import app.application.port.out.RawArticleStore
import app.domain.model.Category
import app.domain.model.Language
import app.domain.model.RawArticle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class IngestServiceTest {

    private val clock = Clock.fixed(Instant.parse("2026-06-29T08:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `활성 카테고리별 1회 수집·적재하고 보관기간 지난 것을 정리한다`() {
        val categories = object : CategoryRepository {
            override fun findAllActive() = listOf(
                Category("politics", "정치", "Politics"),
                Category("realestate", "부동산", "Real Estate"),
            )
        }
        val fetchedCats = mutableListOf<String>()
        val feed = FeedPort { cat, _, _ ->
            fetchedCats += cat // 카테고리당 1회만 호출돼야 함(조합 중복 fetch 제거)
            listOf(RawArticle("t-$cat", "https://u/$cat", "s", Language.KO, cat, Instant.parse("2026-06-29T07:00:00Z")))
        }
        var purgeCutoff: Instant? = null
        val store = object : RawArticleStore {
            override fun saveNew(articles: List<RawArticle>) = articles.size
            override fun findWindow(categoryCode: String, sinceUtc: Instant, untilUtc: Instant) = emptyList<RawArticle>()
            override fun purgeOlderThan(cutoffUtc: Instant): Int { purgeCutoff = cutoffUtc; return 3 }
        }

        val summary = IngestService(categories, feed, store, clock).run()

        assertEquals(listOf("politics", "realestate"), fetchedCats) // 카테고리별 1회씩
        assertEquals(2, summary.fetched)
        assertEquals(2, summary.saved)
        assertEquals(3, summary.purged)
        assertEquals(Instant.parse("2026-06-15T08:00:00Z"), purgeCutoff) // now - 14일
    }
}
