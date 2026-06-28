package app.adapter.out.security

import app.domain.port.out.TokenIssuer
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Access 토큰 발급·검증 (JWT HS256, 무상태). 시크릿은 jwt.secret(env JWT_SECRET).
 * 미설정 시 dev 기본키로 동작하며 경고 — 운영에서는 반드시 JWT_SECRET 설정(≥32바이트).
 * Refresh 토큰은 별도(불투명·DB 저장, B 단계).
 */
@Component
class JwtTokenIssuer(
    @Value("\${jwt.secret:}") secret: String,
    @Value("\${jwt.access-ttl-minutes:30}") private val accessTtlMinutes: Long,
) : TokenIssuer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val key = run {
        val raw = secret.ifBlank {
            log.warn("jwt.secret 미설정 — dev 기본키 사용 중. 운영에서는 JWT_SECRET을 반드시 설정하세요.")
            DEV_SECRET
        }
        Keys.hmacShaKeyFor(raw.toByteArray(Charsets.UTF_8))
    }

    override fun issue(userId: Long): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date(now))
            .expiration(Date(now + accessTtlMinutes * 60_000))
            .signWith(key)
            .compact()
    }

    /** 서명·만료 검증 후 userId 반환. 유효하지 않으면 null. */
    fun verify(token: String): Long? = runCatching {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject.toLong()
    }.getOrNull()

    companion object {
        // HS256 최소 32바이트. dev 전용.
        private const val DEV_SECRET = "onebite-dev-insecure-jwt-secret-change-me-32+"
    }
}
