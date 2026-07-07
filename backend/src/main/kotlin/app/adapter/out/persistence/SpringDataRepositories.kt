package app.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDate

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByUsername(username: String): UserEntity?
    fun existsByUsername(username: String): Boolean
    fun existsByNickname(nickname: String): Boolean
    fun findByPushPermission(pushPermission: String): List<UserEntity>
    fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity?
}

interface EditionJpaRepository : JpaRepository<EditionEntity, Long> {
    fun findByComboKeyAndLanguageAndIssueDate(comboKey: String, language: String, issueDate: LocalDate): EditionEntity?
    fun findFirstByComboKeyAndLanguageAndIssueDateLessThanOrderByIssueDateDesc(
        comboKey: String, language: String, issueDate: LocalDate,
    ): EditionEntity?
    fun findByComboKeyAndLanguageOrderByIssueDateDesc(comboKey: String, language: String): List<EditionEntity>
}

interface UserEditionStateJpaRepository : JpaRepository<UserEditionStateEntity, Long> {
    fun findByUserIdAndEditionId(userId: Long, editionId: Long): UserEditionStateEntity?
    fun findByUserIdAndEditionIdInAndReadIsTrue(userId: Long, editionIds: List<Long>): List<UserEditionStateEntity>
    fun findByUserIdAndBookmarkedIsTrueOrderByBookmarkedAtDesc(userId: Long): List<UserEditionStateEntity>
}

interface SlotJpaRepository : JpaRepository<SlotEntity, Long> {
    fun findByUserId(userId: Long): List<SlotEntity>
    fun findByUserIdAndDeletedAtIsNull(userId: Long): List<SlotEntity>
    fun countByUserIdAndDeletedAtIsNull(userId: Long): Long

    @Modifying
    @Query("update SlotEntity s set s.deletedAt = :ts where s.id = :id and s.userId = :userId and s.deletedAt is null")
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long, @Param("ts") ts: Instant): Int
}

interface CategoryJpaRepository : JpaRepository<CategoryEntity, Long> {
    fun findByActiveTrueOrderBySortOrder(): List<CategoryEntity>
}
