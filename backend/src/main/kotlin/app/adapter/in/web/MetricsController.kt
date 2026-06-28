package app.adapter.`in`.web

import app.adapter.out.metrics.MetricsRegistry
import app.adapter.out.metrics.MetricsSnapshot
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 관측 지표 조회(T077/T088). /internal 경로 → InternalTokenFilter(X-Internal-Token)로 보호.
 * 운영 모니터링/알람 시스템이 폴링해 임계 미달(alarms)을 감지한다.
 */
@RestController
@RequestMapping("/internal/metrics")
class MetricsController(private val metrics: MetricsRegistry) {

    @GetMapping
    fun snapshot(): MetricsSnapshot = metrics.snapshot()
}
