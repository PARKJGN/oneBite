package app.config

import app.adapter.out.security.JwtTokenIssuer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * 무상태 보안. 공개 경로(가입·로그인·카테고리)와 /internal(공유 시크릿) 외에는 JWT 인증 필수.
 * - InternalTokenFilter: internal 경로 공유 시크릿 검증
 * - JwtAuthFilter: Authorization: Bearer access JWT → SecurityContext에 userId 설정
 */
@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwt: JwtTokenIssuer,
        @Value("\${onebite.internal.token:}") internalToken: String,
    ): SecurityFilterChain {
        http
            .cors { } // 아래 corsConfigurationSource 빈 사용(웹 클라이언트 교차 출처 허용)
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**", "/categories", "/error").permitAll()
                it.requestMatchers("/internal/**").permitAll() // InternalTokenFilter가 별도 보호
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) } // 미인증 401
            .addFilterBefore(InternalTokenFilter(internalToken), AuthorizationFilter::class.java)
            .addFilterBefore(JwtAuthFilter(jwt), AuthorizationFilter::class.java)
        return http.build()
    }

    /**
     * 개발용 CORS. Expo 웹·Next.js 등 로컬 프런트(localhost 임의 포트)에서의 호출을 허용한다.
     * 인증은 Authorization: Bearer 헤더 방식이라 자격증명 쿠키는 불필요(allowCredentials=false).
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
            allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }
}
