package app.application

import app.domain.port.`in`.RegisterDeviceTokenUseCase
import app.domain.port.out.DeviceTokenRepository
import org.springframework.stereotype.Service

@Service
class DeviceService(private val tokens: DeviceTokenRepository) : RegisterDeviceTokenUseCase {
    override fun register(userId: Long, token: String, platform: String) {
        require(token.isNotBlank()) { "token이 비어 있습니다" }
        val plat = platform.lowercase().takeIf { it in setOf("android", "ios") } ?: "android"
        tokens.register(userId, token, plat)
    }
}
