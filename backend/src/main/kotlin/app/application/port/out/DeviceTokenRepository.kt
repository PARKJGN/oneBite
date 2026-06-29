package app.application.port.out

/** 기기 푸시 토큰 저장·조회(원칙 I·V). 발송 시 userId로 활성 토큰을 찾아 FCM/APNs로 전송. */
interface DeviceTokenRepository {
    /** 기기 토큰 등록(멱등 — 같은 토큰이면 소유 사용자·플랫폼 갱신). */
    fun register(userId: Long, token: String, platform: String)

    /** 해당 사용자의 등록 토큰 목록. */
    fun findActiveTokens(userId: Long): List<String>

    /** 사용자의 모든 기기 토큰 삭제(계정 탈퇴 시, 원칙 VI). */
    fun deleteByUserId(userId: Long)
}
