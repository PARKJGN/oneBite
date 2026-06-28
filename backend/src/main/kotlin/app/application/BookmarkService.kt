package app.application

import app.domain.port.`in`.BookmarkUseCase
import app.domain.port.`in`.BookmarkView
import app.domain.port.out.EditionRepository
import app.domain.port.out.UserEditionStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class BookmarkService(
    private val editions: EditionRepository,
    private val states: UserEditionStateRepository,
) : BookmarkUseCase {

    @Transactional
    override fun setBookmark(userId: Long, editionId: Long, bookmarked: Boolean) {
        editions.findById(editionId) ?: throw IllegalArgumentException("에디션을 찾을 수 없습니다: $editionId")
        states.setBookmark(userId, editionId, bookmarked, Instant.now())
    }

    @Transactional(readOnly = true)
    override fun list(userId: Long): List<BookmarkView> =
        states.findBookmarkedEditionIds(userId).mapNotNull { id ->
            editions.findById(id)?.let { e ->
                BookmarkView(
                    editionId = e.id!!,
                    issueDate = e.issueDate,
                    oneLine = e.content.oneLine,
                    comboKey = e.comboKey,
                )
            }
        }
}
