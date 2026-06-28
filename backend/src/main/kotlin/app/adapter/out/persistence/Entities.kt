package app.adapter.out.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(unique = true)
    var username: String? = null,        // 소셜 사용자는 null
    var passwordHash: String? = null,    // 소셜 사용자는 null
    var nickname: String = "",
    var recoveryEmail: String? = null,
    var timezone: String = "Asia/Seoul",
    var outputLanguage: String = "ko",
    var pushPermission: String = "unknown",
    var provider: String? = null,        // google|naver|kakao
    var providerId: String? = null,
)

@Entity
@Table(name = "slots")
class SlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    @ElementCollection
    @CollectionTable(name = "slot_categories", joinColumns = [JoinColumn(name = "slot_id")])
    @Column(name = "category_code")
    var categoryCodes: MutableList<String> = mutableListOf(),
    var deletedAt: java.time.Instant? = null, // null이면 활성
    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: java.time.Instant? = null, // DB default now() — 구독 시작일
)

@Entity
@Table(name = "categories")
class CategoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var code: String = "",
    var nameKo: String = "",
    var nameEn: String = "",
    var active: Boolean = true,
    var sortOrder: Int = 0,
)
