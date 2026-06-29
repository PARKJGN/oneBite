package app.domain.service

import app.domain.model.ArticleCluster

/**
 * 중요도 선정(FR-004b): 보도량(clusterSize) + 최신성 조합으로 상위 N개.
 * 편집 주관 판단은 사용하지 않는다(Non-Goals 일관).
 */
object Ranking {
    const val DEFAULT_LIMIT = 20

    /** 보도량(군집크기) 우선, 동률은 최신성. */
    private val byImportance: Comparator<ArticleCluster> =
        compareByDescending<ArticleCluster> { it.clusterSize }.thenByDescending { it.latestPublishedAt }

    fun topClusters(clusters: List<ArticleCluster>, limit: Int = DEFAULT_LIMIT): List<ArticleCluster> =
        clusters.sortedWith(byImportance).take(limit)

    /**
     * 조합(다중 카테고리) 균형 선정(원칙 XV — 교차 종합 전제): 각 카테고리에서 최소 몫(limit/n)을 먼저
     * 보장한 뒤, 남는 자리를 전체 보도량/최신성 상위로 채운다. 한 카테고리가 비면 다른 카테고리로 채움(graceful).
     * 그래야 한쪽으로 쏠리지 않고 두 카테고리가 모두 들어와 교차 인사이트가 생길 수 있다.
     * 단일 카테고리면 topClusters 와 동일.
     */
    fun balancedTopClusters(
        clusters: List<ArticleCluster>,
        categoryCodes: List<String>,
        limit: Int = DEFAULT_LIMIT,
    ): List<ArticleCluster> {
        if (categoryCodes.size <= 1) return topClusters(clusters, limit)
        val perCat = limit / categoryCodes.size
        val byCat = clusters.groupBy { it.representative.categoryCode }
        val selected = LinkedHashSet<ArticleCluster>()
        // 1) 카테고리별 최소 몫 보장
        for (cat in categoryCodes) {
            byCat[cat].orEmpty().sortedWith(byImportance).take(perCat).forEach { selected += it }
        }
        // 2) 남는 자리는 전체 상위(보도량/최신성)로 채움
        clusters.sortedWith(byImportance).forEach { if (selected.size < limit) selected += it }
        // 3) 최종 노출 순서는 중요도순
        return selected.sortedWith(byImportance).take(limit)
    }
}
