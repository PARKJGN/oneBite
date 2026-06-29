package app.adapter.`in`.web

import app.domain.model.Language
import app.application.port.`in`.SlotUseCase
import app.application.port.out.CategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 슬롯·카테고리 API. MVP에서는 인증 필터 도입 전이라 사용자 식별을 X-User-Id 헤더로 받는다
 * (후속 증분에서 토큰 인증으로 대체).
 */
@RestController
class SlotController(
    private val slots: SlotUseCase,
    private val categories: CategoryRepository,
) {

    data class CategoryResponse(val code: String, val name: String)
    data class SlotResponse(val id: Long, val categoryCodes: List<String>, val categoryLine: String)
    data class CreateSlotRequest(val categoryCodes: List<String>)

    @GetMapping("/categories")
    fun categories(@RequestParam(defaultValue = "ko") lang: String): List<CategoryResponse> {
        val language = if (lang.equals("en", true)) Language.EN else Language.KO
        return categories.findAllActive().map { CategoryResponse(it.code, it.nameFor(language)) }
    }

    @GetMapping("/slots")
    fun list(@CurrentUserId userId: Long): List<SlotResponse> =
        slots.list(userId).map { SlotResponse(it.id, it.categoryCodes, it.categoryLine) }

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @RequestBody req: CreateSlotRequest,
    ): SlotResponse {
        val v = slots.create(userId, req.categoryCodes)
        return SlotResponse(v.id, v.categoryCodes, v.categoryLine)
    }

    @PutMapping("/slots/{id}")
    fun update(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
        @RequestBody req: CreateSlotRequest,
    ): SlotResponse {
        val v = slots.update(userId, id, req.categoryCodes)
        return SlotResponse(v.id, v.categoryCodes, v.categoryLine)
    }

    @DeleteMapping("/slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ) {
        slots.delete(userId, id) // 소프트 삭제 — 라이브러리 유지(FR-003b)
    }
}
