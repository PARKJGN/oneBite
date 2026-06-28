-- oneBite 베이스라인 스키마 (T008). 시각은 UTC(timestamptz) 저장(원칙 XI).

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    recovery_email VARCHAR(255),                       -- 선택(FR-001b)
    timezone      VARCHAR(64)  NOT NULL DEFAULT 'Asia/Seoul',
    output_language VARCHAR(2)  NOT NULL DEFAULT 'ko',  -- ko|en (FR-002a)
    push_permission VARCHAR(10) NOT NULL DEFAULT 'unknown', -- granted|denied|unknown
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ                          -- 탈퇴(FR-018a)
);

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(40)  NOT NULL UNIQUE,
    name_ko    VARCHAR(60)  NOT NULL,
    name_en    VARCHAR(60)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT true,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE slots (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 슬롯의 카테고리(1~4개, FR-003). 자식 테이블로 정규화(JPA @ElementCollection 매핑).
CREATE TABLE slot_categories (
    slot_id       BIGINT      NOT NULL REFERENCES slots(id) ON DELETE CASCADE,
    category_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (slot_id, category_code)
);

CREATE TABLE editions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_key     VARCHAR(255) NOT NULL,                -- 정규화 카테고리 조합 해시
    language      VARCHAR(2)   NOT NULL,
    issue_date    DATE         NOT NULL,
    one_line      TEXT         NOT NULL,                -- 한줄평(핵심 "왜")
    market_summary TEXT        NOT NULL,                -- 맥락·영향 단락들(JSON 배열)
    cross_insight TEXT,                                 -- 교차 종합(근거 있을 때만)
    items         TEXT         NOT NULL,                -- 4~5개 항목(JSON)
    refs          TEXT         NOT NULL,                -- 출처 목록(JSON)
    status        VARCHAR(20)  NOT NULL DEFAULT 'ready',-- ready|fallback_prev|fallback_nonAI
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_edition UNIQUE (combo_key, language, issue_date)  -- FR-015a, SC-008
);

CREATE TABLE user_edition_state (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    edition_id    BIGINT      NOT NULL REFERENCES editions(id) ON DELETE CASCADE,
    read          BOOLEAN     NOT NULL DEFAULT false,   -- 데이터만, UI 비노출(FR-020)
    read_at       TIMESTAMPTZ,
    bookmarked    BOOLEAN     NOT NULL DEFAULT false,   -- 책갈피 영구(FR-011b)
    bookmarked_at TIMESTAMPTZ,
    CONSTRAINT uq_user_edition UNIQUE (user_id, edition_id)
);

CREATE TABLE articles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_code VARCHAR(40) NOT NULL,
    title       TEXT         NOT NULL,
    url         TEXT         NOT NULL,
    source      VARCHAR(120) NOT NULL,
    language    VARCHAR(2)   NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    fetched_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cluster_key VARCHAR(255),                           -- 중복 군집(FR-014)
    quality     VARCHAR(10)  NOT NULL DEFAULT 'valid',  -- valid|excluded
    exclusion_reason VARCHAR(120)
);

CREATE TABLE push_delivery (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issue_date  DATE         NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    dispatched_at TIMESTAMPTZ,
    status      VARCHAR(12)  NOT NULL DEFAULT 'queued', -- queued|dispatched|failed|skipped
    attempt_count INT        NOT NULL DEFAULT 0
);

CREATE TABLE metric_event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type        VARCHAR(30)  NOT NULL,                  -- generation|cache_hit|dispatch|failure|fallback|read
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    attributes  TEXT                                    -- 비식별 집계용 JSON(FR-017)
);

CREATE INDEX idx_slots_user ON slots(user_id);
CREATE INDEX idx_ues_user ON user_edition_state(user_id);
CREATE INDEX idx_articles_cat_pub ON articles(category_code, published_at);
CREATE INDEX idx_push_issue ON push_delivery(issue_date, status);
