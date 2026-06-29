package app.adapter

import app.adapter.`in`.web.AuthController
import app.domain.InvalidCredentialsException
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Refresh 토큰: 로그인→회전(새 쌍, 옛 토큰 폐기)→재사용 거부→로그아웃 후 거부.
 */
class RefreshTokenIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController

    @Test
    fun `로그인 후 refresh 회전·재사용 거부·로그아웃`() {
        auth.signup(AuthController.SignupRequest("rita", "password123", "리타"))
        val r1 = auth.login(AuthController.LoginRequest("rita", "password123")).refreshToken

        // 회전: r1 → 새 쌍(r2), r1은 폐기
        val r2 = auth.refresh(AuthController.RefreshRequest(r1)).refreshToken
        assertNotEquals(r1, r2)

        // 옛 refresh(r1) 재사용 거부
        assertThrows(InvalidCredentialsException::class.java) {
            auth.refresh(AuthController.RefreshRequest(r1))
        }

        // 새 refresh(r2)는 동작 → r3
        val r3 = auth.refresh(AuthController.RefreshRequest(r2)).refreshToken

        // 로그아웃(r3 폐기) 후 거부
        auth.logout(AuthController.RefreshRequest(r3))
        assertThrows(InvalidCredentialsException::class.java) {
            auth.refresh(AuthController.RefreshRequest(r3))
        }
    }
}
