package app.adapter.feed

import app.adapter.out.feed.RssFeedAdapter
import app.domain.model.Language
import app.application.port.out.FeedFetcher
import app.application.port.out.RssSource
import app.application.port.out.RssSourceProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RssFeedAdapterTest {

    private val sources = RssSourceProvider { listOf(RssSource(it, "https://x/feed", Language.KO)) }

    private fun rss(items: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"><channel><title>테스트피드</title>$items</channel></rss>
    """.trimIndent()

    private fun item(title: String, pubDate: String) =
        """<item><title>$title</title><link>https://x/a/$title</link><pubDate>$pubDate</pubDate></item>"""

    private val until = Instant.parse("2026-06-24T00:00:00Z") // 창 상한(테스트 기본)

    @Test
    fun `RSS를 파싱해 RawArticle로 매핑한다`() {
        val xml = rss(item("기준금리 동결", "Tue, 23 Jun 2026 09:00:00 GMT"))
        val adapter = RssFeedAdapter(sources, FeedFetcher { xml })
        val since = Instant.parse("2026-06-23T00:00:00Z")

        val articles = adapter.fetch("economy", since, until)

        assertEquals(1, articles.size)
        assertEquals("기준금리 동결", articles.first().title)
        assertEquals(Language.KO, articles.first().language)
        assertEquals("테스트피드", articles.first().source)
    }

    @Test
    fun `창 밖(이전·이후) 항목은 제외한다 (FR-003c)`() {
        val xml = rss(
            item("창안", "Tue, 23 Jun 2026 09:00:00 GMT") +
                item("이전", "Mon, 01 Jun 2026 09:00:00 GMT") + // since 이전
                item("이후", "Wed, 01 Jul 2026 09:00:00 GMT"),  // until 이후
        )
        val adapter = RssFeedAdapter(sources, FeedFetcher { xml })
        val articles = adapter.fetch("economy", Instant.parse("2026-06-23T00:00:00Z"), until)
        assertEquals(listOf("창안"), articles.map { it.title })
    }

    @Test
    fun `fetch 실패(null)면 해당 소스는 건너뛴다`() {
        val adapter = RssFeedAdapter(sources, FeedFetcher { null })
        assertTrue(adapter.fetch("economy", Instant.EPOCH, until).isEmpty())
    }

    @Test
    fun `파싱 불가 XML은 빈 결과로 처리한다`() {
        val adapter = RssFeedAdapter(sources, FeedFetcher { "not-xml" })
        assertTrue(adapter.fetch("economy", Instant.EPOCH, until).isEmpty())
    }
}
