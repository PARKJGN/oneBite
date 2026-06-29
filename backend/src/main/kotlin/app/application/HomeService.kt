package app.application

import app.domain.UserNotFoundException
import app.domain.service.ComboKey
import app.application.port.`in`.HighlightItemView
import app.application.port.`in`.HomeUseCase
import app.application.port.`in`.YesterdayHighlightsView
import app.application.port.out.EditionRepository
import app.application.port.out.SlotRepository
import app.application.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ceil

@Service
class HomeService(
    private val users: UserRepository,
    private val slots: SlotRepository,
    private val editions: EditionRepository,
) : HomeUseCase {

    @Transactional(readOnly = true)
    override fun yesterdayHighlights(userId: Long, page: Int, size: Int): YesterdayHighlightsView {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        val pageSize = if (size <= 0) 5 else size           // 기본 5개/페이지(FR-021)
        val safePage = if (page < 0) 0 else page
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)

        // 사용자의 모든 슬롯(조합) 어제 에디션 항목을 합친다(중복 조합 제거).
        val combos = slots.findAllByUserId(userId).map { ComboKey.of(it.categoryCodes) }.distinct()
        val all: List<HighlightItemView> = combos.flatMap { combo ->
            val edition = editions.findByKey(combo, user.outputLanguage, yesterday) ?: return@flatMap emptyList()
            edition.content.items.map { HighlightItemView(it.title, it.source, it.categoryCode, edition.id!!, it.url) }
        }

        val totalItems = all.size
        val totalPages = if (totalItems == 0) 0 else ceil(totalItems.toDouble() / pageSize).toInt()
        val pageItems = all.drop(safePage * pageSize).take(pageSize)
        return YesterdayHighlightsView(
            page = safePage, size = pageSize, totalItems = totalItems, totalPages = totalPages, items = pageItems,
        )
    }
}
