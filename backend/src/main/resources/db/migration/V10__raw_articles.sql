-- 수집(ingest) 적재 테이블: 주기 수집이 RSS를 여기 쌓고, 8시 배치는 라이브 피드 대신 여기서 창을 자른다.
-- 이유: RSS 피드는 최근 N개만 노출 → 시간 지나면 창 구간 기사가 피드에서 사라짐(고정 창 재구성 불가).
-- 중복 차단: (category_code, url) UNIQUE → 매시간 재수집해도 같은 기사는 1행.
CREATE TABLE raw_articles (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_code VARCHAR(40)  NOT NULL,
    url           TEXT         NOT NULL,
    title         TEXT         NOT NULL,
    source        TEXT         NOT NULL,
    language      VARCHAR(2)   NOT NULL,
    published_at  TIMESTAMPTZ  NOT NULL,
    fetched_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_raw_article UNIQUE (category_code, url)
);
-- 창 조회용(카테고리 + 발행시각 범위)
CREATE INDEX idx_raw_articles_window ON raw_articles (category_code, published_at);
