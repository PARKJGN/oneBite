-- 초기 카테고리 시드 ~10개 (T009). 운영자가 이후 마이그레이션으로 추가/변경(FR-002b).
INSERT INTO categories (code, name_ko, name_en, active, sort_order) VALUES
 ('politics',   '정치',   'Politics',     true, 1),
 ('economy',    '경제',   'Economy',      true, 2),
 ('realestate', '부동산', 'Real Estate',  true, 3),
 ('tech',       '테크',   'Tech',         true, 4),
 ('science',    '과학',   'Science',      true, 5),
 ('world',      '국제',   'World',        true, 6),
 ('culture',    '문화',   'Culture',      true, 7),
 ('sports',     '스포츠', 'Sports',       true, 8),
 ('environment','환경',   'Environment',  true, 9),
 ('health',     '건강',   'Health',       true, 10);
