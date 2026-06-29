package app.application

import app.application.port.`in`.RegisterDeviceTokenUseCase
import app.application.port.out.DeviceTokenRepository
import org.springframework.stereotype.Service

@Service
class DeviceService(private val tokens: DeviceTokenRepository) : RegisterDeviceTokenUseCase {
    override fun register(userId: Long, token: String, platform: String) {
        require(token.isNotBlank()) { "token이 비어 있습니다" }
        val plat = platform.lowercase().takeIf { it in setOf("android", "ios") } ?: "android"
        tokens.register(userId, token, plat)
    }
}
