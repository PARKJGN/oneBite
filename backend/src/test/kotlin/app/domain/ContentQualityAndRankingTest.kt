package app.domain

import app.domain.model.ArticleCluster
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.model.RawArticle
import app.domain.service.ContentQuality
import app.domain.service.LowQualityContentException
import app.domain.service.Ranking
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class ContentQualityAndRankingTest {

    private fun item(c: String = "politics") = EditionItem("제목", "출처", "https://u", c)
    private fun content(
        oneLine: String = "핵심: 무언가",
        summary: List<String> = listOf("맥락과 영향 설명"),
        items: List<EditionItem> = listOf(item()),
    ) = EditionContent(oneLine, summary, null, items, listOf("출처"))

    @Test
    fun `정상 콘텐츠는 통과한다`() {
        assertDoesNotThrow { ContentQuality.validate(content()) }
    }

    @Test
    fun `한줄평이 비면 거부`() {
        assertThrows(LowQualityContentException::class.java) { ContentQuality.validate(content(oneLine = "")) }
    }

    @Test
    fun `filler 상투구가 있으면 거부 (XII)`() {
        assertThrows(LowQualityContentException::class.java) {
            ContentQuality.validate(content(oneLine = "이번 사안은 큰 영향을 미칠 것으로 예상됩니다"))
        }
    }

    @Test
    fun `항목이 상한을 넘으면 거부`() {
        assertThrows(LowQualityContentException::class.java) {
            ContentQuality.validate(content(items = (1..6).map { item() }))
        }
    }

    private fun cluster(size: Int, latest: Instant): ArticleCluster {
        val members = (1..size).map {
            RawArticle("t$it", "u$it", "s", Language.KO, "politics", latest.minusSeconds(it.toLong()))
        }
        return ArticleCluster(representative = members.first(), members = members)
    }

    @Test
    fun `보도량(군집크기) 우선, 동률은 최신성으로 상위 선정 (FR-004b)`() {
        val a = cluster(2, Instant.parse("2026-06-23T00:00:00Z"))
        val b = cluster(3, Instant.parse("2026-06-22T00:00:00Z"))
        val c = cluster(1, Instant.parse("2026-06-23T12:00:00Z"))
        val top = Ranking.topClusters(listOf(a, b, c), limit = 2)
        assertEquals(listOf(3, 2), top.map { it.clusterSize }) // B(3) → A(2)
    }
}
