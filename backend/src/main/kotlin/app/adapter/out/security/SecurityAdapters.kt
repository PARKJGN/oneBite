package app.adapter.out.security

import app.application.port.out.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHasher : PasswordHasher {
    private val encoder = BCryptPasswordEncoder()
    override fun hash(raw: String): String = encoder.encode(raw)
    override fun matches(raw: String, hash: String): Boolean = encoder.matches(raw, hash)
}

// TokenIssuer 구현은 JwtTokenIssuer(JWT HS256)로 대체됨.
