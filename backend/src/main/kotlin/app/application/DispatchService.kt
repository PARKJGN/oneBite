package app.application

import app.application.port.`in`.DispatchUseCase
import app.application.port.out.DeliveryTarget
import app.application.port.out.DeliveryTargetQuery
import app.application.port.out.EditionRepository
import app.application.port.out.EventPublisher
import app.application.port.out.PushDeliveryRepository
import app.application.port.out.PushJob
import app.application.port.out.PushJobPublisher
import app.domain.service.TimeWindow
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 발송(원칙 I·V·IX·X): 동의 게이트 통과 사용자에게 묶음 1푸시(FR-009). 빈 콘텐츠는 skip.
 * - dispatchDueAt: 각 사용자의 타임존 로컬 08:00 발송 윈도에 든 사용자만 발송(원칙 XI, FR-008).
 *   n8n이 짧은 주기(예: 15분)로 호출 → 멱등(같은 일자 1회)으로 중복 방지.
 * - dispatchForDate: 운영/수동용 — 해당 일자 전체 발송(타임존 무시).
 */
@Service
class DispatchService(
    private val targets: DeliveryTargetQuery,
    private val editions: EditionRepository,
    private val publisher: PushJobPublisher,
    private val events: EventPublisher,
    private val pushDeliveries: PushDeliveryRepository,
) : DispatchUseCase {

    override fun dispatchForDate(issueDate: LocalDate): Int =
        targets.findEligibleTargets().count { dispatchOne(it, issueDate, recordedAt = null) }

    /** 타임존별 정시 발송: now 기준 각 사용자 로컬시간이 08:00 ±tolerance 면 그날치를 1회 발송. */
    fun dispatchDueAt(nowUtc: Instant, toleranceMinutes: Long = 5): Int {
        var dispatched = 0
        for (t in targets.findEligibleTargets()) {
            val zone = runCatching { ZoneId.of(t.timezone) }.getOrDefault(ZoneId.of("Asia/Seoul"))
            val local = nowUtc.atZone(zone)
            val send = TimeWindow.sendInstant(local.toLocalDate(), zone) // 그날 로컬 08:00
            if (!TimeWindow.isWithinTolerance(send, local, toleranceMinutes)) continue
            val issueDate = local.toLocalDate()
            if (pushDeliveries.existsForDate(t.userId, issueDate)) continue // 멱등(1일 1회)
            if (dispatchOne(t, issueDate, recordedAt = nowUtc)) dispatched++
        }
        return dispatched
    }

    private fun dispatchOne(t: DeliveryTarget, issueDate: LocalDate, recordedAt: Instant?): Boolean {
        val editionIds = t.comboKeys.distinct().mapNotNull { editions.findByKey(it, t.language, issueDate)?.id }
        if (editionIds.isEmpty()) {
            events.emit("dispatch_skip", mapOf("userId" to t.userId)); return false // 빈 푸시 금지
        }
        publisher.publish(PushJob(userId = t.userId, issueDate = issueDate, editionIds = editionIds))
        if (recordedAt != null) pushDeliveries.record(t.userId, issueDate, recordedAt)
        events.emit("dispatch", mapOf("userId" to t.userId, "editions" to editionIds.size))
        return true
    }
}
