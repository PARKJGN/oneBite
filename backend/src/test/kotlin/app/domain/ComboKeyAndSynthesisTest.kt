package app.domain

import app.domain.service.ComboKey
import app.domain.service.CrossSynthesis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComboKeyAndSynthesisTest {

    /** FR-015a: 카테고리 조합은 순서 무관 → 같은 키. */
    @Test
    fun `combo key는 순서와 무관하게 동일하다`() {
        assertEquals(
            ComboKey.of(listOf("politics", "realestate")),
            ComboKey.of(listOf("realestate", "POLITICS")),
        )
    }

    /** FR-007a: 전체 근거 없으면 가장 큰 부분집합. */
    @Test
    fun `전체 근거가 없으면 근거 있는 3개 부분집합을 택한다`() {
        val cats = listOf("politics", "economy", "realestate", "tech")
        val grounded = setOf("politics", "economy", "realestate")
        val result = CrossSynthesis.largestGroundedSubset(cats) { it == grounded }
        assertEquals(grounded, result)
    }

    /** FR-007a: 어떤 조합도 근거 없으면 연결 없음(억지 연결 금지). */
    @Test
    fun `근거가 전혀 없으면 null을 반환한다`() {
        val cats = listOf("politics", "sports", "health")
        assertNull(CrossSynthesis.largestGroundedSubset(cats) { false })
    }

    /** 단일 카테고리는 교차 종합 대상이 아니다. */
    @Test
    fun `단일 카테고리는 null`() {
        assertNull(CrossSynthesis.largestGroundedSubset(listOf("politics")) { true })
    }

    @Test
    fun `전체 근거가 있으면 전체를 택한다`() {
        val cats = listOf("politics", "economy")
        val result = CrossSynthesis.largestGroundedSubset(cats) { true }
        assertTrue(result == setOf("politics", "economy"))
    }
}
