package app.adapter

import app.adapter.out.security.JwtTokenIssuer
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer

/**
 * 통합 테스트 공통 베이스: 실제 PostgreSQL(Testcontainers) + Flyway + MockMvc.
 * - postgres 는 싱글톤 컨테이너 — 한 번만 start() 하고 전 IT 가 공유, JVM 종료 시 Ryuk 이 정리.
 *   (주의: @Container/@Testcontainers 로 베이스에 두면 첫 서브클래스 종료 시 stop 되어 이후 클래스가 깨진다.)
 * - Kafka 자동구성은 비활성(대부분 IT 가 미사용). Kafka 가 필요한 IT 는 이 베이스를 쓰지 않고 자체 구성한다.
 * - HTTP 검증용 mockMvc/objectMapper 와 보호 엔드포인트용 bearer() 헬퍼 제공.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTest {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var jwt: JwtTokenIssuer
    @Autowired protected lateinit var jdbc: JdbcTemplate

    /** 보호 엔드포인트 호출용 Authorization 헤더 값(JWT). */
    protected fun bearer(userId: Long): String = "Bearer ${jwt.issue(userId)}"

    /**
     * 싱글톤 DB 를 전 IT 가 공유하므로 테스트마다 데이터를 비워 격리한다.
     * 시드(categories·rss_sources)와 flyway 이력은 보존. RESTART IDENTITY 로 ID 도 초기화(예측 가능한 단언).
     */
    @BeforeEach
    fun cleanDatabase() {
        val tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                "AND tablename NOT IN ('flyway_schema_history', 'categories', 'rss_sources')",
            String::class.java,
        )
        if (tables.isNotEmpty()) {
            jdbc.execute("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE")
        }
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("onebite").withUsername("onebite").withPassword("onebite")
            .also { it.start() }

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
