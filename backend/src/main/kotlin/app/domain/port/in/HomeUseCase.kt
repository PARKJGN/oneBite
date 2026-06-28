package app.domain.port.`in`

/**
 * 홈 "어제 핵심 뉴스"(FR-021): 사용자의 모든 슬롯의 어제 에디션 항목을 합쳐
 * 핵심 뉴스 항목을 페이지당 N개(기본 5)로 페이지네이션. 신규 생성 없음(기존 콘텐츠 재사용).
 */
interface HomeUseCase {
    fun yesterdayHighlights(userId: Long, page: Int, size: Int): YesterdayHighlightsView
}

data class YesterdayHighlightsView(
    val page: Int,
    val size: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<HighlightItemView>,
)

data class HighlightItemView(
    val title: String,
    val source: String,
    val categoryCode: String,
    val editionId: Long, // 출처 에디션(참고)
    val url: String,     // 원문 기사 링크 — 선택 시 실제 뉴스로 이동(FR-021)
)
