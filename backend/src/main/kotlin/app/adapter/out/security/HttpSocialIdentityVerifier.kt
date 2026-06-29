package app.adapter.out.security

import app.application.port.out.SocialIdentity
import app.application.port.out.SocialIdentityVerifier
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 실제 소셜 토큰 검증(FR-001c). 클라이언트가 제공자 SDK로 받은 access token으로 각 제공자
 * UserInfo 엔드포인트를 호출해 providerId/nickname을 얻는다. onebite.social=http 일 때 활성화.
 * 우리 앱 시크릿은 불필요(사용자 access token으로 조회). 저장 정보는 최소화(원칙 VI).
 */
@Component
@ConditionalOnProperty(name = ["onebite.social"], havingValue = "http")
class HttpSocialIdentityVerifier(private val om: ObjectMapper) : SocialIdentityVerifier {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun verify(provider: String, accessToken: String): SocialIdentity {
        require(accessToken.isNotBlank()) { "accessToken이 비어 있습니다" }
        val identity = when (provider) {
            "google" -> get("https://www.googleapis.com/oauth2/v3/userinfo", accessToken).let {
                SocialIdentity("google", it.path("sub").asText(), it.path("name").asText("").ifBlank { "사용자" })
            }
            "kakao" -> get("https://kapi.kakao.com/v2/user/me", accessToken).let {
                val nick = it.path("kakao_account").path("profile").path("nickname").asText("")
                    .ifBlank { it.path("properties").path("nickname").asText("") }
                SocialIdentity("kakao", it.path("id").asText(), nick.ifBlank { "사용자" })
            }
            "naver" -> get("https://openapi.naver.com/v1/nid/me", accessToken).path("response").let {
                val nick = it.path("nickname").asText("").ifBlank { it.path("name").asText("") }
                SocialIdentity("naver", it.path("id").asText(), nick.ifBlank { "사용자" })
            }
            else -> throw IllegalArgumentException("지원하지 않는 소셜 제공자: $provider")
        }
        require(identity.providerId.isNotBlank()) { "providerId를 확인할 수 없습니다(토큰/스코프 확인)" }
        return identity
    }

    private fun get(url: String, token: String): JsonNode {
        val req = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()
        val res = client.send(req, HttpResponse.BodyHandlers.ofString())
        require(res.statusCode() in 200..299) { "소셜 토큰 검증 실패(${res.statusCode()}): ${res.body()}" }
        return om.readTree(res.body())
    }
}
