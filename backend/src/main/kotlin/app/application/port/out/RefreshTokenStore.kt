package app.application.port.out

/**
 * Refresh 토큰 저장(불투명·해시 저장, FR-001). access(JWT) 만료 시 재로그인 없이 갱신.
 * 단일 사용(회전) + 폐기 가능(로그아웃·탈퇴).
 */
interface RefreshTokenStore {
    /** 새 refresh 토큰 발급(원문 반환, 해시 저장). */
    fun issue(userId: Long): String

    /** 검증 + 회전(소비·삭제): 유효하면 userId 반환, 무효/만료면 null. */
    fun consume(rawToken: String): Long?

    /** 사용자의 모든 refresh 토큰 폐기(탈퇴/전체 로그아웃). */
    fun revokeAll(userId: Long)
}
