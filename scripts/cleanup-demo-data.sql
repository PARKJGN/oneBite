-- 데모/검증용 더미 계정 정리 (안전: 알려진 프리픽스로만 한정)
-- 실행: docker exec -i infra-postgres-1 psql -U onebite -d onebite < scripts/cleanup-demo-data.sql
-- editions(공유 Edition)는 사용자 FK가 없어 보존된다(탈퇴 시 비식별 보존 정책과 동일).
BEGIN;

-- FK CASCADE가 없는 자식 테이블은 명시 삭제(refresh_tokens, device_tokens)
DELETE FROM refresh_tokens
 WHERE user_id IN (SELECT id FROM users WHERE username ~ '^(diag_|live_|fbdemo_|e2e_|lockme_)');
DELETE FROM device_tokens
 WHERE user_id IN (SELECT id FROM users WHERE username ~ '^(diag_|live_|fbdemo_|e2e_|lockme_)');

-- users 삭제 → slots / user_edition_state / push_delivery 는 ON DELETE CASCADE 로 함께 정리
DELETE FROM users WHERE username ~ '^(diag_|live_|fbdemo_|e2e_|lockme_)';

COMMIT;
