package app.adapter.out.persistence

import app.domain.model.Category
import app.domain.model.Language
import app.domain.model.PushPermission
import app.domain.model.Slot
import app.domain.model.User
import app.application.port.out.CategoryRepository
import app.application.port.out.SlotRepository
import app.application.port.out.UserRepository
import org.springframework.stereotype.Component
import java.time.Instant

/** 저장 포트 구현 — 도메인 ↔ JPA 엔티티 매핑(헥사고날: 도메인 오염 방지). */

@Component
class UserPersistenceAdapter(private val jpa: UserJpaRepository) : UserRepository {
    override fun save(user: User): User = jpa.save(user.toEntity()).toDomain()
    override fun findByUsername(username: String): User? = jpa.findByUsername(username)?.toDomain()
    override fun existsByUsername(username: String): Boolean = jpa.existsByUsername(username)
    override fun findById(id: Long): User? = jpa.findById(id).orElse(null)?.toDomain()
    override fun findByProvider(provider: String, providerId: String): User? =
        jpa.findByProviderAndProviderId(provider, providerId)?.toDomain()
    override fun delete(userId: Long) = jpa.deleteById(userId)

    private fun User.toEntity() = UserEntity(
        id = id, username = username, passwordHash = passwordHash, nickname = nickname,
        recoveryEmail = recoveryEmail, timezone = timezone,
        outputLanguage = outputLanguage.name.lowercase(), pushPermission = pushPermission.name.lowercase(),
        provider = provider, providerId = providerId,
    )

    private fun UserEntity.toDomain() = User(
        id = id, username = username, passwordHash = passwordHash, nickname = nickname,
        recoveryEmail = recoveryEmail, timezone = timezone,
        outputLanguage = Language.valueOf(outputLanguage.uppercase()),
        pushPermission = PushPermission.valueOf(pushPermission.uppercase()),
        provider = provider, providerId = providerId,
    )
}

@Component
class SlotPersistenceAdapter(private val jpa: SlotJpaRepository) : SlotRepository {
    override fun save(slot: Slot): Slot =
        jpa.save(SlotEntity(id = slot.id, userId = slot.userId, categoryCodes = slot.categoryCodes.toMutableList())).toDomain()
    override fun findById(slotId: Long): Slot? = jpa.findById(slotId).orElse(null)?.toDomain()
    override fun findActiveByUserId(userId: Long): List<Slot> =
        jpa.findByUserIdAndDeletedAtIsNull(userId).map { it.toDomain() }
    override fun findAllByUserId(userId: Long): List<Slot> = jpa.findByUserId(userId).map { it.toDomain() }
    override fun countActiveByUserId(userId: Long): Int = jpa.countByUserIdAndDeletedAtIsNull(userId).toInt()
    override fun deactivate(slotId: Long, userId: Long): Boolean =
        jpa.softDelete(slotId, userId, Instant.now()) > 0

    private fun SlotEntity.toDomain() = Slot(
        id = id, userId = userId, categoryCodes = categoryCodes.toList(), active = deletedAt == null,
        createdAt = createdAt,
    )
}

@Component
class CategoryPersistenceAdapter(private val jpa: CategoryJpaRepository) : CategoryRepository {
    override fun findAllActive(): List<Category> =
        jpa.findByActiveTrueOrderBySortOrder().map { Category(it.code, it.nameKo, it.nameEn, it.active) }
}
