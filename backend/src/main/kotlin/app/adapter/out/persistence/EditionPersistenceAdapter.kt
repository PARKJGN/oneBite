package app.adapter.out.persistence

import app.domain.model.CrossInsight
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionStatus
import app.domain.model.Language
import app.application.port.out.DeliveryTarget
import app.application.port.out.DeliveryTargetQuery
import app.application.port.out.EditionRepository
import app.application.port.out.UserEditionStateRepository
import app.domain.service.ComboKey
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Component
class EditionPersistenceAdapter(
    private val jpa: EditionJpaRepository,
    private val om: ObjectMapper,
) : EditionRepository {

    override fun findByKey(comboKey: String, language: Language, issueDate: LocalDate): Edition? =
        jpa.findByComboKeyAndLanguageAndIssueDate(comboKey, language.name.lowercase(), issueDate)?.toDomain()

    override fun findLatestBefore(comboKey: String, language: Language, issueDate: LocalDate): Edition? =
        jpa.findFirstByComboKeyAndLanguageAndIssueDateLessThanOrderByIssueDateDesc(
            comboKey, language.name.lowercase(), issueDate,
        )?.toDomain()

    override fun findById(id: Long): Edition? = jpa.findById(id).orElse(null)?.toDomain()

    override fun findByComboAndLanguage(comboKey: String, language: Language): List<Edition> =
        jpa.findByComboKeyAndLanguageOrderByIssueDateDesc(comboKey, language.name.lowercase()).map { it.toDomain() }

    override fun save(edition: Edition): Edition {
        val e = EditionEntity(
            id = edition.id,
            comboKey = edition.comboKey,
            language = edition.language.name.lowercase(),
            issueDate = edition.issueDate,
            oneLine = edition.content.oneLine,
            marketSummary = om.writeValueAsString(edition.content.marketSummary),
            crossInsight = om.writeValueAsString(edition.content.crossInsights), // JSON 배열(CrossInsight)
            items = om.writeValueAsString(edition.content.allItems()),            // 평탄화(컬럼 NOT NULL 만족·참고용)
            refs = om.writeValueAsString(edition.content.references),
            status = edition.status.name.lowercase(),
        )
        return jpa.save(e).toDomain()
    }

    private fun EditionEntity.toDomain() = Edition(
        id = id,
        comboKey = comboKey,
        language = Language.valueOf(language.uppercase()),
        issueDate = issueDate,
        content = EditionContent(
            oneLine = oneLine,
            marketSummary = om.readValue(marketSummary),
            crossInsights = crossInsight?.let { om.readValue<List<CrossInsight>>(it) } ?: emptyList(),
            references = om.readValue(refs),
        ),
        status = EditionStatus.valueOf(status.uppercase()),
    )
}

@Component
class UserEditionStatePersistenceAdapter(
    private val jpa: UserEditionStateJpaRepository,
) : UserEditionStateRepository {
    override fun markRead(userId: Long, editionId: Long, atUtc: Instant) {
        val existing = jpa.findByUserIdAndEditionId(userId, editionId)
        if (existing == null) {
            jpa.save(UserEditionStateEntity(userId = userId, editionId = editionId, read = true, readAt = atUtc))
        } else if (!existing.read) {
            existing.read = true; existing.readAt = atUtc; jpa.save(existing)
        }
    }

    override fun findReadEditionIds(userId: Long, editionIds: List<Long>): Set<Long> {
        if (editionIds.isEmpty()) return emptySet()
        return jpa.findByUserIdAndEditionIdInAndReadIsTrue(userId, editionIds).map { it.editionId }.toSet()
    }

    override fun setBookmark(userId: Long, editionId: Long, bookmarked: Boolean, atUtc: Instant) {
        val existing = jpa.findByUserIdAndEditionId(userId, editionId)
        if (existing == null) {
            jpa.save(
                UserEditionStateEntity(
                    userId = userId, editionId = editionId,
                    bookmarked = bookmarked, bookmarkedAt = if (bookmarked) atUtc else null,
                ),
            )
        } else {
            existing.bookmarked = bookmarked
            existing.bookmarkedAt = if (bookmarked) atUtc else null
            jpa.save(existing)
        }
    }

    override fun isBookmarked(userId: Long, editionId: Long): Boolean =
        jpa.findByUserIdAndEditionId(userId, editionId)?.bookmarked ?: false

    override fun findBookmarkedEditionIds(userId: Long): List<Long> =
        jpa.findByUserIdAndBookmarkedIsTrueOrderByBookmarkedAtDesc(userId).map { it.editionId }
}

@Component
class DeliveryTargetQueryAdapter(
    private val users: UserJpaRepository,
    private val slots: SlotJpaRepository,
) : DeliveryTargetQuery {
    // 지연 컬렉션(categoryCodes)을 세션 안에서 읽도록 트랜잭션 경계 유지 (OSIV off, LazyInitialization 방지)
    @Transactional(readOnly = true)
    override fun findEligibleTargets(): List<DeliveryTarget> =
        users.findByPushPermission("granted").mapNotNull { u ->
            val userSlots = slots.findByUserIdAndDeletedAtIsNull(u.id!!) // 활성 슬롯만 발송
            if (userSlots.isEmpty()) return@mapNotNull null // 동의 게이트(슬롯 0 제외)
            DeliveryTarget(
                userId = u.id!!,
                language = Language.valueOf(u.outputLanguage.uppercase()),
                comboKeys = userSlots.map { ComboKey.of(it.categoryCodes) },
                timezone = u.timezone,
            )
        }
}
