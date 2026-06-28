-- 소셜 로그인 병행(FR-001c). 소셜 사용자는 아이디/비밀번호가 없으므로 nullable로 완화하고
-- (provider, provider_id) 유일 제약을 둔다. 저장 정보는 provider·provider_id·nickname으로 최소화.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
ALTER TABLE users ADD COLUMN provider     VARCHAR(20);   -- google|naver|kakao (null이면 아이디/비번 계정)
ALTER TABLE users ADD COLUMN provider_id  VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_user_social UNIQUE (provider, provider_id);
