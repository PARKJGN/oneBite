package app.adapter.out.persistence

import app.application.port.out.DeviceTokenRepository
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    var token: String = "",
    var platform: String = "android",
    var updatedAt: Instant = Instant.now(),
)

interface DeviceTokenJpaRepository : JpaRepository<DeviceTokenEntity, Long> {
    fun findByToken(token: String): DeviceTokenEntity?
    fun findByUserId(userId: Long): List<DeviceTokenEntity>
    fun deleteByUserId(userId: Long): Long
}

@Component
class DeviceTokenAdapter(private val jpa: DeviceTokenJpaRepository) : DeviceTokenRepository {

    @Transactional
    override fun register(userId: Long, token: String, platform: String) {
        val existing = jpa.findByToken(token)
        if (existing != null) {
            existing.userId = userId
            existing.platform = platform
            existing.updatedAt = Instant.now()
            jpa.save(existing)
        } else {
            jpa.save(DeviceTokenEntity(userId = userId, token = token, platform = platform))
        }
    }

    @Transactional(readOnly = true)
    override fun findActiveTokens(userId: Long): List<String> =
        jpa.findByUserId(userId).map { it.token }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        jpa.deleteByUserId(userId)
    }
}
