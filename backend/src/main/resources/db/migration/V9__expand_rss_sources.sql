-- RSS 소스 균형화(FR-002b/003a): 전 카테고리가 최소 2개 소스를 갖도록 확장.
-- 기존(V5+V6): 각 카테고리 1개 + culture/sports/environment 누락 → 한 소스가 비면 통째로 빵꾸.
-- 모든 URL 은 2026-06-29 fetch 로 live(200 + item) 검증함. 국내 주제=한국어, 글로벌 주제=영어(GLM 번역).
INSERT INTO rss_sources (category_code, url, language) VALUES
 -- 기존 1개 카테고리에 2번째 소스 추가 --
 ('politics',    'https://www.hani.co.kr/rss/politics/',                            'ko'),
 ('economy',     'https://www.hani.co.kr/rss/economy/',                             'ko'),
 ('realestate',  'https://www.mk.co.kr/rss/50300009/',                              'ko'),  -- 매일경제 부동산
 ('tech',        'https://www.theguardian.com/technology/rss',                      'en'),
 ('science',     'https://www.theguardian.com/science/rss',                         'en'),
 ('world',       'https://www.theguardian.com/world/rss',                           'en'),
 ('health',      'http://feeds.bbci.co.uk/news/health/rss.xml',                     'en'),  -- WHO(V6) + BBC
 -- 누락 카테고리 신규(2개씩) --
 ('culture',     'https://www.hani.co.kr/rss/culture/',                             'ko'),
 ('culture',     'https://www.yna.co.kr/rss/culture.xml',                           'ko'),
 ('sports',      'https://www.yna.co.kr/rss/sports.xml',                            'ko'),
 ('sports',      'https://www.hani.co.kr/rss/sports/',                              'ko'),
 ('environment', 'https://www.theguardian.com/environment/rss',                     'en'),
 ('environment', 'http://feeds.bbci.co.uk/news/science_and_environment/rss.xml',    'en');
