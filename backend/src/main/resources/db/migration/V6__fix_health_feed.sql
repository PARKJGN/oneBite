-- health 피드 교체: medicalnewstoday.com/rss 는 404(가용성 소실).
-- WHO 영문 뉴스 피드로 교체(검증: 2026-06 기준 200/live). 운영자는 가용성을 주기 점검한다.
UPDATE rss_sources
   SET url = 'https://www.who.int/rss-feeds/news-english.xml'
 WHERE category_code = 'health'
   AND url = 'https://www.medicalnewstoday.com/rss';
