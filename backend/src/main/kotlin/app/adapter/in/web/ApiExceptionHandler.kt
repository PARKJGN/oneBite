package app.adapter.`in`.web

import app.domain.InvalidCredentialsException
import app.domain.TooManyLoginAttemptsException
import app.domain.UnknownCategoryException
import app.domain.UserNotFoundException
import app.domain.UsernameAlreadyExistsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 도메인 예외 → HTTP 매핑. 사용자 대면 메시지는 간결하게(상세는 서버 로깅). */
@RestControllerAdvice
class ApiExceptionHandler {

    data class ErrorResponse(val message: String)

    @ExceptionHandler(UsernameAlreadyExistsException::class)
    fun onDuplicate(e: UsernameAlreadyExistsException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "conflict"))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun onInvalidCreds(e: InvalidCredentialsException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message ?: "unauthorized"))

    @ExceptionHandler(UserNotFoundException::class)
    fun onNotFound(e: UserNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "not found"))

    @ExceptionHandler(TooManyLoginAttemptsException::class)
    fun onTooManyAttempts(e: TooManyLoginAttemptsException) =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", e.retryAfterSeconds.toString())
            .body(ErrorResponse(e.message ?: "too many requests"))

    @ExceptionHandler(UnknownCategoryException::class, IllegalArgumentException::class)
    fun onBadRequest(e: RuntimeException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "bad request"))
}
