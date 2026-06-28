package app.domain

import app.domain.model.Slot
import app.domain.model.SlotPolicy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

/** FR-003: 슬롯 ≤3, 슬롯당 카테고리 1~4. */
class SlotRulesTest {

    @Test
    fun `카테고리 1개에서 4개까지는 허용된다`() {
        assertDoesNotThrow { Slot(null, 1L, listOf("politics")) }
        assertDoesNotThrow { Slot(null, 1L, listOf("politics", "economy", "realestate", "tech")) }
    }

    @Test
    fun `카테고리가 0개면 거부된다`() {
        assertThrows(IllegalArgumentException::class.java) { Slot(null, 1L, emptyList()) }
    }

    @Test
    fun `카테고리가 5개 이상이면 거부된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            Slot(null, 1L, listOf("politics", "economy", "realestate", "tech", "science"))
        }
    }

    @Test
    fun `슬롯 내 카테고리 중복은 거부된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            Slot(null, 1L, listOf("politics", "politics"))
        }
    }

    @Test
    fun `사용자당 슬롯은 최대 3개까지만 추가할 수 있다`() {
        assertDoesNotThrow { SlotPolicy.assertCanAdd(existingSlotCount = 2) }
        assertThrows(IllegalArgumentException::class.java) { SlotPolicy.assertCanAdd(existingSlotCount = 3) }
    }
}
