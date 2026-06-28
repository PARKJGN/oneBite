package app.adapter.out.feed

import app.domain.model.RawArticle
import app.domain.port.out.FeedFetcher
import app.domain.port.out.FeedPort
import app.domain.port.out.RssSourceProvider
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.time.Instant

/**
 * 실 RSS 수집·파싱(FR-003a) + 품질 게이트(FR-003c): 파싱 불가·제목/링크 없음·발행일 없음·
 * 기준 시점 이전(오래된) 항목을 제외하고 로깅한다. 네트워크는 FeedFetcher로 격리(테스트 용이).
 */
@Component
class RssFeedAdapter(
    private val sources: RssSourceProvider,
    private val fetcher: FeedFetcher,
) : FeedPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetchSince(categoryCode: String, sinceUtc: Instant): List<RawArticle> =
        sources.findByCategory(categoryCode).flatMap { source ->
            val body = fetcher.fetch(source.url)
            if (body == null) { log.warn("RSS fetch 실패: {}", source.url); return@flatMap emptyList() }
            parse(body, categoryCode, source.language, sinceUtc, source.url)
        }

    private fun parse(
        body: String,
        categoryCode: String,
        language: app.domain.model.Language,
        sinceUtc: Instant,
        sourceUrl: String,
    ): List<RawArticle> = try {
        val feed = SyndFeedInput().build(XmlReader(ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))))
        val sourceName = feed.title?.takeIf { it.isNotBlank() } ?: hostOf(sourceUrl)
        feed.entries.mapNotNull { e ->
            val title = e.title?.trim().orEmpty()
            val link = e.link?.trim().orEmpty()
            val published = (e.publishedDate ?: e.updatedDate)?.toInstant()
            when {
                title.isBlank() || link.isBlank() -> { null }      // 제목/링크 없음 → 제외
                published == null -> null                          // 발행일 없음 → 제외
                published.isBefore(sinceUtc) -> null               // 오래된 항목 → 제외(롤링 윈도)
                else -> RawArticle(title, link, sourceName, language, categoryCode, published)
            }
        }
    } catch (ex: Exception) {
        log.warn("RSS 파싱 실패({}): {}", sourceUrl, ex.message)
        emptyList()
    }

    private fun hostOf(url: String) = runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)
}
