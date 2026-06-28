-- 슬롯 소프트 삭제(FR-003b): 삭제해도 라이브러리(히스토리)에 유지하기 위해 물리 삭제 대신
-- deleted_at로 표시한다. 발송 대상 쿼리는 deleted_at IS NULL(활성)만, 라이브러리는 전체.
ALTER TABLE slots ADD COLUMN deleted_at TIMESTAMPTZ;
CREATE INDEX idx_slots_user_active ON slots(user_id) WHERE deleted_at IS NULL;
