package app.application.port.`in`

import java.time.LocalDate

/** 8시 배치 생성(VIII): 구독 슬롯에 한해 조합·언어·일자별 에디션 생성(있으면 재사용). */
interface GenerateEditionsUseCase {
    fun runForDate(issueDate: LocalDate): GenerationSummary
}
data class GenerationSummary(val generated: Int, val reused: Int, val fallback: Int = 0)

/** 발송(원칙 I·V·X): 동의 게이트 통과 사용자에게 묶음 1푸시 잡 발행. */
interface DispatchUseCase {
    fun dispatchForDate(issueDate: LocalDate): Int // 발행한 푸시 잡 수
}

/** 열람(VIII): 오늘 발송분 + 상세. 조회 시 읽음 기록(FR-020). */
interface ReadEditionUseCase {
    fun today(userId: Long): TodayView
    fun edition(userId: Long, editionId: Long): EditionDetailView
}

data class TodayView(
    val issueDate: LocalDate?,
    val slots: List<TodaySlotView>,
    val banner: String?, // 폴백 시 "오늘 뉴스레터 준비 중입니다"(FR-016)
)
data class TodaySlotView(
    val comboKey: String,
    val categoryLine: String,
    val editionId: Long?,
    val oneLine: String?,
    val read: Boolean, // 읽음 상태 UI 노출(FR-020)
)

data class EditionDetailView(
    val id: Long,
    val issueDate: LocalDate,
    val oneLine: String,
    val marketSummary: List<String>,
    val crossInsight: String?,
    val items: List<ItemView>,
    val references: List<String>,
    val bookmarked: Boolean, // 책갈피 상태(FR-011b)
)
data class ItemView(val title: String, val source: String, val url: String, val categoryCode: String)
