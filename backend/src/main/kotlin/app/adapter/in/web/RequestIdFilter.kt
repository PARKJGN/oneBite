package app.adapter.`in`.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청마다 추적 ID(requestId)를 부여한다. 표준 응답 봉투(ApiResponse.requestId)와
 * 응답 헤더(X-Request-Id), 로깅 MDC 에 같은 값을 실어 요청을 한 줄로 추적할 수 있게 한다.
 * 클라이언트가 X-Request-Id 를 보내면 그 값을 이어받는다(분산 추적).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val id = request.getHeader(HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        request.setAttribute(ATTR, id)
        response.setHeader(HEADER, id)
        MDC.put(MDC_KEY, id)
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    companion object {
        const val HEADER = "X-Request-Id"
        const val ATTR = "onebite.requestId"
        const val MDC_KEY = "requestId"

        /** 현재 요청의 추적 ID(없으면 "-"). ApiResponse/예외핸들러가 사용. */
        fun current(): String =
            (
                RequestContextHolder.getRequestAttributes()
                    ?.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST) as? String
                ) ?: "-"
    }
}