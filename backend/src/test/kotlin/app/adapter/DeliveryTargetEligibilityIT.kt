package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.MeController
import app.adapter.`in`.web.SlotController
import app.application.port.out.DeliveryTargetQuery
import app.domain.service.ComboKey
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
 * 회귀(LazyInitializationException): findEligibleTargets는 슬롯의 지연 컬렉션(categoryCodes)을
 * 세션 안에서 읽어야 한다(OSIV off). 이 테스트는 @Transactional 없이 호출하므로,
 * 어댑터의 @Transactional(readOnly=true)이 빠지면 LazyInitializationException으로 실패한다.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DeliveryTargetEligibilityIT {

    companion object {
        @Container @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("onebite").withUsername("onebite").withPassword("onebite")

        @JvmStatic @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.autoconfigure.exclude") {
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
            }
        }
    }

    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var me: MeController
    @Autowired lateinit var slot: SlotController
    @Autowired lateinit var targets: DeliveryTargetQuery

    @Test
    fun `푸시 동의+슬롯 사용자는 지연 컬렉션 초기화 없이 eligible 타깃으로 조회된다`() {
        val user = auth.signup(AuthController.SignupRequest("evie", "password123", "이브"))
        me.update(user.userId, MeController.UpdateRequest(pushPermission = "granted"))
        slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))

        // @Transactional 없는 호출 — 어댑터가 세션을 안 열면 categoryCodes 접근 시 LazyInitializationException
        val target = targets.findEligibleTargets().single { it.userId == user.userId }

        assertEquals(listOf(ComboKey.of(listOf("politics", "economy"))), target.comboKeys)
        assertTrue(target.comboKeys.isNotEmpty())
    }
}
