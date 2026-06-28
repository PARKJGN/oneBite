package app.adapter.out.metrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MetricsRegistryTest {

    private fun registryAtKst(utcInstant: String) = MetricsRegistry(
        clock = Clock.fixed(Instant.parse(utcInstant), ZoneId.of("UTC")),
        contentDeadline = "07:30",
        sendLocalTime = "08:00",
        toleranceMinutes = 5,
        zoneId = "Asia/Seoul",
    )

    @Test
    fun `마감 이전 생성은 정시로 집계되고 재사용률을 산출한다`() {
        val reg = registryAtKst("2026-06-24T22:00:00Z") // = 07:00 KST (07:30 이전)
        repeat(2) { reg.record("generation", emptyMap()) }
        repeat(8) { reg.record("cache_hit", emptyMap()) }

        val s = reg.snapshot()
        assertEquals(2, s.counts["generation"])
        assertEquals(8, s.counts["cache_hit"])
        assertEquals(0.8, s.reuseRate, 0.001) // 8 / (8+2)
        assertEquals(1.0, s.contentOnTimeRate, 0.001) // 07:00 ≤ 07:30
        assertTrue(s.alarms.isEmpty(), "정시면 알람 없음")
    }

    @Test
    fun `마감 이후 생성·디스패치는 SC-001·SC-002 알람을 띄운다`() {
        val reg = registryAtKst("2026-06-24T23:30:00Z") // = 08:30 KST (07:30·08:05 모두 초과)
        reg.record("generation", emptyMap())
        reg.record("dispatch", emptyMap())

        val s = reg.snapshot()
        assertEquals(0.0, s.contentOnTimeRate, 0.001)
        assertEquals(0.0, s.dispatchOnTimeRate, 0.001)
        assertEquals(2, s.alarms.size)
        assertTrue(s.alarms.any { it.contains("SC-001") })
        assertTrue(s.alarms.any { it.contains("SC-002") })
    }
}
