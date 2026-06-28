package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.out.security.JwtTokenIssuer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * JWT 인증(회귀): 보호 경로는 토큰 없으면 401, 유효 JWT면 200. 공개 경로는 토큰 없이 200.
 * (X-User-Id 헤더 무방비 갭이 막혔는지 HTTP 레벨로 검증)
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthIT {

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

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var auth: AuthController
    @Autowired lateinit var jwt: JwtTokenIssuer

    @Test
    fun `보호 경로는 토큰 없으면 401, 유효 JWT면 200`() {
        val user = auth.signup(AuthController.SignupRequest("hank", "password123", "행크"))

        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized)

        val token = jwt.issue(user.userId)
        mockMvc.perform(get("/me").header("Authorization", "Bearer $token")).andExpect(status().isOk)
    }

    @Test
    fun `공개 경로(categories)는 토큰 없이 200`() {
        mockMvc.perform(get("/categories")).andExpect(status().isOk)
    }
}
