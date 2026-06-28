package app.adapter.out.persistence

import app.domain.model.Language
import app.domain.port.out.RssSource
import app.domain.port.out.RssSourceProvider
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Entity
@Table(name = "rss_sources")
class RssSourceEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var categoryCode: String = "",
    var url: String = "",
    var language: String = "ko",
    var active: Boolean = true,
)

interface RssSourceJpaRepository : JpaRepository<RssSourceEntity, Long> {
    fun findByCategoryCodeAndActiveIsTrue(categoryCode: String): List<RssSourceEntity>
}

@Component
class RssSourceAdapter(private val jpa: RssSourceJpaRepository) : RssSourceProvider {
    override fun findByCategory(categoryCode: String): List<RssSource> =
        jpa.findByCategoryCodeAndActiveIsTrue(categoryCode)
            .map { RssSource(it.categoryCode, it.url, Language.valueOf(it.language.uppercase())) }
}
