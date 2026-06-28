package app.domain.model

import java.time.Instant

/** 수집된 원본 기사(RSS). */
data class RawArticle(
    val title: String,
    val url: String,
    val source: String,
    val language: Language,
    val categoryCode: String,
    val publishedAt: Instant,
)

/**
 * 같은 사건으로 묶인 기사 군집(IX, FR-014). clusterSize = 보도량(중요도 신호).
 */
data class ArticleCluster(
    val representative: RawArticle,
    val members: List<RawArticle>,
) {
    val clusterSize: Int get() = members.size
    val latestPublishedAt: Instant get() = members.maxOf { it.publishedAt }
}
