package app.adapter.`in`.web

import app.application.port.`in`.LibraryEditionView
import app.application.port.`in`.LibrarySlotView
import app.application.port.`in`.LibraryUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 라이브러리 히스토리(FR-011c) 2단계. MVP: 사용자 식별 X-User-Id 헤더. */
@RestController
@RequestMapping("/library")
class LibraryController(private val library: LibraryUseCase) {

    @GetMapping("/slots")
    fun slots(@CurrentUserId userId: Long): List<LibrarySlotView> = library.slots(userId)

    @GetMapping("/editions")
    fun editions(
        @CurrentUserId userId: Long,
        @RequestParam comboKey: String,
    ): List<LibraryEditionView> = library.editions(userId, comboKey)
}
