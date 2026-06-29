package app.adapter.`in`.web

import app.application.port.`in`.HomeUseCase
import app.application.port.`in`.YesterdayHighlightsView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 홈 "어제 핵심 뉴스"(FR-021). 페이지당 5개 기본. MVP: 사용자 식별 X-User-Id. */
@RestController
@RequestMapping("/home")
class HomeController(private val home: HomeUseCase) {

    @GetMapping("/yesterday")
    fun yesterday(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
    ): YesterdayHighlightsView = home.yesterdayHighlights(userId, page, size)
}
