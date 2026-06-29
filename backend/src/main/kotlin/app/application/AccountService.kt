package app.application

import app.application.port.out.DeviceTokenRepository
import app.application.port.out.RefreshTokenStore
import app.application.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 계정 삭제(탈퇴, FR-018a): 개인 식별 정보·슬롯·읽음/책갈피·발송기록 제거(FK cascade) +
 * 기기/Refresh 토큰 명시 삭제(FK 미연결, 원칙 VI). 공유 Edition(요약 본문)은 개인정보가 아니라 보존.
 */
@Service
class AccountService(
    private val users: UserRepository,
    private val deviceTokens: DeviceTokenRepository,
    private val refreshTokens: RefreshTokenStore,
) {
    @Transactional
    fun delete(userId: Long) {
        deviceTokens.deleteByUserId(userId) // FK 미연결 → 명시 삭제
        refreshTokens.revokeAll(userId)     // 탈퇴 시 모든 refresh 토큰 폐기
        users.delete(userId)
    }
}
