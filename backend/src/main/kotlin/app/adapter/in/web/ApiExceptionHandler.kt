package app.adapter.`in`.web

import app.domain.InvalidCredentialsException
import app.domain.NicknameAlreadyExistsException
import app.domain.TooManyLoginAttemptsException
import app.domain.UnknownCategoryException
import app.domain.UserNotFoundException
import app.domain.UsernameAlreadyExistsException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 도메인 예외 → 표준 에러 응답(ApiResponse) 매핑. code(ErrorCode) + 사용자 메시지 + requestId.
 * 사용자 대면 메시지는 간결하게(상세는 서버 로깅·requestId 로 추적).
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private fun of(code: ErrorCode, message: String?) =
        ResponseEntity.status(code.status).body(ApiResponse.error(code, message ?: code.defaultMessage))

    @ExceptionHandler(UsernameAlreadyExistsException::class)
    fun onDuplicateUsername(e: UsernameAlreadyExistsException) = of(ErrorCode.USERNAME_ALREADY_EXISTS, e.message)

    @ExceptionHandler(NicknameAlreadyExistsException::class)
    fun onDuplicateNickname(e: NicknameAlreadyExistsException) = of(ErrorCode.NICKNAME_ALREADY_EXISTS, e.message)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun onInvalidCreds(e: InvalidCredentialsException) = of(ErrorCode.INVALID_CREDENTIALS, e.message)

    @ExceptionHandler(UserNotFoundException::class)
    fun onNotFound(e: UserNotFoundException) = of(ErrorCode.USER_NOT_FOUND, e.message)

    @ExceptionHandler(TooManyLoginAttemptsException::class)
    fun onTooManyAttempts(e: TooManyLoginAttemptsException) =
        ResponseEntity.status(ErrorCode.TOO_MANY_ATTEMPTS.status)
            .header("Retry-After", e.retryAfterSeconds.toString())
            .body(ApiResponse.error(ErrorCode.TOO_MANY_ATTEMPTS, e.message ?: ErrorCode.TOO_MANY_ATTEMPTS.defaultMessage))

    @ExceptionHandler(UnknownCategoryException::class, IllegalArgumentException::class)
    fun onBadRequest(e: RuntimeException) = of(ErrorCode.BAD_REQUEST, e.message)
}
