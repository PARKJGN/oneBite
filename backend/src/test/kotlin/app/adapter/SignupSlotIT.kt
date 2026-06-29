package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.SlotController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * US1 통합 테스트(T024): 가입 → 슬롯 생성/조회가 실제 PostgreSQL + Flyway에서 동작.
 * Docker가 없으면 클래스 전체가 자동 스킵된다(disabledWithoutDocker = true).
 */
class SignupSlotIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var slot: SlotController

    @Test
    fun `가입 후 슬롯을 생성하고 조회한다`() {
        val signup = auth.signup(
            AuthController.SignupRequest("alice", "password123", "앨리스", recoveryEmail = null),
        )
        assertTrue(signup.userId > 0)

        slot.create(signup.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        val list = slot.list(signup.userId)

        assertEquals(1, list.size)
        assertEquals("정치 · 경제", list.first().categoryLine)
    }

    @Test
    fun `카테고리 목록은 시드된 10개를 반환한다`() {
        assertEquals(10, slot.categories("ko").size)
    }
}
