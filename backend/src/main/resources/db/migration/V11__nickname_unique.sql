-- 닉네임 유니크 제약(아이디처럼 중복 불가). 회원가입 시 중복확인 + DB 레벨 보장.
-- 기존 데이터에 중복 닉네임이 있으면 UNIQUE 추가가 실패하므로, 먼저 중복분에 _id 접미사를 붙여 정리한다.
-- (가장 낮은 id 한 건은 원본 유지, 나머지 중복 행만 접미사.)
UPDATE users u
SET nickname = u.nickname || '_' || u.id
WHERE EXISTS (
    SELECT 1 FROM users u2
    WHERE u2.nickname = u.nickname AND u2.id < u.id
);

ALTER TABLE users ADD CONSTRAINT users_nickname_key UNIQUE (nickname);
