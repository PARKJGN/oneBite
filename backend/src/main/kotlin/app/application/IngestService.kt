package app.application

import app.application.port.`in`.IngestSummary
import app.application.port.`in`.IngestUseCase
import app.application.port.out.CategoryRepository
import app.application.port.out.FeedPort
import app.application.port.out.RawArticleStore
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration

/**
 * 주기 수집(매시간, n8n→/internal/ingest:run): 활성 카테고리별 RSS 를 1회씩 가져와 raw_articles 에 적재.
 * - 라이브 피드는 최근분만 노출되므로, 사라지기 전에 적재해 8시 배치가 고정 창을 안정적으로 재구성하게 한다.
 * - 카테고리당 1회만 fetch → 조합마다 중복 fetch 하던 비효율 제거.
 * - 중복은 RawArticleStore(ON CONFLICT)에서 무시. 보관 기간 지난 건은 purge.
 */
@Service
class IngestService(
    private val categories: CategoryRepository,
    private val feed: FeedPort,
    private val store: RawArticleStore,
    private val clock: Clock,
) : IngestUseCase {

    companion object {
        private val LOOKBACK = Duration.ofHours(48)   // 피드에 남은 최근분을 넉넉히 적재(중복은 어차피 무시)
        private val RETENTION = Duration.ofDays(14)    // 적재 보관 기간
    }

    override fun run(): IngestSummary {
        val now = clock.instant()
        val since = now.minus(LOOKBACK)
        var fetched = 0
        var saved = 0
        for (category in categories.findAllActive()) {
            val articles = feed.fetch(category.code, since, now)
            fetched += articles.size
            saved += store.saveNew(articles)
        }
        val purged = store.purgeOlderThan(now.minus(RETENTION))
        return IngestSummary(fetched = fetched, saved = saved, purged = purged)
    }
}
