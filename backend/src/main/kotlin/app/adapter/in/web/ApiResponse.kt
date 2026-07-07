package app.adapter.`in`.web

/**
 * 표준 API 응답 봉투 — 모든 성공/에러 응답을 감싼다.
 * - code: 성공은 "OK", 실패는 ErrorCode 이름(예: NICKNAME_ALREADY_EXISTS). 클라이언트는 이 문자열로 분기.
 * - message: 사용자 대면 메시지(성공은 "OK").
 * - requestId: 요청 추적 ID(응답 헤더 X-Request-Id 와 동일값).
 * - data: 성공 페이로드(에러는 null).
 */
data class ApiResponse<T>(
    val code: String,
    val message: String,
    val requestId: String,
    val data: T? = null,
) {
    companion object {
        fun <T> ok(data: T?): ApiResponse<T> =
            ApiResponse("OK", "OK", RequestIdFilter.current(), data)

        fun error(code: ErrorCode, message: String): ApiResponse<Nothing?> =
            ApiResponse(code.name, message, RequestIdFilter.current(), null)
    }
}