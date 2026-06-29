package app.adapter.`in`.web

import app.application.port.`in`.EditionDetailView
import app.application.port.`in`.ReadEditionUseCase
import app.application.port.`in`.TodayView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/** 오늘 발송분·상세 열람(VIII). MVP: 사용자 식별 X-User-Id 헤더. */
@RestController
class EditionController(private val read: ReadEditionUseCase) {

    @GetMapping("/today")
    fun today(@CurrentUserId userId: Long): TodayView = read.today(userId)

    @GetMapping("/editions/{id}")
    fun edition(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ): EditionDetailView = read.edition(userId, id)
}
