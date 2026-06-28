-- Refresh 토큰(불투명) — 원문이 아닌 SHA-256 해시만 저장(유출 대비), 단일 사용·회전·폐기 가능.
-- user_id 느슨한 참조(FK 미설정) — 탈퇴/로그아웃 시 명시 삭제(원칙 VI).
CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
