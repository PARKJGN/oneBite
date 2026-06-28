package app.domain.service

/**
 * Edition 공유 키(FR-015a, SC-008): 카테고리 조합은 순서 무관.
 * 정규화(정렬·중복 제거)하여 같은 조합이 항상 같은 키가 되도록 한다.
 * 최종 Edition 식별은 (comboKey, language, issueDate) 조합.
 */
object ComboKey {
    fun of(categoryCodes: Collection<String>): String =
        categoryCodes.map { it.trim().lowercase() }
            .toSortedSet()
            .joinToString("+")
}
