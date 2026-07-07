package app.adapter.`in`.web

import org.springframework.http.HttpStatus

/** 에러 코드 ↔ HTTP 상태 매핑. 클라이언트는 code(문자열)로 분기한다. */
enum class ErrorCode(val status: HttpStatus, val defaultMessage: String) {
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
}