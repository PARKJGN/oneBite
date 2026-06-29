package app.adapter

import app.adapter.out.security.JwtTokenIssuer
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트 공통 베이스: 실제 PostgreSQL(Testcontainers) + Flyway + MockMvc.
 * - Docker 없으면 전체 스킵(disabledWithoutDocker).
 * - postgres 컨테이너는 static 이라 전 IT 가 1개를 공유(클래스마다 재기동 안 함).
 * - Kafka 자동구성은 비활성(대부분 IT 가 미사용). Kafka 가 필요한 IT 는 이 베이스를 쓰지 않고 자체 구성한다.
 * - HTTP 검증용 mockMvc/objectMapper 와 보호 엔드포인트용 bearer() 헬퍼 제공.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTest {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var jwt: JwtTokenIssuer

    /** 보호 엔드포인트 호출용 Authorization 헤더 값(JWT). */
    protected fun bearer(userId: Long): String = "Bearer ${jwt.issue(userId)}"

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
            registry.add("spring.autoconfigure.exclude") {
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
            }
        }
    }
}
