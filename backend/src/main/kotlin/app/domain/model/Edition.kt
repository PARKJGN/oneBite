package app.domain.model

import app.domain.LowQualityContentException
import java.time.LocalDate

enum class EditionStatus { READY, FALLBACK_PREV, FALLBACK_NON_AI }

/** 에디션 내 항목(참고 기사 단위). */
data class EditionItem(
    val title: String,
    val source: String,
    val url: String,
    val categoryCode: String,
)

/**
 * 복합정보(교차 종합, 원칙 XV): 카테고리 간 근거 있는 연결 1건.
 * items = 이 복합정보에 실제로 사용된 기사들(= 관련 뉴스). 묶이지 않은 기사는 에디션에 싣지 않는다.
 */
data class CrossInsight(
    val headline: String,
    val body: String,
    val items: List<EditionItem>,
)

/**
 * 에디션 콘텐츠(II·XII·XV): 한줄평 + 전체 시장요약 + 복합정보 0~3개(각자 관련 기사 보유).
 */
data class EditionContent(
    val oneLine: String,
    val marketSummary: List<String>,
    val crossInsights: List<CrossInsight>,
    val references: List<String>,
) {
    /** 모든 복합정보가 사용한 기사(평탄화). */
    fun allItems(): List<EditionItem> = crossInsights.flatMap { it.items }

    /**
     * 품질 불변식(II·XII·XV, FR-005a): 한줄평·시장요약 존재, 복합정보 0~3개·각자 관련기사(출처/링크) 보유,
     * 총 항목 상한 이내, 공허한 상투구(filler) 미포함. 위반 시 예외 → 재생성/폴백(원칙 X).
     */
    fun validate() {
        if (oneLine.isBlank()) throw LowQualityContentException("한줄평 누락")
        if (marketSummary.isEmpty() || marketSummary.all { it.isBlank() }) {
            throw LowQualityContentException("시장 요약 누락")
        }
        if (crossInsights.size > MAX_INSIGHTS) {
            throw LowQualityContentException("복합정보 수(${crossInsights.size})가 상한 $MAX_INSIGHTS 초과")
        }
        crossInsights.forEach { ci ->
            if (ci.headline.isBlank() || ci.body.isBlank()) throw LowQualityContentException("복합정보 제목/본문 누락")
            if (ci.items.isEmpty()) throw LowQualityContentException("복합정보 '${ci.headline}'에 관련 기사 없음")
            if (ci.items.any { it.url.isBlank() || it.source.isBlank() }) {
                throw LowQualityContentException("출처/링크 누락 항목 존재")
            }
        }
        val totalItems = crossInsights.sumOf { it.items.size }
        if (totalItems > MAX_ITEMS) throw LowQualityContentException("총 항목 수($totalItems)가 상한 $MAX_ITEMS 초과")

        val haystack = (listOf(oneLine) + marketSummary + crossInsights.flatMap { listOf(it.headline, it.body) })
            .joinToString(" ")
        FILLER.firstOrNull { haystack.contains(it) }?.let {
            throw LowQualityContentException("공허한 상투구 포함: '$it'")
        }
    }

    fun isValid(): Boolean = runCatching { validate() }.isSuccess

    companion object {
        const val MAX_INSIGHTS = 3 // 복합정보 상한
        const val MAX_ITEMS = 20   // 총 관련 기사 상한(랭킹 DEFAULT_LIMIT 과 일치)
        private val FILLER = listOf(
            "큰 영향을 미칠 것으로 예상", "귀추가 주목", "중요한 의미를 갖", "관심이 모아지고 있",
        )
    }
}

/**
 * 에디션 — (comboKey, language, issueDate) 단위로 식별·공유(FR-015a).
 */
data class Edition(
    val id: Long?,
    val comboKey: String,
    val language: Language,
    val issueDate: LocalDate,
    val content: EditionContent,
    val status: EditionStatus = EditionStatus.READY,
)
