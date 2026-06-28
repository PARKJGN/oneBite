package app.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * 내부 트리거(/internal 경로) 인증. n8n 등 신뢰된 호출자만 공유 시크릿(X-Internal-Token)으로 접근.
 * fail-closed: 토큰 미설정이거나 헤더 불일치면 401 (실수로 열려 있지 않게). 비교는 상수시간.
 * 설정: onebite.internal.token (env ONEBITE_INTERNAL_TOKEN).
 */
class InternalTokenFilter(private val token: String) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (request.requestURI.startsWith("/internal/")) {
            val provided = request.getHeader(HEADER).orEmpty()
            val ok = token.isNotBlank() && constantTimeEquals(provided, token)
            if (!ok) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json;charset=UTF-8"
                response.writer.write("{\"message\":\"내부 인증 실패\"}")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    companion object { const val HEADER = "X-Internal-Token" }
}
