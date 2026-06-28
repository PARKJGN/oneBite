-- 기기 푸시 토큰(FCM/APNs) 저장 — 발송 시 userId로 조회(원칙 I·V). 토큰은 기기당 1행(UNIQUE).
-- user_id는 느슨한 참조(FK 미설정)로 두어 계정 삭제 경로와 결합하지 않는다(정리 정책은 후속).
CREATE TABLE device_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      TEXT         NOT NULL UNIQUE,
    platform   VARCHAR(16)  NOT NULL DEFAULT 'android',  -- android | ios
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);
