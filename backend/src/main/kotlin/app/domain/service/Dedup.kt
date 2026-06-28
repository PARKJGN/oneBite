package app.domain.service

import app.domain.model.ArticleCluster
import app.domain.model.RawArticle

/**
 * 중복 통합(IX, FR-014): 정규화된 제목 유사 키로 군집화한다(MVP는 정규화 제목 해시).
 * 교차 언어 중복은 best-effort(여기서는 동일 언어 내 통합 중심).
 */
object Dedup {
    fun clusterByEvent(articles: List<RawArticle>): List<ArticleCluster> {
        val groups = articles.groupBy { normalizeTitle(it.title) }
        return groups.values.map { members ->
            val representative = members.maxByOrNull { it.publishedAt } ?: members.first()
            ArticleCluster(representative = representative, members = members)
        }
    }

    private fun normalizeTitle(title: String): String =
        title.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString(" ")
}
