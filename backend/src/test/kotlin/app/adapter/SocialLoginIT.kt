package app.adapter

import app.adapter.`in`.web.SocialAuthController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/** 소셜 로그인 통합 테스트(FR-001c): 실제 Postgres에서 가입(upsert)·재로그인 idempotency. */
class SocialLoginIT : IntegrationTest() {

    @Autowired lateinit var social: SocialAuthController

    @Test
    fun `소셜 로그인은 최초 가입 후 재로그인 시 같은 계정을 재사용한다`() {
        val first = social.login(SocialAuthController.SocialLoginRequest("kakao", "kakao-uid-1:앨리스"))
        assertTrue(first.isNew)

        val second = social.login(SocialAuthController.SocialLoginRequest("kakao", "kakao-uid-1"))
        assertFalse(second.isNew)
        assertEquals(first.userId, second.userId) // (provider, providerId) UNIQUE → 동일 계정
    }
}
