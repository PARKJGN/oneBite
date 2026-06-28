package app.domain.model

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
)

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
