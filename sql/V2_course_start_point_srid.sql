-- Version 2: course.start_point에 SRID 4326 지정
-- 컬럼에 SRID가 없으면 옵티마이저가 공간 인덱스(idx_course_location)를 쓰지 않는다.
-- 반경 검색이 공간 인덱스로 후보를 먼저 거를 수 있도록 SRID를 지정한다.

-- 0. 적용 전 확인: 기존 데이터가 모두 SRID 4326이어야 한다 (SRID 4326 컬럼에는 4326 값만 들어간다)
-- SELECT ST_SRID(start_point) AS srid, COUNT(*) FROM course GROUP BY ST_SRID(start_point);

-- 1. 공간 인덱스 삭제 (공간 인덱스가 걸린 컬럼은 SRID를 바꿀 수 없다, ERROR 3644)
ALTER TABLE course DROP INDEX idx_course_location;

-- 2. 컬럼에 SRID 4326 지정
ALTER TABLE course MODIFY COLUMN start_point POINT NOT NULL SRID 4326;

-- 3. 공간 인덱스 재생성
CREATE SPATIAL INDEX idx_course_location ON course (start_point);
