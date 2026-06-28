package app.domain.model

import java.time.Instant

/**
 * 슬롯 = 카테고리 1~4개의 묶음 = 하나의 뉴스레터(FR-003).
 * 사용자당 최대 3개(SlotPolicy에서 강제).
 */
data class Slot(
    val id: Long?,
    val userId: Long,
    val categoryCodes: List<String>,
    val active: Boolean = true, // 소프트 삭제 시 false(라이브러리엔 유지, 발송 대상에서만 제외)
    val createdAt: Instant? = null, // 구독 시작일 — 에디션은 등록 '다음날'부터 노출
) {
    init {
        require(categoryCodes.isNotEmpty()) { "슬롯은 최소 1개의 카테고리를 가져야 한다" }
        require(categoryCodes.size <= MAX_CATEGORIES) {
            "슬롯당 카테고리는 최대 $MAX_CATEGORIES 개까지 가능하다"
        }
        require(categoryCodes.toSet().size == categoryCodes.size) {
            "슬롯 내 카테고리는 중복될 수 없다"
        }
    }

    companion object {
        const val MAX_CATEGORIES = 4
        const val MAX_SLOTS_PER_USER = 3
    }
}

object SlotPolicy {
    /** 사용자당 슬롯 한도 검증(FR-003). */
    fun assertCanAdd(existingSlotCount: Int) {
        require(existingSlotCount < Slot.MAX_SLOTS_PER_USER) {
            "슬롯은 사용자당 최대 ${Slot.MAX_SLOTS_PER_USER}개까지 만들 수 있다"
        }
    }
}
