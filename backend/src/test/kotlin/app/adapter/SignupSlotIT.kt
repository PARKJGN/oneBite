package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.SlotController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * US1 통합 테스트(T024): 가입 → 슬롯 생성/조회가 실제 PostgreSQL + Flyway에서 동작.
 * Docker가 없으면 클래스 전체가 자동 스킵된다(disabledWithoutDocker = true).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SignupSlotIT {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("onebite").withUsername("onebite").withPassword("onebite")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            // Kafka는 이 테스트에서 사용하지 않음(자동 구성 비활성은 후속 정리)
            registry.add("spring.autoconfigure.exclude") {
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
            }
        }
    }

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
