package app.application.port.out

import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.Language
import app.domain.model.RawArticle
import java.time.Instant
import java.time.LocalDate

/** RSS 수집(FR-003a) — 어댑터가 카테고리별 피드를 읽는다. */
fun interface FeedPort {
    /** [sinceUtc, untilUtc] 창 안에 발행된 기사만 수집(8시 다이제스트 = 직전 24h, 컷오프 07:30). */
    fun fetch(categoryCode: String, sinceUtc: Instant, untilUtc: Instant): List<RawArticle>
}

/** 카테고리별 RSS 소스 목록(데이터 관리, FR-002b/003a). */
fun interface RssSourceProvider {
    fun findByCategory(categoryCode: String): List<RssSource>
}
data class RssSource(val categoryCode: String, val url: String, val language: Language)

/**
 * 수집 적재 저장소: 주기 수집(매시간)이 RSS를 여기 쌓고, 8시 배치는 라이브 피드 대신 여기서 창을 자른다.
 * (RSS 는 최근 N개만 노출 → 시간 지나면 창 구간 기사가 사라짐. 적재해두면 고정 창을 안정적으로 재구성.)
 */
interface RawArticleStore {
    /** (category_code, url) 중복은 무시(ON CONFLICT). 신규로 저장된 건수 반환. */
    fun saveNew(articles: List<RawArticle>): Int
    /** [sinceUtc, untilUtc] 발행분 조회(생성 창). */
    fun findWindow(categoryCode: String, sinceUtc: Instant, untilUtc: Instant): List<RawArticle>
    /** 보관 정책: cutoff 이전 발행분 삭제. 삭제 건수 반환. */
    fun purgeOlderThan(cutoffUtc: Instant): Int
}

/** RSS 본문 다운로드(네트워크). 실패 시 null(품질 게이트에서 제외·로깅). */
fun interface FeedFetcher {
    fun fetch(url: String): String?
}

/** LLM 요약·번역(II·XV) — 랭킹된 군집을 받아 사용자 언어로 에디션 콘텐츠를 생성. */
fun interface SummarizerPort {
    fun summarize(input: SummarizeInput): EditionContent
}

data class SummarizeInput(
    val categoryCodes: List<String>,
    val language: Language,
    val articles: List<RawArticle>, // 군집 대표 기사들(랭킹 상위)
)

/** 에디션 저장·조회. (comboKey, language, issueDate) 단위 공유(FR-015a). */
interface EditionRepository {
    fun findByKey(comboKey: String, language: Language, issueDate: LocalDate): Edition?
    fun findLatestBefore(comboKey: String, language: Language, issueDate: LocalDate): Edition?
    fun findById(id: Long): Edition?
    fun save(edition: Edition): Edition
    fun findByComboAndLanguage(comboKey: String, language: Language): List<Edition> // 라이브러리(최신순)
}

/** 읽음/책갈피 상태(FR-020/011b). */
interface UserEditionStateRepository {
    fun markRead(userId: Long, editionId: Long, atUtc: Instant)
    fun findReadEditionIds(userId: Long, editionIds: List<Long>): Set<Long>
    fun setBookmark(userId: Long, editionId: Long, bookmarked: Boolean, atUtc: Instant)
    fun isBookmarked(userId: Long, editionId: Long): Boolean
    fun findBookmarkedEditionIds(userId: Long): List<Long> // 최신 책갈피 순(영구 보존)
}

/** 발송/생성 대상 조회. 발송은 동의 게이트(권한 granted), 생성은 구독(활성 슬롯) 기준. */
interface DeliveryTargetQuery {
    /** 발송(푸시) 대상 — 동의 게이트(슬롯≥1 AND 권한 granted). DispatchService 용(원칙 I, FR-010). */
    fun findEligibleTargets(): List<DeliveryTarget>

    /** 생성 대상 — 활성 슬롯≥1 인 모든 구독자(푸시 동의 무관). 인앱 열람 위해 생성은 구독 기준(원칙 IV). */
    fun findSubscribedTargets(): List<DeliveryTarget>
}

data class DeliveryTarget(
    val userId: Long,
    val language: Language,
    val comboKeys: List<String>, // 사용자의 슬롯들(각 슬롯 카테고리 조합의 정규화 키)
    val timezone: String = "Asia/Seoul", // 사용자 타임존(원칙 XI) — 로컬 08:00 발송 판정
)

/** 발송 기록·멱등(원칙 V): 같은 사용자·발송일자에 중복 발송 방지. */
interface PushDeliveryRepository {
    fun existsForDate(userId: Long, issueDate: LocalDate): Boolean
    fun record(userId: Long, issueDate: LocalDate, scheduledAtUtc: Instant)
}

/** 푸시 팬아웃(원칙 X) — Kafka 등으로 발송 작업을 발행. */
fun interface PushJobPublisher {
    fun publish(job: PushJob)
}

/** 실제 푸시 전송(APNs/FCM). consumer가 호출. 실패 시 false → 재시도(원칙 X). */
fun interface PushSender {
    fun send(job: PushJob): Boolean
}

data class PushJob(
    val userId: Long,
    val issueDate: LocalDate,
    val editionIds: List<Long>, // 묶음 1푸시(FR-009)
)

/** 관측 이벤트(FR-017, XIV) — 비식별 집계. */
fun interface EventPublisher {
    fun emit(type: String, attributes: Map<String, Any?>)
}
