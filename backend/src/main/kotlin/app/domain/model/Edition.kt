package app.domain.model

import app.domain.LowQualityContentException
import java.time.LocalDate

enum class EditionStatus { READY, FALLBACK_PREV, FALLBACK_NON_AI }

/** 에디션 내 항목(참고/요약 단위). */
data class EditionItem(
    val title: String,
    val source: String,
    val url: String,
    val categoryCode: String,
)

/**
 * 에디션 콘텐츠(II·XII): 한줄평(핵심 왜) + 시장요약(맥락·영향) + 항목 + 출처.
 * 교차 종합(XV)은 근거 있을 때만 채워진다(null 허용).
 */
data class EditionContent(
    val oneLine: String,
    val marketSummary: List<String>,
    val crossInsight: String?,
    val items: List<EditionItem>,
    val references: List<String>,
) {
    /**
     * 실행 가능한 인사이트 품질 불변식(II·XII, FR-005a):
     * 한줄평·시장요약 존재, 항목 1~5개·출처/링크 보유, 공허한 상투구(filler) 미포함.
     * 위반 시 예외 → 재생성/폴백 트리거(원칙 X). 폴백 콘텐츠는 검증 대상이 아니므로 생성자(init)가 아닌 명시 호출.
     */
    fun validate() {
        if (oneLine.isBlank()) throw LowQualityContentException("한줄평 누락")
        if (marketSummary.isEmpty() || marketSummary.all { it.isBlank() }) {
            throw LowQualityContentException("시장 요약 누락")
        }
        if (items.isEmpty() || items.size > MAX_ITEMS) {
            throw LowQualityContentException("항목 수(${items.size})가 1~$MAX_ITEMS 범위를 벗어남")
        }
        if (items.any { it.url.isBlank() || it.source.isBlank() }) {
            throw LowQualityContentException("출처/링크 누락 항목 존재")
        }
        val haystack = (listOf(oneLine, crossInsight.orEmpty()) + marketSummary).joinToString(" ")
        FILLER.firstOrNull { haystack.contains(it) }?.let {
            throw LowQualityContentException("공허한 상투구 포함: '$it'")
        }
    }

    fun isValid(): Boolean = runCatching { validate() }.isSuccess

    companion object {
        const val MAX_ITEMS = 20 // 슬롯당 항목 상한(랭킹 DEFAULT_LIMIT 과 일치시킬 것)
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
