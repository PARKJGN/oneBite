package app.domain.service

import app.domain.model.PushPermission

/**
 * 동의 기반 전달(원칙 I, FR-010): 슬롯이 하나도 없거나 푸시 권한이 없으면
 * 발송 대상에서 제외한다. 이 게이트는 '푸시 발송'에만 적용된다(인앱 열람은 무관).
 */
object ConsentGate {
    fun isEligibleForPush(slotCount: Int, permission: PushPermission): Boolean =
        slotCount > 0 && permission == PushPermission.GRANTED
}
