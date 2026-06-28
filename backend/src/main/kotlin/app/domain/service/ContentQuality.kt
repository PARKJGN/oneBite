package app.domain.service

import app.domain.model.EditionContent

class LowQualityContentException(reason: String) :
    RuntimeException("요약 품질 기준 미달: $reason")

/**
 * 실행 가능한 인사이트 품질 검증(II·XII, FR-005a):
 * 3요소(한줄평·시장요약) 존재, 항목 1~5개, 공허한 상투구(filler) 미포함.
 * 통과하지 못하면 예외 → 재생성/폴백 트리거(원칙 X).
 */
object ContentQuality {
    private val FILLER = listOf(
        "큰 영향을 미칠 것으로 예상", "귀추가 주목", "중요한 의미를 갖", "관심이 모아지고 있",
    )

    fun validate(content: EditionContent) {
        if (content.oneLine.isBlank()) throw LowQualityContentException("한줄평 누락")
        if (content.marketSummary.isEmpty() || content.marketSummary.all { it.isBlank() }) {
            throw LowQualityContentException("시장 요약 누락")
        }
        if (content.items.isEmpty() || content.items.size > Ranking.DEFAULT_LIMIT) {
            throw LowQualityContentException("항목 수(${content.items.size})가 1~${Ranking.DEFAULT_LIMIT} 범위를 벗어남")
        }
        if (content.items.any { it.url.isBlank() || it.source.isBlank() }) {
            throw LowQualityContentException("출처/링크 누락 항목 존재")
        }
        val haystack = (listOf(content.oneLine, content.crossInsight.orEmpty()) + content.marketSummary).joinToString(" ")
        FILLER.firstOrNull { haystack.contains(it) }?.let {
            throw LowQualityContentException("공허한 상투구 포함: '$it'")
        }
    }

    fun isValid(content: EditionContent): Boolean =
        runCatching { validate(content) }.isSuccess
}
