package app.domain

import app.domain.model.PushPermission
import app.domain.service.ConsentGate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 원칙 I / FR-010: 동의 게이트 불변식. */
class ConsentGateTest {

    @Test
    fun `슬롯이 없으면 권한이 있어도 발송 대상이 아니다`() {
        assertFalse(ConsentGate.isEligibleForPush(slotCount = 0, permission = PushPermission.GRANTED))
    }

    @Test
    fun `권한이 없으면 슬롯이 있어도 발송 대상이 아니다`() {
        assertFalse(ConsentGate.isEligibleForPush(slotCount = 2, permission = PushPermission.DENIED))
        assertFalse(ConsentGate.isEligibleForPush(slotCount = 2, permission = PushPermission.UNKNOWN))
    }

    @Test
    fun `슬롯이 있고 권한이 허용이면 발송 대상이다`() {
        assertTrue(ConsentGate.isEligibleForPush(slotCount = 1, permission = PushPermission.GRANTED))
    }
}
