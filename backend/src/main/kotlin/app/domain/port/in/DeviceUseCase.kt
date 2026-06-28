package app.domain.port.`in`

/** 기기 푸시 토큰 등록(US4/원칙 III·I) — 클라이언트가 발급받은 FCM/APNs 토큰을 서버에 등록. */
interface RegisterDeviceTokenUseCase {
    fun register(userId: Long, token: String, platform: String)
}
