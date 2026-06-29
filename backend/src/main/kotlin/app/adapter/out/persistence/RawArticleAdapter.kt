package app.adapter.out.persistence

import app.application.port.out.RawArticleStore
import app.domain.model.Language
import app.domain.model.RawArticle
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

/** raw_articles 적재/조회. 중복은 (category_code, url) UNIQUE + ON CONFLICT DO NOTHING 로 무시. */
@Component
class RawArticlePersistenceAdapter(private val jdbc: JdbcTemplate) : RawArticleStore {

    override fun saveNew(articles: List<RawArticle>): Int {
        val sql = "INSERT INTO raw_articles (category_code, url, title, source, language, published_at) " +
            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (category_code, url) DO NOTHING"
        // ON CONFLICT DO NOTHING → 신규 1, 중복 0. 합 = 신규 저장 건수.
        return articles.sumOf { a ->
            jdbc.update(
                sql, a.categoryCode, a.url, a.title, a.source,
                a.language.name.lowercase(), Timestamp.from(a.publishedAt),
            )
        }
    }

    override fun findWindow(categoryCode: String, sinceUtc: Instant, untilUtc: Instant): List<RawArticle> =
        jdbc.query(
            "SELECT category_code, url, title, source, language, published_at FROM raw_articles " +
                "WHERE category_code = ? AND published_at >= ? AND published_at <= ? ORDER BY published_at DESC",
            { rs, _ ->
                RawArticle(
                    title = rs.getString("title"),
                    url = rs.getString("url"),
                    source = rs.getString("source"),
                    language = Language.valueOf(rs.getString("language").uppercase()),
                    categoryCode = rs.getString("category_code"),
                    publishedAt = rs.getTimestamp("published_at").toInstant(),
                )
            },
            categoryCode, Timestamp.from(sinceUtc), Timestamp.from(untilUtc),
        )

    override fun purgeOlderThan(cutoffUtc: Instant): Int =
        jdbc.update("DELETE FROM raw_articles WHERE published_at < ?", Timestamp.from(cutoffUtc))
}
