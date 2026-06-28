package app.domain

/** 도메인 예외 — 어댑터(웹)에서 적절한 상태코드로 매핑. */
class UsernameAlreadyExistsException(username: String) :
    RuntimeException("이미 사용 중인 아이디입니다: $username")

class InvalidCredentialsException : RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다")

/** 연속 로그인 실패 임계 초과로 계정이 일시 잠김(무차별 대입 방어). HTTP 429. */
class TooManyLoginAttemptsException(val retryAfterSeconds: Long) :
    RuntimeException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.")

class UnknownCategoryException(codes: Collection<String>) :
    RuntimeException("알 수 없는 카테고리: $codes")

class UserNotFoundException(userId: Long) :
    RuntimeException("사용자를 찾을 수 없습니다: $userId")
