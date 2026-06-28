package app.adapter.out.security

import app.domain.port.out.SocialCodeExchanger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * 웹 OAuth code → access token 교환(onebite.social=http). 각 제공자 토큰 엔드포인트에
 * client_id/client_secret(서버 보관) + code + redirect_uri 로 교환한다.
 * 얻은 access token은 HttpSocialIdentityVerifier가 다시 UserInfo로 검증한다.
 */
@Component
@ConditionalOnProperty(name = ["onebite.social"], havingValue = "http")
class HttpSocialCodeExchanger(
    private val om: ObjectMapper,
    @Value("\${onebite.oauth.kakao.client-id:}") private val kakaoClientId: String,
    @Value("\${onebite.oauth.kakao.client-secret:}") private val kakaoClientSecret: String,
    @Value("\${onebite.oauth.naver.client-id:}") private val naverClientId: String,
    @Value("\${onebite.oauth.naver.client-secret:}") private val naverClientSecret: String,
    @Value("\${onebite.oauth.google.client-id:}") private val googleClientId: String,
    @Value("\${onebite.oauth.google.client-secret:}") private val googleClientSecret: String,
) : SocialCodeExchanger {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun exchange(provider: String, code: String, redirectUri: String, state: String?): String {
        require(code.isNotBlank()) { "code가 비어 있습니다" }
        val form = LinkedHashMap<String, String>()
        form["grant_type"] = "authorization_code"
        form["code"] = code
        val tokenUrl = when (provider) {
            "kakao" -> {
                form["client_id"] = kakaoClientId
                form["redirect_uri"] = redirectUri
                if (kakaoClientSecret.isNotBlank()) form["client_secret"] = kakaoClientSecret
                "https://kauth.kakao.com/oauth/token"
            }
            "naver" -> {
                form["client_id"] = naverClientId
                form["client_secret"] = naverClientSecret
                if (!state.isNullOrBlank()) form["state"] = state
                "https://nid.naver.com/oauth2.0/token"
            }
            "google" -> {
                form["client_id"] = googleClientId
                form["client_secret"] = googleClientSecret
                form["redirect_uri"] = redirectUri
                "https://oauth2.googleapis.com/token"
            }
            else -> throw IllegalArgumentException("지원하지 않는 소셜 제공자: $provider")
        }

        val body = form.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        val req = HttpRequest.newBuilder(URI(tokenUrl))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val res = client.send(req, HttpResponse.BodyHandlers.ofString())
        require(res.statusCode() in 200..299) { "토큰 교환 실패(${res.statusCode()}): ${res.body()}" }
        val accessToken = om.readTree(res.body()).path("access_token").asText("")
        require(accessToken.isNotBlank()) { "access_token을 받지 못했습니다: ${res.body()}" }
        return accessToken
    }

    private fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)
}
