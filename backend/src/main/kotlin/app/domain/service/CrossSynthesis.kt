package app.domain.service

/**
 * 교차 카테고리 종합(원칙 XV, FR-007a):
 * 전체 카테고리를 우선 엮되, 전체를 잇는 근거가 없으면 근거가 되는 가장 큰
 * 부분집합(4→3→2)으로 연결한다. 어떤 조합으로도 근거가 없으면 연결을 만들지 않는다
 * (억지 연결 금지 → null 반환 = 개별 요약만 제공).
 *
 * `hasEvidence`는 요약 어댑터(LLM)가 제공하는 "해당 카테고리 부분집합에 실제
 * 근거 있는 연결이 존재하는가" 판정 함수. 도메인은 부분집합 탐색 순서만 강제한다.
 */
object CrossSynthesis {
    fun largestGroundedSubset(
        categoryCodes: List<String>,
        hasEvidence: (Set<String>) -> Boolean,
    ): Set<String>? {
        val codes = categoryCodes.toSet()
        if (codes.size < 2) return null // 단일 카테고리는 교차 종합 대상 아님
        // 큰 부분집합부터(전체 → size-1 → ... → 2) 탐색, 처음 근거 있는 것을 채택
        for (size in codes.size downTo 2) {
            val grounded = combinations(codes.toList(), size).firstOrNull { hasEvidence(it) }
            if (grounded != null) return grounded
        }
        return null // 억지 연결 금지
    }

    private fun combinations(items: List<String>, k: Int): Sequence<Set<String>> = sequence {
        if (k == 0) { yield(emptySet()); return@sequence }
        if (k > items.size) return@sequence
        val head = items.first()
        val tail = items.drop(1)
        for (c in combinations(tail, k - 1)) yield(c + head)
        for (c in combinations(tail, k)) yield(c)
    }
}
