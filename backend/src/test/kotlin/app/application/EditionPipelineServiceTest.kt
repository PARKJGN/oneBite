package app.application

import app.domain.model.CrossInsight
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.EditionStatus
import app.domain.model.Language
import app.domain.model.RawArticle
import app.application.port.out.DeliveryTarget
import app.application.port.out.DeliveryTargetQuery
import app.application.port.out.EditionRepository
import app.application.port.out.EventPublisher
import app.application.port.out.RawArticleStore
import app.application.port.out.PushDeliveryRepository
import app.application.port.out.PushJob
import app.application.port.out.PushJobPublisher
import app.application.port.out.SummarizeInput
import app.application.port.out.SummarizerPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EditionPipelineServiceTest {

    private val date = LocalDate.parse("2026-06-23")

    private class FakeEditions : EditionRepository {
        val store = HashMap<String, Edition>()
        private fun key(c: String, l: Language, d: LocalDate) = "$c|$l|$d"
        override fun findByKey(comboKey: String, language: Language, issueDate: LocalDate) = store[key(comboKey, language, issueDate)]
        override fun findLatestBefore(comboKey: String, language: Language, issueDate: LocalDate): Edition? =
            store.values.filter { it.comboKey == comboKey && it.language == language && it.issueDate.isBefore(issueDate) }
                .maxByOrNull { it.issueDate }
        override fun findById(id: Long) = store.values.firstOrNull { it.id == id }
        override fun findByComboAndLanguage(comboKey: String, language: Language) =
            store.values.filter { it.comboKey == comboKey && it.language == language }.sortedByDescending { it.issueDate }
        override fun save(edition: Edition): Edition {
            val saved = edition.copy(id = (store.size + 1).toLong())
            store[key(saved.comboKey, saved.language, saved.issueDate)] = saved
            return saved
        }
    }

    private val targets = object : DeliveryTargetQuery {
        private val list = listOf(DeliveryTarget(userId = 1L, language = Language.KO, comboKeys = listOf("politics")))
        override fun findEligibleTargets() = list   // 발송 테스트용(동의 게이트)
        override fun findSubscribedTargets() = list  // 생성 테스트용(구독 기준) — 동일 대상
    }
    private val rawArticles = object : RawArticleStore {
        override fun saveNew(articles: List<RawArticle>) = 0
        override fun findWindow(categoryCode: String, sinceUtc: Instant, untilUtc: Instant) =
            listOf(RawArticle("t", "https://u", "연합", Language.KO, "politics", Instant.now()))
        override fun purgeOlderThan(cutoffUtc: Instant) = 0
    }
    private val summarizer = SummarizerPort { input: SummarizeInput ->
        EditionContent("핵심", listOf("요약"), listOf(CrossInsight("주요 소식", "본문", input.articles.map { EditionItem(it.title, it.source, it.url, it.categoryCode) })), listOf("연합"))
    }
    private val failingSummarizer = SummarizerPort { _: SummarizeInput -> throw RuntimeException("LLM 실패") }
    private val noEvents = EventPublisher { _, _ -> }

    private class FakePushDeliveries : PushDeliveryRepository {
        val recorded = mutableSetOf<Pair<Long, LocalDate>>()
        override fun existsForDate(userId: Long, issueDate: LocalDate) = (userId to issueDate) in recorded
        override fun record(userId: Long, issueDate: LocalDate, scheduledAtUtc: Instant) { recorded += (userId to issueDate) }
    }
    private fun editionAt(editions: FakeEditions, d: LocalDate = date) =
        editions.save(Edition(null, "politics", Language.KO, d, EditionContent("핵심", listOf("요약"), listOf(CrossInsight("주요 소식", "본문", listOf(EditionItem("t", "s", "u", "politics")))), listOf("s"))))

    @Test
    fun `신규 조합은 생성된다`() {
        val editions = FakeEditions()
        val svc = EditionGenerationService(targets, editions, rawArticles,summarizer, noEvents)
        val summary = svc.runForDate(date)
        assertEquals(1, summary.generated)
        assertEquals(0, summary.reused)
        assertEquals(1, editions.store.size)
    }

    @Test
    fun `이미 있는 조합은 재사용되어 재생성하지 않는다 (FR-015a)`() {
        val editions = FakeEditions()
        editions.save(Edition(null, "politics", Language.KO, date, EditionContent("핵심", listOf("요약"), listOf(CrossInsight("주요 소식", "본문", listOf(EditionItem("t", "s", "u", "politics")))), listOf("s"))))
        val svc = EditionGenerationService(targets, editions, rawArticles,summarizer, noEvents)
        val summary = svc.runForDate(date)
        assertEquals(0, summary.generated)
        assertEquals(1, summary.reused)
    }

    @Test
    fun `AI 실패 시 비-AI 헤드라인으로 폴백 생성한다 (직전분 없음, 원칙 X)`() {
        val editions = FakeEditions()
        val svc = EditionGenerationService(targets, editions, rawArticles,failingSummarizer, noEvents)
        val summary = svc.runForDate(date)
        assertEquals(0, summary.generated)
        assertEquals(1, summary.fallback)
        assertEquals(1, editions.store.size)
        assertEquals(EditionStatus.FALLBACK_NON_AI, editions.store.values.first().status)
    }

    @Test
    fun `AI 실패 시 직전 발송분이 있으면 그것으로 폴백한다 (원칙 X)`() {
        val editions = FakeEditions()
        editions.save(
            Edition(null, "politics", Language.KO, date.minusDays(1),
                EditionContent("어제 핵심", listOf("어제 요약"), listOf(CrossInsight("주요 소식", "본문", listOf(EditionItem("t", "s", "u", "politics")))), listOf("s"))),
        )
        val svc = EditionGenerationService(targets, editions, rawArticles,failingSummarizer, noEvents)
        val summary = svc.runForDate(date)
        assertEquals(1, summary.fallback)
        val today = editions.findByKey("politics", Language.KO, date)!!
        assertEquals(EditionStatus.FALLBACK_PREV, today.status)
        assertEquals("어제 핵심", today.content.oneLine) // 직전 콘텐츠 재사용
    }

    @Test
    fun `발송은 동의 대상에게 묶음 1푸시로 발행한다 (FR-009)`() {
        val editions = FakeEditions(); editionAt(editions)
        val published = mutableListOf<PushJob>()
        val svc = DispatchService(targets, editions, PushJobPublisher { published += it }, noEvents, FakePushDeliveries())

        val dispatched = svc.dispatchForDate(date)

        assertEquals(1, dispatched)
        assertEquals(1, published.size)
        assertEquals(1, published.first().editionIds.size)
        assertEquals(1L, published.first().userId)
    }

    @Test
    fun `보낼 에디션이 없으면 발송을 건너뛴다 (빈 푸시 금지)`() {
        val editions = FakeEditions() // 비어 있음
        val published = mutableListOf<PushJob>()
        val svc = DispatchService(targets, editions, PushJobPublisher { published += it }, noEvents, FakePushDeliveries())
        val dispatched = svc.dispatchForDate(date)
        assertEquals(0, dispatched)
        assertEquals(0, published.size)
    }

    @Test
    fun `타임존 로컬 08시 윈도에 들면 발송하고, 같은 날 재호출은 멱등 (FR-008, 원칙 XI)`() {
        val editions = FakeEditions(); editionAt(editions, date) // date = 2026-06-23
        val published = mutableListOf<PushJob>()
        val pushes = FakePushDeliveries()
        val svc = DispatchService(targets, editions, PushJobPublisher { published += it }, noEvents, pushes)

        // Asia/Seoul(UTC+9) 기준 2026-06-23 08:02 → UTC 2026-06-22T23:02
        val now = Instant.parse("2026-06-22T23:02:00Z")
        assertEquals(1, svc.dispatchDueAt(now))   // 발송
        assertEquals(0, svc.dispatchDueAt(now))   // 멱등(중복 발송 없음)
        assertEquals(1, published.size)
    }

    @Test
    fun `로컬 08시 윈도 밖이면 발송하지 않는다`() {
        val editions = FakeEditions(); editionAt(editions)
        val svc = DispatchService(targets, editions, PushJobPublisher { }, noEvents, FakePushDeliveries())
        // Asia/Seoul 10:00 → UTC 01:00 (윈도 밖)
        assertEquals(0, svc.dispatchDueAt(Instant.parse("2026-06-23T01:00:00Z")))
    }
}
