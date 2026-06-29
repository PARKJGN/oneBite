package app.application

import app.domain.UnknownCategoryException
import app.domain.model.Category
import app.domain.model.Slot
import app.application.port.out.CategoryRepository
import app.application.port.out.SlotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** SlotService 단위 테스트 — 페이크 포트로 도메인 규칙 검증(Spring/DB 불필요). */
class SlotServiceTest {

    private val categories = object : CategoryRepository {
        override fun findAllActive() = listOf(
            Category("politics", "정치", "Politics"),
            Category("economy", "경제", "Economy"),
        )
    }

    private class FakeSlots : SlotRepository {
        val store = mutableListOf<Slot>()
        override fun save(slot: Slot): Slot {
            if (slot.id != null) { // 업데이트
                val i = store.indexOfFirst { it.id == slot.id }
                if (i >= 0) { store[i] = slot; return slot }
            }
            val saved = slot.copy(id = (store.size + 1).toLong())
            store += saved
            return saved
        }
        override fun findById(slotId: Long) = store.firstOrNull { it.id == slotId }
        override fun findActiveByUserId(userId: Long) = store.filter { it.userId == userId && it.active }
        override fun findAllByUserId(userId: Long) = store.filter { it.userId == userId }
        override fun countActiveByUserId(userId: Long) = store.count { it.userId == userId && it.active }
        override fun deactivate(slotId: Long, userId: Long): Boolean {
            val i = store.indexOfFirst { it.id == slotId && it.userId == userId && it.active }
            if (i < 0) return false
            store[i] = store[i].copy(active = false)
            return true
        }
    }

    @Test
    fun `유효한 카테고리로 슬롯을 생성하고 카테고리 라인을 만든다`() {
        val service = SlotService(FakeSlots(), categories)
        val view = service.create(userId = 1L, categoryCodes = listOf("politics", "economy"))
        assertEquals("정치 · 경제", view.categoryLine)
        assertEquals(listOf("politics", "economy"), view.categoryCodes)
    }

    @Test
    fun `알 수 없는 카테고리는 거부된다`() {
        val service = SlotService(FakeSlots(), categories)
        assertThrows(UnknownCategoryException::class.java) {
            service.create(userId = 1L, categoryCodes = listOf("politics", "unknown"))
        }
    }

    @Test
    fun `사용자당 4번째 슬롯 생성은 거부된다`() {
        val slots = FakeSlots()
        val service = SlotService(slots, categories)
        repeat(3) { service.create(1L, listOf("politics")) }
        assertThrows(IllegalArgumentException::class.java) {
            service.create(1L, listOf("economy"))
        }
    }

    @Test
    fun `슬롯을 in-place 수정하면 카테고리가 갱신된다`() {
        val slots = FakeSlots()
        val service = SlotService(slots, categories)
        val created = service.create(1L, listOf("politics"))
        val updated = service.update(1L, created.id, listOf("politics", "economy"))
        assertEquals(created.id, updated.id) // 같은 슬롯(id 유지)
        assertEquals("정치 · 경제", updated.categoryLine)
        assertEquals(1, slots.findActiveByUserId(1L).size) // 새로 추가된 게 아님
    }

    @Test
    fun `타인의 슬롯은 수정할 수 없다`() {
        val slots = FakeSlots()
        val service = SlotService(slots, categories)
        val created = service.create(1L, listOf("politics"))
        assertThrows(IllegalArgumentException::class.java) {
            service.update(userId = 999L, slotId = created.id, categoryCodes = listOf("economy"))
        }
    }
}
