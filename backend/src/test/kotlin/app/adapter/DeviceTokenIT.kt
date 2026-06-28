package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.DeviceController
import app.domain.port.out.DeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/** 기기 토큰 등록·조회 통합 테스트: 등록(멱등) 후 userId로 활성 토큰을 찾는다. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DeviceTokenIT {

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
    @Autowired lateinit var device: DeviceController
    @Autowired lateinit var tokens: DeviceTokenRepository

    @Test
    fun `기기 토큰을 등록하면 userId로 조회되고, 같은 토큰 재등록은 멱등이다`() {
        val user = auth.signup(AuthController.SignupRequest("frank", "password123", "프랭크"))
        device.register(user.userId, DeviceController.RegisterRequest("fcm-token-abc", "android"))
        device.register(user.userId, DeviceController.RegisterRequest("fcm-token-abc", "ios")) // 멱등(플랫폼만 갱신)

        val list = tokens.findActiveTokens(user.userId)
        assertEquals(listOf("fcm-token-abc"), list)
    }
}
