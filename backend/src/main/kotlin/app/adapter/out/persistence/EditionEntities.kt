package app.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "editions")
class EditionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var comboKey: String = "",
    var language: String = "",
    var issueDate: LocalDate = LocalDate.EPOCH,
    @Column(columnDefinition = "text") var oneLine: String = "",
    @Column(columnDefinition = "text") var marketSummary: String = "[]", // JSON 배열
    @Column(columnDefinition = "text") var crossInsight: String? = null,
    @Column(columnDefinition = "text") var items: String = "[]",         // JSON
    @Column(columnDefinition = "text") var refs: String = "[]",          // JSON
    var status: String = "ready",
)

@Entity
@Table(name = "user_edition_state")
class UserEditionStateEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    var editionId: Long = 0,
    var read: Boolean = false,
    var readAt: Instant? = null,
    var bookmarked: Boolean = false,
    var bookmarkedAt: Instant? = null,
)
