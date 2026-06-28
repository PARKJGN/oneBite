package app.domain.service

import app.domain.model.ArticleCluster

/**
 * 중요도 선정(FR-004b): 보도량(clusterSize) + 최신성 조합으로 상위 N개.
 * 편집 주관 판단은 사용하지 않는다(Non-Goals 일관).
 */
object Ranking {
    const val DEFAULT_LIMIT = 5

    fun topClusters(clusters: List<ArticleCluster>, limit: Int = DEFAULT_LIMIT): List<ArticleCluster> =
        clusters
            .sortedWith(
                compareByDescending<ArticleCluster> { it.clusterSize }
                    .thenByDescending { it.latestPublishedAt },
            )
            .take(limit)
}
