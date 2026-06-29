package app.application

import app.domain.UserNotFoundException
import app.domain.service.ComboKey
import app.application.port.`in`.EditionDetailView
import app.application.port.`in`.ItemView
import app.application.port.`in`.ReadEditionUseCase
import app.application.port.`in`.TodaySlotView
import app.application.port.`in`.TodayView
import app.application.port.out.CategoryRepository
import app.application.port.out.EditionRepository
import app.application.port.out.SlotRepository
import app.application.port.out.UserEditionStateRepository
import app.application.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 열람(VIII): 오늘 발송분(슬롯별) + 상세. 오늘 에디션이 없으면 직전 발송분을 보여주고
 * "준비 중" 배너(FR-016). 상세 조회 시 읽음 기록(FR-020, UI 비노출).
 */
@Service
class EditionReadService(
    private val users: UserRepository,
    private val slots: SlotRepository,
    private val categories: CategoryRepository,
    private val editions: EditionRepository,
    private val states: UserEditionStateRepository,
    private val clock: Clock,
) : ReadEditionUseCase {

    @Transactional(readOnly = true)
    override fun today(userId: Long): TodayView {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        // 사용자 타임존 기준 '오늘'(원칙 XI) — dispatch(dispatchDueAt)와 동일 기준, UTC 고정 시 자정~8시 경계 어긋남 방지
        val zone = runCatching { ZoneId.of(user.timezone) }.getOrDefault(ZoneId.of("Asia/Seoul"))
        val today = LocalDate.ofInstant(clock.instant(), zone)
        val nameByCode = categories.findAllActive().associate { it.code to it.nameKo }

        val userSlots = slots.findActiveByUserId(userId)
        val resolved = userSlots.map { slot ->
            val comboKey = ComboKey.of(slot.categoryCodes)
            // 에디션은 슬롯 '등록 다음날'부터 노출 — 등록 당일/이후 발송분은 보여주지 않는다.
            val createdDate = slot.createdAt?.let { LocalDate.ofInstant(it, zone) }
            val edition = if (createdDate != null && !createdDate.isBefore(today)) {
                null
            } else {
                editions.findByKey(comboKey, user.outputLanguage, today)
                    ?: editions.findLatestBefore(comboKey, user.outputLanguage, today)
            }
            Triple(slot, comboKey, edition)
        }
        // 오늘치가 없는 슬롯이 하나라도 있으면(직전분 표시 또는 없음) "준비 중" 안내(FR-016)
        val anyFallback = userSlots.isNotEmpty() && resolved.any { it.third?.issueDate != today }
        val readIds = states.findReadEditionIds(userId, resolved.mapNotNull { it.third?.id })
        val slotViews = resolved.map { (slot, comboKey, edition) ->
            TodaySlotView(
                comboKey = comboKey,
                categoryLine = slot.categoryCodes.joinToString(" · ") { nameByCode[it] ?: it },
                editionId = edition?.id,
                oneLine = edition?.content?.oneLine,
                read = edition?.id != null && edition.id in readIds,
            )
        }
        val banner = if (anyFallback) "오늘 뉴스레터 준비 중입니다" else null
        return TodayView(issueDate = today, slots = slotViews, banner = banner)
    }

    @Transactional
    override fun edition(userId: Long, editionId: Long): EditionDetailView {
        val e = editions.findById(editionId) ?: throw IllegalArgumentException("에디션을 찾을 수 없습니다: $editionId")
        states.markRead(userId, editionId, Instant.now()) // 읽음 기록(데이터만)
        return EditionDetailView(
            id = e.id!!,
            issueDate = e.issueDate,
            oneLine = e.content.oneLine,
            marketSummary = e.content.marketSummary,
            crossInsight = e.content.crossInsight,
            items = e.content.items.map { ItemView(it.title, it.source, it.url, it.categoryCode) },
            references = e.content.references,
            bookmarked = states.isBookmarked(userId, editionId),
        )
    }
}
