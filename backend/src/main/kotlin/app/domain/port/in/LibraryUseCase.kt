package app.domain.port.`in`

import java.time.LocalDate

/**
 * 라이브러리(히스토리) 2단계(FR-011c):
 *  ① 슬롯 목록(삭제 슬롯도 과거 에디션 있으면 유지)
 *  ② 선택 슬롯(조합)이 받은 에디션 목록(읽음 상태 포함)
 *  ③ 상세는 ReadEditionUseCase.edition 재사용
 */
interface LibraryUseCase {
    fun slots(userId: Long): List<LibrarySlotView>
    fun editions(userId: Long, comboKey: String): List<LibraryEditionView>
}

data class LibrarySlotView(
    val comboKey: String,
    val categoryLine: String,
    val editionCount: Int,
    val latestDate: LocalDate?,
    val active: Boolean, // 삭제된 슬롯이면 false(라이브러리엔 남아 있음)
)

data class LibraryEditionView(
    val editionId: Long,
    val issueDate: LocalDate,
    val oneLine: String,
    val read: Boolean, // 읽음 상태 UI 노출(FR-020)
)
