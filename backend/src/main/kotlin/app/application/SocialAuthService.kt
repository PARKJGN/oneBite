package app.application

import app.domain.model.User
import app.domain.port.`in`.SocialCodeLoginCommand
import app.domain.port.`in`.SocialLoginCommand
import app.domain.port.`in`.SocialLoginResult
import app.domain.port.`in`.SocialLoginUseCase
import app.domain.port.out.RefreshTokenStore
import app.domain.port.out.SocialCodeExchanger
import app.domain.port.out.SocialIdentity
import app.domain.port.out.SocialIdentityVerifier
import app.domain.port.out.TokenIssuer
import app.domain.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialAuthService(
    private val users: UserRepository,
    private val verifier: SocialIdentityVerifier,
    private val tokens: TokenIssuer,
    private val refreshTokens: RefreshTokenStore,
    private val codeExchanger: SocialCodeExchanger,
) : SocialLoginUseCase {

    companion object {
        val SUPPORTED = setOf("google", "naver", "kakao")
        const val DEFAULT_NICKNAME = "사용자" // 제공자 닉네임 미수신 시 fallback(검증기와 동일)
    }

    @Transactional
    override fun login(cmd: SocialLoginCommand): SocialLoginResult {
        val provider = cmd.provider.lowercase()
        require(provider in SUPPORTED) { "지원하지 않는 소셜 제공자: ${cmd.provider}" }
        return upsert(verifier.verify(provider, cmd.accessToken))
    }

    @Transactional
    override fun loginWithCode(cmd: SocialCodeLoginCommand): SocialLoginResult {
        val provider = cmd.provider.lowercase()
        require(provider in SUPPORTED) { "지원하지 않는 소셜 제공자: ${cmd.provider}" }
        // 웹: 인가코드 → access token(서버에서 secret으로 교환) → 검증
        val accessToken = codeExchanger.exchange(provider, cmd.code, cmd.redirectUri, cmd.state)
        return upsert(verifier.verify(provider, accessToken))
    }

    // 제공자 식별 정보로 로그인/가입(upsert) 공통 처리
    private fun upsert(identity: SocialIdentity): SocialLoginResult {
        val existing = users.findByProvider(identity.provider, identity.providerId)
        if (existing != null) {
            // 기존 닉네임이 비었거나 기본값("사용자")이면 제공자 닉네임으로 보정.
            // (사용자가 직접 정한 닉네임은 덮어쓰지 않는다)
            val user = if ((existing.nickname.isBlank() || existing.nickname == DEFAULT_NICKNAME) &&
                identity.nickname.isNotBlank() && identity.nickname != DEFAULT_NICKNAME
            ) {
                users.save(existing.copy(nickname = identity.nickname))
            } else {
                existing
            }
            return SocialLoginResult(
                token = tokens.issue(user.id!!),
                refreshToken = refreshTokens.issue(user.id),
                userId = user.id, isNew = false,
            )
        }
        val created = users.save(
            User(
                id = null,
                username = null,        // 소셜 사용자는 아이디/비번 없음
                passwordHash = null,
                nickname = identity.nickname,
                provider = identity.provider,
                providerId = identity.providerId,
            ),
        )
        return SocialLoginResult(
            token = tokens.issue(created.id!!),
            refreshToken = refreshTokens.issue(created.id),
            userId = created.id, isNew = true,
        )
    }
}
