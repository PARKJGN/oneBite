package app.application

import app.domain.InvalidCredentialsException
import app.domain.UsernameAlreadyExistsException
import app.domain.model.User
import app.application.port.`in`.AuthUseCase
import app.application.port.`in`.LoginCommand
import app.application.port.`in`.LoginResult
import app.application.port.`in`.SignupCommand
import app.application.port.`in`.SignupResult
import app.application.port.`in`.TokenPair
import app.application.port.out.LoginAttemptGuard
import app.application.port.out.PasswordHasher
import app.application.port.out.RefreshTokenStore
import app.application.port.out.TokenIssuer
import app.application.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val tokens: TokenIssuer,
    private val refreshTokens: RefreshTokenStore,
    private val loginGuard: LoginAttemptGuard,
) : AuthUseCase {

    companion object {
        val USERNAME_LENGTH = 4..20 // 아이디 길이 제약
    }

    @Transactional(readOnly = true)
    override fun isUsernameAvailable(username: String): Boolean =
        username.length in USERNAME_LENGTH && !users.existsByUsername(username)

    @Transactional
    override fun signup(cmd: SignupCommand): SignupResult {
        require(cmd.username.length in USERNAME_LENGTH) { "아이디는 ${USERNAME_LENGTH.first}~${USERNAME_LENGTH.last}자여야 합니다" }
        require(cmd.password.length >= 8) { "비밀번호는 8자 이상이어야 합니다" }
        require(cmd.nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다" }
        if (users.existsByUsername(cmd.username)) throw UsernameAlreadyExistsException(cmd.username)

        val saved = users.save(
            User(
                id = null,
                username = cmd.username,
                passwordHash = hasher.hash(cmd.password),
                nickname = cmd.nickname,
                recoveryEmail = cmd.recoveryEmail?.takeIf { it.isNotBlank() },
            ),
        )
        return SignupResult(
            userId = saved.id!!,
            nickname = saved.nickname,
            token = tokens.issue(saved.id),
            refreshToken = refreshTokens.issue(saved.id), // 가입 즉시 자동 로그인
        )
    }

    @Transactional // refresh 토큰 발급(INSERT)이 포함되어 읽기-쓰기
    override fun login(cmd: LoginCommand): LoginResult {
        loginGuard.assertNotLocked(cmd.username) // 잠겨 있으면 429(무차별 대입 방어)
        val user = users.findByUsername(cmd.username) ?: run { loginGuard.recordFailure(cmd.username); throw InvalidCredentialsException() }
        val hash = user.passwordHash ?: run { loginGuard.recordFailure(cmd.username); throw InvalidCredentialsException() } // 소셜 전용 계정
        if (!hasher.matches(cmd.password, hash)) {
            loginGuard.recordFailure(cmd.username)
            throw InvalidCredentialsException()
        }
        loginGuard.recordSuccess(cmd.username) // 성공 시 실패 카운트 초기화
        return LoginResult(
            token = tokens.issue(user.id!!),
            refreshToken = refreshTokens.issue(user.id),
            userId = user.id,
        )
    }

    @Transactional
    override fun refresh(refreshToken: String): TokenPair {
        val userId = refreshTokens.consume(refreshToken) ?: throw InvalidCredentialsException() // 회전: 옛 토큰 폐기
        return TokenPair(token = tokens.issue(userId), refreshToken = refreshTokens.issue(userId))
    }

    @Transactional
    override fun logout(refreshToken: String) {
        refreshTokens.consume(refreshToken) // 제시한 refresh 토큰 폐기(반환값 무시)
    }
}
