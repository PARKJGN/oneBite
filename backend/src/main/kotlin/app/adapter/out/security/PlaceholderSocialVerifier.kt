package app.adapter.out.security

import app.application.port.out.SocialCodeExchanger
import app.application.port.out.SocialIdentity
import app.application.port.out.SocialIdentityVerifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * dev/test용 소셜 검증(기본) — accessToken을 "providerId" 또는 "providerId:nickname"으로 해석.
 * 실제 검증은 onebite.social=http 로 전환하면 HttpSocialIdentityVerifier가 대신 활성화된다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.social"], havingValue = "placeholder", matchIfMissing = true)
class PlaceholderSocialVerifier : SocialIdentityVerifier {
    override fun verify(provider: String, accessToken: String): SocialIdentity {
        require(accessToken.isNotBlank()) { "accessToken이 비어 있습니다" }
        val parts = accessToken.split(":", limit = 2)
        val providerId = parts[0].trim()
        require(providerId.isNotBlank()) { "providerId를 확인할 수 없습니다" }
        val nickname = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: "사용자"
        return SocialIdentity(provider = provider, providerId = providerId, nickname = nickname)
    }
}

/**
 * dev/test용 code 교환(기본) — 실제 교환 없이 dev access token을 반환(PlaceholderSocialVerifier가 해석).
 * 실제 교환은 onebite.social=http 의 HttpSocialCodeExchanger가 담당.
 */
@Component
@ConditionalOnProperty(name = ["onebite.social"], havingValue = "placeholder", matchIfMissing = true)
class PlaceholderSocialCodeExchanger : SocialCodeExchanger {
    override fun exchange(provider: String, code: String, redirectUri: String, state: String?): String =
        "dev-$provider-uid:테스터"
}
