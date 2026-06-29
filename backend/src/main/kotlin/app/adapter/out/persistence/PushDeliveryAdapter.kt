package app.adapter.out.persistence

import app.application.port.out.PushDeliveryRepository
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "push_delivery")
class PushDeliveryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    var issueDate: LocalDate = LocalDate.EPOCH,
    var scheduledAt: Instant = Instant.EPOCH,
    var dispatchedAt: Instant? = null,
    var status: String = "queued",
    var attemptCount: Int = 0,
)

interface PushDeliveryJpaRepository : JpaRepository<PushDeliveryEntity, Long> {
    fun existsByUserIdAndIssueDate(userId: Long, issueDate: LocalDate): Boolean
}

@Component
class PushDeliveryAdapter(private val jpa: PushDeliveryJpaRepository) : PushDeliveryRepository {
    override fun existsForDate(userId: Long, issueDate: LocalDate): Boolean =
        jpa.existsByUserIdAndIssueDate(userId, issueDate)

    override fun record(userId: Long, issueDate: LocalDate, scheduledAtUtc: Instant) {
        jpa.save(
            PushDeliveryEntity(
                userId = userId, issueDate = issueDate, scheduledAt = scheduledAtUtc,
                dispatchedAt = scheduledAtUtc, status = "dispatched", attemptCount = 1,
            ),
        )
    }
}
