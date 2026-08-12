package app.application

import app.domain.model.User
import app.application.port.`in`.SocialCodeLoginCommand
import app.application.port.`in`.SocialLoginCommand
import app.application.port.`in`.SocialLoginResult
import app.application.port.`in`.SocialLoginUseCase
import app.application.port.out.RefreshTokenStore
import app.application.port.out.SocialCodeExchanger
import app.application.port.out.SocialIdentity
import app.application.port.out.SocialIdentityVerifier
import app.application.port.out.TokenIssuer
import app.application.port.out.UserRepository
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
        private const val NICKNAME_MAX = 50   // users.nickname VARCHAR(50)
        private const val SUFFIX_TRIES = 50   // 번호 접미사 탐색 상한
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

    /**
     * 닉네임은 UNIQUE(V11) 라서 제공자 닉네임을 그대로 쓰면 두 번째 사용자가 제약 위반으로 튕긴다.
     * 특히 제공자가 별명을 안 주면 모두 DEFAULT_NICKNAME 으로 몰려 반드시 충돌한다.
     * 비어 있는 이름을 찾아 돌려준다(길이는 VARCHAR(50) 안에서 자른다).
     */
    private fun availableNickname(desired: String): String {
        val base = desired.ifBlank { DEFAULT_NICKNAME }.take(NICKNAME_MAX)
        if (!users.existsByNickname(base)) return base
        for (n in 2..SUFFIX_TRIES) {
            val suffix = n.toString()
            val candidate = base.take(NICKNAME_MAX - suffix.length) + suffix
            if (!users.existsByNickname(candidate)) return candidate
        }
        // 앞 번호가 전부 찼으면 임의 접미사로 폴백(사실상 충돌 없음)
        val rand = java.util.UUID.randomUUID().toString().take(8)
        return base.take(NICKNAME_MAX - rand.length - 1) + "_" + rand
    }

    // 제공자 식별 정보로 로그인/가입(upsert) 공통 처리
    private fun upsert(identity: SocialIdentity): SocialLoginResult {
        val existing = users.findByProvider(identity.provider, identity.providerId)
        if (existing != null) {
            // 기존 닉네임이 비었거나 기본값("사용자")이면 제공자 닉네임으로 보정.
            // (사용자가 직접 정한 닉네임은 덮어쓰지 않는다)
            // 단순 보정 때문에 개명(종건 -> 종건2)되면 이상하므로, 그 이름이 비어 있을 때만 바꾼다.
            val user = if ((existing.nickname.isBlank() || existing.nickname == DEFAULT_NICKNAME) &&
                identity.nickname.isNotBlank() && identity.nickname != DEFAULT_NICKNAME &&
                !users.existsByNickname(identity.nickname)
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
                nickname = availableNickname(identity.nickname),
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
