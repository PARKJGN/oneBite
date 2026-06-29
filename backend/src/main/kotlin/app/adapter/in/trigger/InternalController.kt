package app.adapter.`in`.trigger

import app.application.DispatchService
import app.application.port.`in`.GenerateEditionsUseCase
import app.application.port.`in`.GenerationSummary
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

/**
 * n8n 오케스트레이션이 호출하는 내부 트리거(인증: 내부 전용으로 후속 보강).
 * - pipeline:run  : 구독 조합별 에디션 생성(있으면 재사용). 07:30 마감 전 실행.
 * - dispatch:run  : 동의 게이트 통과 사용자에게 묶음 1푸시 발행. 08:00 발송.
 */
@RestController
@RequestMapping("/internal")
class InternalController(
    private val generate: GenerateEditionsUseCase,
    private val dispatch: DispatchService,
) {
    data class DispatchResult(val dispatched: Int)

    @PostMapping("/pipeline:run")
    fun pipeline(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): GenerationSummary = generate.runForDate(date ?: LocalDate.now())

    /**
     * date 미지정(권장, n8n 주기 호출): 각 사용자 타임존 08:00 발송 윈도에 든 대상만 발송(멱등).
     * date 지정: 운영/수동 — 해당 일자 전체 발송.
     */
    @PostMapping("/dispatch:run")
    fun dispatch(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): DispatchResult =
        DispatchResult(if (date != null) dispatch.dispatchForDate(date) else dispatch.dispatchDueAt(Instant.now()))
}
