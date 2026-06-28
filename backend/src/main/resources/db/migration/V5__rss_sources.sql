-- RSS 소스(FR-003a/002b): 운영자가 마이그레이션으로 카테고리별 피드를 관리한다.
-- URL은 예시이며 운영자가 검증·확장한다(가용성은 변동될 수 있음).
CREATE TABLE rss_sources (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_code VARCHAR(40)  NOT NULL,
    url           TEXT         NOT NULL,
    language      VARCHAR(2)   NOT NULL,     -- ko|en
    active        BOOLEAN      NOT NULL DEFAULT true
);
CREATE INDEX idx_rss_sources_cat ON rss_sources(category_code) WHERE active;

INSERT INTO rss_sources (category_code, url, language) VALUES
 ('economy',     'https://www.hankyung.com/feed/economy',          'ko'),
 ('politics',    'https://www.hankyung.com/feed/politics',         'ko'),
 ('realestate',  'https://www.hankyung.com/feed/realestate',       'ko'),
 ('tech',        'https://feeds.arstechnica.com/arstechnica/index','en'),
 ('science',     'https://www.sciencedaily.com/rss/all.xml',       'en'),
 ('world',       'http://feeds.bbci.co.uk/news/world/rss.xml',     'en'),
 ('health',      'https://www.medicalnewstoday.com/rss',           'en');
