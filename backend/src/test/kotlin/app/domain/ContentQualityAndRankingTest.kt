package app.domain

import app.domain.model.ArticleCluster
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.model.RawArticle
import app.domain.LowQualityContentException
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
        assertDoesNotThrow { content().validate() }
    }

    @Test
    fun `한줄평이 비면 거부`() {
        assertThrows(LowQualityContentException::class.java) { content(oneLine = "").validate() }
    }

    @Test
    fun `filler 상투구가 있으면 거부 (XII)`() {
        assertThrows(LowQualityContentException::class.java) {
            content(oneLine = "이번 사안은 큰 영향을 미칠 것으로 예상됩니다").validate()
        }
    }

    @Test
    fun `항목이 상한을 넘으면 거부`() {
        assertThrows(LowQualityContentException::class.java) {
            content(items = (1..21).map { item() }).validate() // MAX_ITEMS(20) 초과
        }
    }

    private fun cluster(size: Int, latest: Instant, cat: String = "politics"): ArticleCluster {
        val members = (1..size).map {
            RawArticle("t$it", "u$it", "s", Language.KO, cat, latest.minusSeconds(it.toLong()))
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

    @Test
    fun `조합은 카테고리별 균형으로 선정한다 (한쪽 쏠림 방지, XV)`() {
        // 부동산이 더 많고 최신이어도 정치도 최소 몫(5÷2=2) 보장돼야 함
        val realestate = (1..6).map { cluster(1, Instant.parse("2026-06-29T17:0$it:00Z"), "realestate") }
        val politics = listOf(
            cluster(1, Instant.parse("2026-06-28T01:00:00Z"), "politics"),
            cluster(1, Instant.parse("2026-06-28T02:00:00Z"), "politics"),
        )
        val top = Ranking.balancedTopClusters(realestate + politics, listOf("politics", "realestate"), limit = 5)
        val cats = top.map { it.representative.categoryCode }
        assertEquals(5, top.size)
        assertEquals(2, cats.count { it == "politics" })    // 정치 최소 몫 보장
        assertEquals(3, cats.count { it == "realestate" })  // 나머지는 우세 카테고리로 채움
    }

    @Test
    fun `한 카테고리가 비면 다른 카테고리로 채운다 (graceful)`() {
        val politics = (1..6).map { cluster(1, Instant.parse("2026-06-29T0$it:00:00Z"), "politics") }
        val top = Ranking.balancedTopClusters(politics, listOf("politics", "realestate"), limit = 5)
        assertEquals(5, top.size)
        assertEquals(5, top.count { it.representative.categoryCode == "politics" }) // 부동산 0건 → 정치로 채움
    }
}
