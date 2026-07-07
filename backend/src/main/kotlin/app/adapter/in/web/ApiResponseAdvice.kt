package app.adapter.`in`.web

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * 모든 앱 컨트롤러의 성공 응답을 ApiResponse 봉투로 감싼다.
 * 제외:
 *  - 이미 ApiResponse(예: 예외핸들러가 만든 에러 응답) → 이중 래핑 방지
 *  - null 본문(204 No Content / 202 Accepted 등 빈 응답) → 빈 응답 보존
 *  - String / ByteArray(문자열·바이너리 컨버터) → 컨버터 충돌 방지
 * basePackages 로 우리 컨트롤러에만 적용(actuator/springdoc 등 미개입).
 */
@RestControllerAdvice(basePackages = ["app.adapter.in.web", "app.adapter.in.trigger"])
class ApiResponseAdvice : ResponseBodyAdvice<Any> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body == null) return null
        if (body is ApiResponse<*>) return body
        if (body is String || body is ByteArray) return body
        return ApiResponse.ok(body)
    }
}