package app.adapter.out.persistence

import app.domain.port.out.RefreshTokenStore
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    var tokenHash: String = "",
    var expiresAt: Instant = Instant.EPOCH,
    var createdAt: Instant = Instant.now(),
)

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
    fun deleteByTokenHash(tokenHash: String): Long
    fun deleteByUserId(userId: Long): Long
}

@Component
class RefreshTokenAdapter(
    private val jpa: RefreshTokenJpaRepository,
    @Value("\${jwt.refresh-ttl-days:30}") private val ttlDays: Long,
) : RefreshTokenStore {

    private val random = SecureRandom()

    @Transactional
    override fun issue(userId: Long): String {
        val raw = randomToken()
        jpa.save(
            RefreshTokenEntity(
                userId = userId,
                tokenHash = sha256(raw),
                expiresAt = Instant.now().plus(ttlDays, ChronoUnit.DAYS),
            ),
        )
        return raw
    }

    @Transactional
    override fun consume(rawToken: String): Long? {
        val entity = jpa.findByTokenHash(sha256(rawToken)) ?: return null
        jpa.deleteByTokenHash(entity.tokenHash) // 단일 사용(회전) — 재사용 차단
        return if (entity.expiresAt.isAfter(Instant.now())) entity.userId else null
    }

    @Transactional
    override fun revokeAll(userId: Long) {
        jpa.deleteByUserId(userId)
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
