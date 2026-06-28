package app.application

import app.domain.UnknownCategoryException
import app.domain.model.Slot
import app.domain.model.SlotPolicy
import app.domain.port.`in`.SlotUseCase
import app.domain.port.`in`.SlotView
import app.domain.port.out.CategoryRepository
import app.domain.port.out.SlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SlotService(
    private val slots: SlotRepository,
    private val categories: CategoryRepository,
) : SlotUseCase {

    @Transactional
    override fun create(userId: Long, categoryCodes: List<String>): SlotView {
        val active = categories.findAllActive()
        val activeCodes = active.map { it.code }.toSet()
        val unknown = categoryCodes.filterNot { it in activeCodes }
        if (unknown.isNotEmpty()) throw UnknownCategoryException(unknown)

        SlotPolicy.assertCanAdd(slots.countActiveByUserId(userId))
        val saved = slots.save(Slot(id = null, userId = userId, categoryCodes = categoryCodes))
        return saved.toView(active.associate { it.code to it.nameKo })
    }

    @Transactional
    override fun update(userId: Long, slotId: Long, categoryCodes: List<String>): SlotView {
        val active = categories.findAllActive()
        val activeCodes = active.map { it.code }.toSet()
        val unknown = categoryCodes.filterNot { it in activeCodes }
        if (unknown.isNotEmpty()) throw UnknownCategoryException(unknown)

        val existing = slots.findById(slotId)
            ?: throw IllegalArgumentException("슬롯을 찾을 수 없습니다: $slotId")
        require(existing.userId == userId) { "본인 슬롯만 수정할 수 있습니다" }
        require(existing.active) { "삭제된 슬롯은 수정할 수 없습니다" }

        // 새 Slot 생성으로 1~4개 검증(data class copy는 init 검증을 안 거치므로 생성자 사용)
        val updated = Slot(id = existing.id, userId = userId, categoryCodes = categoryCodes, active = true)
        return slots.save(updated).toView(active.associate { it.code to it.nameKo })
    }

    @Transactional(readOnly = true)
    override fun list(userId: Long): List<SlotView> {
        val nameByCode = categories.findAllActive().associate { it.code to it.nameKo }
        return slots.findActiveByUserId(userId).map { it.toView(nameByCode) }
    }

    @Transactional
    override fun delete(userId: Long, slotId: Long) {
        slots.deactivate(slotId, userId) // 발송만 중단, 라이브러리(히스토리)는 유지(FR-003b)
    }

    private fun Slot.toView(nameByCode: Map<String, String>): SlotView =
        SlotView(
            id = this.id!!,
            categoryCodes = this.categoryCodes,
            categoryLine = this.categoryCodes.joinToString(" · ") { nameByCode[it] ?: it },
        )
}
