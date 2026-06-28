package app.config

import app.adapter.out.security.JwtTokenIssuer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorization: Bearer <access JWT> 검증 → 유효하면 SecurityContext에 userId(principal) 설정.
 * 토큰이 없거나 무효면 인증 미설정 → SecurityConfig의 authenticated() 경로에서 401.
 */
class JwtAuthFilter(private val jwt: JwtTokenIssuer) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val userId = jwt.verify(header.substring(7).trim())
            if (userId != null) {
                // 유효 토큰이면 인증 설정(익명 인증을 덮어씀 — AnonymousAuthenticationFilter가 먼저 도는 순서 무관)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userId, null, emptyList())
            }
        }
        filterChain.doFilter(request, response)
    }
}
