package app.adapter.out.metrics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

/**
 * 관측 이벤트 인메모리 집계(FR-017, T077/T088). 비식별 카운트 + 정시성(SC-001/002) 산출.
 * EventPublisher가 emit할 때마다 record()로 누적된다.
 * TODO: 다중 인스턴스/영속 지표는 Micrometer+Prometheus 또는 Kafka→시계열 저장소로 확장.
 */
@Component
class MetricsRegistry(
    private val clock: Clock,
    @Value("\${onebite.delivery.content-ready-deadline:07:30}") contentDeadline: String,
    @Value("\${onebite.delivery.send-local-time:08:00}") sendLocalTime: String,
    @Value("\${onebite.delivery.dispatch-tolerance-minutes:5}") toleranceMinutes: Long,
    @Value("\${onebite.metrics.zone:Asia/Seoul}") zoneId: String,
) {
    private val zone = ZoneId.of(zoneId)
    private val contentReadyDeadline = LocalTime.parse(contentDeadline)         // SC-001 마감(07:30)
    private val dispatchDeadline = LocalTime.parse(sendLocalTime).plusMinutes(toleranceMinutes) // SC-002 마감(08:05)

    private val counts = ConcurrentHashMap<String, LongAdder>()
    private val genOnTime = LongAdder()
    private val genLate = LongAdder()
    private val dispatchOnTime = LongAdder()
    private val dispatchLate = LongAdder()

    fun record(type: String, attributes: Map<String, Any?>) {
        counts.computeIfAbsent(type) { LongAdder() }.increment()
        val now = LocalTime.now(clock.withZone(zone))
        when (type) {
            "generation" -> if (!now.isAfter(contentReadyDeadline)) genOnTime.increment() else genLate.increment()
            "dispatch" -> if (!now.isAfter(dispatchDeadline)) dispatchOnTime.increment() else dispatchLate.increment()
        }
    }

    fun snapshot(): MetricsSnapshot {
        val c = counts.mapValues { it.value.sum() }
        val generation = c["generation"] ?: 0
        val cacheHit = c["cache_hit"] ?: 0
        val fallback = c["fallback"] ?: 0
        val aiFailure = c["ai_failure"] ?: 0

        val produced = cacheHit + generation + fallback // 당일 산출(재사용 포함) 총량
        val reuseRate = ratio(cacheHit, produced)                          // 재사용률(SC 목표 0.90)
        val fallbackRate = ratio(fallback, generation + fallback)          // 폴백 제공 빈도
        val aiFailureRate = ratio(aiFailure, generation + aiFailure)       // AI 실패율
        val contentOnTimeRate = ratio(genOnTime.sum(), genOnTime.sum() + genLate.sum())     // SC-001(목표 1.0)
        val dispatchOnTimeRate = ratio(dispatchOnTime.sum(), dispatchOnTime.sum() + dispatchLate.sum()) // SC-002(목표 0.99)

        val alarms = buildList {
            if (genOnTime.sum() + genLate.sum() > 0 && contentOnTimeRate < 1.0) {
                add("SC-001 위반: 콘텐츠 07:30 완료율 ${pct(contentOnTimeRate)} (<100%)")
            }
            if (dispatchOnTime.sum() + dispatchLate.sum() > 0 && dispatchOnTimeRate < 0.99) {
                add("SC-002 위반: 디스패치 +5분 완료율 ${pct(dispatchOnTimeRate)} (<99%)")
            }
        }

        return MetricsSnapshot(
            counts = c,
            reuseRate = reuseRate,
            fallbackRate = fallbackRate,
            aiFailureRate = aiFailureRate,
            contentOnTimeRate = contentOnTimeRate,
            dispatchOnTimeRate = dispatchOnTimeRate,
            alarms = alarms,
        )
    }

    private fun ratio(num: Long, den: Long): Double = if (den == 0L) 0.0 else num.toDouble() / den
    private fun pct(r: Double): String = "%.1f%%".format(r * 100)
}

data class MetricsSnapshot(
    val counts: Map<String, Long>,
    val reuseRate: Double,
    val fallbackRate: Double,
    val aiFailureRate: Double,
    val contentOnTimeRate: Double,
    val dispatchOnTimeRate: Double,
    val alarms: List<String>,
)
