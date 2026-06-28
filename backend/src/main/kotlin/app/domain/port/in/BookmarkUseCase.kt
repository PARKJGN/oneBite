package app.domain.port.`in`

import java.time.LocalDate

/** 책갈피(FR-011b): 에디션 책갈피 설정/해제 + 목록(영구 보존). */
interface BookmarkUseCase {
    fun setBookmark(userId: Long, editionId: Long, bookmarked: Boolean)
    fun list(userId: Long): List<BookmarkView>
}

data class BookmarkView(
    val editionId: Long,
    val issueDate: LocalDate,
    val oneLine: String,
    val comboKey: String, // 표시용 카테고리 조합 키(클라이언트에서 '·'로 포맷)
)
