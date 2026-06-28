package app.adapter

import app.adapter.`in`.web.AuthController
import app.domain.InvalidCredentialsException
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Refresh 토큰: 로그인→회전(새 쌍, 옛 토큰 폐기)→재사용 거부→로그아웃 후 거부.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class RefreshTokenIT {

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

    @Test
    fun `로그인 후 refresh 회전·재사용 거부·로그아웃`() {
        auth.signup(AuthController.SignupRequest("rita", "password123", "리타"))
        val r1 = auth.login(AuthController.LoginRequest("rita", "password123")).refreshToken

        // 회전: r1 → 새 쌍(r2), r1은 폐기
        val r2 = auth.refresh(AuthController.RefreshRequest(r1)).refreshToken
        assertNotEquals(r1, r2)

        // 옛 refresh(r1) 재사용 거부
        assertThrows(InvalidCredentialsException::class.java) {
            auth.refresh(AuthController.RefreshRequest(r1))
        }

        // 새 refresh(r2)는 동작 → r3
        val r3 = auth.refresh(AuthController.RefreshRequest(r2)).refreshToken

        // 로그아웃(r3 폐기) 후 거부
        auth.logout(AuthController.RefreshRequest(r3))
        assertThrows(InvalidCredentialsException::class.java) {
            auth.refresh(AuthController.RefreshRequest(r3))
        }
    }
}
