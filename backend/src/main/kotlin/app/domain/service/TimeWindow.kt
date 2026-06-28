package app.domain.service

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 타임존 일관성(원칙 XI): "오전 8시"는 사용자 타임존 기준 벽시계 시간.
 * UTC 저장 → 발송 시 사용자 타임존으로 변환. DST의 미존재/중복 시각도 1회만 발송.
 */
object TimeWindow {
    val SEND_LOCAL_TIME: LocalTime = LocalTime.of(8, 0)

    /**
     * 주어진 발송일자의 사용자 로컬 08:00에 해당하는 시점을 반환.
     * - spring-forward(존재하지 않는 시각): ZonedDateTime이 자동으로 다음 유효 시각으로 보정 → 1회.
     * - fall-back(중복 시각): 더 이른 오프셋을 선택해 1회만 발송.
     */
    fun sendInstant(issueDate: LocalDate, zone: ZoneId): ZonedDateTime {
        val candidate = ZonedDateTime.of(issueDate, SEND_LOCAL_TIME, zone)
        // fall-back 중복 시각이면 더 이른 오프셋으로 정규화(1회 발송 보장)
        return candidate.withEarlierOffsetAtOverlap()
    }

    /** 디스패치 정시성 판정(SC-002): 예정 시각 대비 허용 오차 내인가. */
    fun isWithinTolerance(scheduled: ZonedDateTime, dispatched: ZonedDateTime, toleranceMinutes: Long): Boolean {
        val diff = Duration.between(scheduled, dispatched).abs()
        return diff <= Duration.ofMinutes(toleranceMinutes)
    }
}
