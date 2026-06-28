package app.adapter.`in`.web

import app.application.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 계정 삭제(FR-018a). MVP: 사용자 식별 X-User-Id. */
@RestController
class AccountController(private val account: AccountService) {
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@CurrentUserId userId: Long) = account.delete(userId)
}
