package app.adapter

import app.domain.port.out.PushJob
import app.domain.port.out.PushJobPublisher
import app.domain.port.out.PushSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import java.time.LocalDate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Kafka 푸시 팬아웃 통합 테스트(T048/T040): 프로듀서가 발행한 잡을 컨슈머가 받아
 * PushSender로 디스패치하는지 실제 Kafka 브로커로 검증. Docker 없으면 자동 스킵.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = ["onebite.messaging.kafka=true"])
class KafkaPushDispatchIT {

    companion object {
        @Container @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("onebite").withUsername("onebite").withPassword("onebite")

        @Container @JvmStatic
        val kafka = KafkaContainer("apache/kafka:3.8.0")

        @JvmStatic
        @org.springframework.test.context.DynamicPropertySource
        fun props(registry: org.springframework.test.context.DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    // 컨슈머가 호출하는 PushSender를 캡처용으로 오버라이드
    @TestConfiguration
    class CaptureConfig {
        val received = ConcurrentLinkedQueue<PushJob>()
        val latch = CountDownLatch(1)

        @Bean @Primary
        fun capturingSender(): PushSender = PushSender { job ->
            received += job
            latch.countDown()
            true
        }
    }

    @Autowired lateinit var publisher: PushJobPublisher
    @Autowired lateinit var capture: CaptureConfig

    @Test
    fun `발행한 푸시 잡을 컨슈머가 받아 PushSender로 전달한다`() {
        publisher.publish(PushJob(userId = 42L, issueDate = LocalDate.parse("2026-06-24"), editionIds = listOf(1L, 2L)))

        assertTrue(capture.latch.await(20, TimeUnit.SECONDS), "컨슈머가 메시지를 수신하지 못함")
        val job = capture.received.first()
        assertEquals(42L, job.userId)
        assertEquals(listOf(1L, 2L), job.editionIds)
    }
}
