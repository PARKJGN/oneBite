package app.application

import app.adapter.out.security.InMemoryLoginAttemptGuard
import app.domain.InvalidCredentialsException
import app.domain.TooManyLoginAttemptsException
import app.domain.model.User
import app.domain.port.`in`.LoginCommand
import app.domain.port.out.EmailSender
import app.domain.port.out.LoginAttemptGuard
import app.domain.port.out.PasswordHasher
import app.domain.port.out.PasswordResetTokenStore
import app.domain.port.out.RefreshTokenStore
import app.domain.port.out.TokenIssuer
import app.domain.port.out.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** FR-001b 비밀번호 재설정: 복구 이메일 발송 → 토큰 검증 → 비밀번호 변경. */
class AuthResetTest {

    private class FakeUsers(seed: User) : UserRepository {
        val store = mutableListOf(seed)
        override fun save(user: User): User {
            val i = store.indexOfFirst { it.id == user.id }
            if (i >= 0) { store[i] = user; return user }
            val saved = user.copy(id = (store.size + 1).toLong()); store += saved; return saved
        }
        override fun findByUsername(username: String) = store.firstOrNull { it.username == username }
        override fun existsByUsername(username: String) = store.any { it.username == username }
        override fun findById(id: Long) = store.firstOrNull { it.id == id }
        override fun findByProvider(provider: String, providerId: String): User? = null
        override fun delete(userId: Long) { store.removeIf { it.id == userId } }
    }

    private val hasher = object : PasswordHasher {
        override fun hash(raw: String) = "H:$raw"
        override fun matches(raw: String, hash: String) = hash == "H:$raw"
    }
    private val tokens = TokenIssuer { "login-$it" }
    private val resetTokens = object : PasswordResetTokenStore {
        val map = HashMap<String, Long>()
        override fun issue(userId: Long): String { val t = "reset-$userId"; map[t] = userId; return t }
        override fun consume(token: String): Long? = map.remove(token)
    }
    private val refreshTokens = object : RefreshTokenStore {
        val map = HashMap<String, Long>(); var seq = 0
        override fun issue(userId: Long): String { val t = "rt-$userId-${seq++}"; map[t] = userId; return t }
        override fun consume(rawToken: String): Long? = map.remove(rawToken)
        override fun revokeAll(userId: Long) { map.values.removeIf { it == userId } }
    }
    private val loginGuard = object : LoginAttemptGuard {
        override fun assertNotLocked(key: String) {}
        override fun recordFailure(key: String) {}
        override fun recordSuccess(key: String) {}
    }

    @Test
    fun `복구 이메일이 있으면 재설정 링크를 보내고, 토큰으로 비밀번호를 바꾼다`() {
        val users = FakeUsers(User(1, "alice", "H:old", "앨리스", recoveryEmail = "a@b.com"))
        val captured = mutableListOf<Pair<String, String>>()
        val svc = AuthService(users, hasher, tokens, resetTokens, EmailSender { e, t -> captured += e to t }, refreshTokens, loginGuard)

        svc.requestPasswordReset("alice")
        assertEquals(1, captured.size)
        assertEquals("a@b.com", captured.first().first)

        svc.confirmPasswordReset(captured.first().second, "newpass12")
        assertEquals("H:newpass12", users.findById(1)!!.passwordHash)
    }

    @Test
    fun `복구 이메일이 없으면 메일을 보내지 않는다`() {
        val users = FakeUsers(User(1, "bob", "H:old", "밥", recoveryEmail = null))
        val captured = mutableListOf<Pair<String, String>>()
        AuthService(users, hasher, tokens, resetTokens, EmailSender { e, t -> captured += e to t }, refreshTokens, loginGuard)
            .requestPasswordReset("bob")
        assertTrue(captured.isEmpty())
    }

    @Test
    fun `유효하지 않은 토큰으로 재설정하면 거부된다`() {
        val users = FakeUsers(User(1, "alice", "H:old", "앨리스", recoveryEmail = "a@b.com"))
        val svc = AuthService(users, hasher, tokens, resetTokens, EmailSender { _, _ -> }, refreshTokens, loginGuard)
        assertThrows(IllegalArgumentException::class.java) { svc.confirmPasswordReset("bogus", "newpass12") }
        assertNull(resetTokens.consume("bogus"))
    }

    @Test
    fun `로그인 연속 실패가 임계치를 넘으면 계정이 잠긴다`() {
        val users = FakeUsers(User(1, "alice", "H:secret123", "앨리스"))
        val guard = InMemoryLoginAttemptGuard(maxAttempts = 3, lockMinutes = 15)
        val svc = AuthService(users, hasher, tokens, resetTokens, EmailSender { _, _ -> }, refreshTokens, guard)

        // 3회 연속 실패 → 잠금 발동
        repeat(3) {
            assertThrows(InvalidCredentialsException::class.java) { svc.login(LoginCommand("alice", "wrong")) }
        }
        // 잠긴 뒤에는 올바른 비밀번호여도 429(TooMany)로 거부
        assertThrows(TooManyLoginAttemptsException::class.java) { svc.login(LoginCommand("alice", "secret123")) }
    }
}
