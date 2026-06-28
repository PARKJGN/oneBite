package app.adapter

import app.adapter.`in`.web.SocialAuthController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/** 소셜 로그인 통합 테스트(FR-001c): 실제 Postgres에서 가입(upsert)·재로그인 idempotency. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SocialLoginIT {

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
