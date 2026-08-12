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
        // DB의 users_nickname_key UNIQUE(V11)를 흉내낸다 — 이게 없으면 충돌 버그를 테스트가 놓친다.
        override fun save(user: User): User {
            if (store.any { it.nickname == user.nickname && it.id != user.id }) {
                throw IllegalStateException("users_nickname_key 위반: ${user.nickname}")
            }
            val existingIdx = store.indexOfFirst { it.id != null && it.id == user.id }
            if (existingIdx >= 0) { store[existingIdx] = user; return user }
            val saved = user.copy(id = (store.size + 1).toLong()); store += saved; return saved
        }
        override fun findByUsername(username: String) = store.firstOrNull { it.username == username }
        override fun existsByUsername(username: String) = store.any { it.username == username }
        override fun existsByNickname(nickname: String) = store.any { it.nickname == nickname }
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
    fun `제공자 닉네임이 이미 있으면 접미사를 붙여 가입시킨다`() {
        val users = FakeUsers()
        val svc = SocialAuthService(users, verifier, tokens, refreshTokens, codeExchanger)
        svc.login(SocialLoginCommand("kakao", "kakao-1"))   // "소셜유저" 선점
        val r2 = svc.login(SocialLoginCommand("naver", "naver-1")) // 같은 닉네임을 원하는 다른 사람

        assertTrue(r2.isNew)
        assertEquals(2, users.store.size)
        assertEquals("소셜유저", users.store[0].nickname)
        assertEquals("소셜유저2", users.store[1].nickname)
    }

    @Test
    fun `별명 미동의 계정이 여럿이어도 모두 가입된다`() {
        // 네이버가 별명을 안 주면 검증기가 "사용자"로 떨어뜨린다 — 전부 같은 닉네임을 원하게 된다.
        val users = FakeUsers()
        val noNickname = SocialIdentityVerifier { provider, token ->
            SocialIdentity(provider, providerId = token, nickname = SocialAuthService.DEFAULT_NICKNAME)
        }
        val svc = SocialAuthService(users, noNickname, tokens, refreshTokens, codeExchanger)
        svc.login(SocialLoginCommand("naver", "naver-1"))
        svc.login(SocialLoginCommand("naver", "naver-2"))
        svc.login(SocialLoginCommand("naver", "naver-3"))

        assertEquals(3, users.store.size)
        assertEquals(listOf("사용자", "사용자2", "사용자3"), users.store.map { it.nickname })
    }

    @Test
    fun `닉네임 보정은 그 이름이 이미 있으면 건너뛴다`() {
        val users = FakeUsers()
        // 기본 닉네임으로 가입한 기존 소셜 사용자
        val basic = SocialIdentityVerifier { provider, token ->
            SocialIdentity(provider, providerId = token, nickname = SocialAuthService.DEFAULT_NICKNAME)
        }
        SocialAuthService(users, basic, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("naver", "naver-1"))
        // "소셜유저"를 이미 쓰는 다른 사람이 생긴다
        SocialAuthService(users, verifier, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("kakao", "kakao-1"))

        // 이제 naver-1이 별명 제공에 동의해 "소셜유저"를 원하지만, 이미 점유돼 있으므로 보정하지 않는다
        val r = SocialAuthService(users, verifier, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("naver", "naver-1"))

        assertFalse(r.isNew)
        assertEquals(2, users.store.size)
        assertEquals(SocialAuthService.DEFAULT_NICKNAME, users.store.first { it.provider == "naver" }.nickname)
    }

    @Test
    fun `지원하지 않는 제공자는 거부된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            SocialAuthService(FakeUsers(), verifier, tokens, refreshTokens, codeExchanger).login(SocialLoginCommand("facebook", "x"))
        }
    }
}
