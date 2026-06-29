package app.application

import app.domain.model.User
import app.application.port.`in`.SocialLoginCommand
import app.application.port.out.RefreshTokenStore
import app.application.port.out.SocialCodeExchanger
import app.application.port.out.SocialIdentity
import app.application.port.out.SocialIdentityVerifier
import app.application.port.out.TokenIssuer
import app.application.port.out.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocialAuthServiceTest {

    private class FakeUsers : UserRepository {
        val store = mutableListOf<User>()
        override fun save(user: User): User {
            val saved = user.copy(id = (store.size + 1).toLong()); store += saved; return saved
        }
        override fun findByUsername(username: String) = store.firstOrNull { it.username == username }
        override fun existsByUsername(username: String) = store.any { it.username == username }
        override fun findById(id: Long) = store.firstOrNull { it.id == id }
        override fun findByProvider(provider: String, providerId: String) =
            store.firstOrNull { it.provider == provider && it.providerId == providerId }
        override fun delete(userId: Long) { store.removeIf { it.id == userId } }
    }

    private val verifier = SocialIdentityVerifier { provider, token ->
        SocialIdentity(provider, providerId = token, nickname = "소셜유저")
    }
    private val tokens = TokenIssuer { "token-$it" }
    private val refreshTokens = object : RefreshTokenStore {
        val map = HashMap<String, Long>(); var seq = 0
        override fun issue(userId: Long): String { val t = "rt-$userId-${seq++}"; map[t] = userId; return t }
        override fun consume(rawToken: String): Long? = map.remove(rawToken)
        override fun revokeAll(userId: Long) { map.values.removeIf { it == userId } }
    }
    private val codeExchanger = SocialCodeExchanger { _, code, _, _ -> code } // 교환 결과를 token처럼 전달(fake verifier가 providerId로 해석)

    @Test
    fun `신규 소셜 사용자는 생성되고 isNew true`() {
        val users = FakeUsers()
        val r = SocialAuthService(users, verifier, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("kakao", "kakao-123"))
        assertTrue(r.isNew)
        assertEquals(1, users.store.size)
        val u = users.store.first()
        assertEquals("kakao", u.provider)
        assertEquals("kakao-123", u.providerId)
        assertEquals(null, u.passwordHash) // 소셜 사용자는 비번 없음
    }

    @Test
    fun `기존 소셜 사용자는 재사용되고 isNew false (중복 생성 없음)`() {
        val users = FakeUsers()
        val svc = SocialAuthService(users, verifier, tokens, refreshTokens, codeExchanger)
        svc.login(SocialLoginCommand("kakao", "kakao-123"))
        val r2 = svc.login(SocialLoginCommand("kakao", "kakao-123"))
        assertFalse(r2.isNew)
        assertEquals(1, users.store.size)
    }

    @Test
    fun `지원하지 않는 제공자는 거부된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            SocialAuthService(FakeUsers(), verifier, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("facebook", "x"))
        }
    }
}
