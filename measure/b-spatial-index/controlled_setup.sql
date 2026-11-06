-- B 7단계 통제 실험용 테이블. 세 접근 경로(전체 스캔, B-tree, 공간 인덱스)를 같은 테이블·같은 데이터에서 비교한다.
-- 원본: course_srid (4단계에서 만든 420만 행, SRID 4326). 앱이 쓰는 course, course_image는 건드리지 않는다.
--
-- course_exp
--   start_lat,   start_lon   : 가상 컬럼 (4단계 후보 3과 같은 조건, 실험 E에서 원래 현상 재현용)
--   start_lat_s, start_lon_s : STORED 컬럼 (실험 B의 공정한 B-tree 비교용)
--   idx_exp_location   : 공간 인덱스 (start_point)
--   idx_exp_lat_lon_v  : B-tree (start_lat, start_lon)      가상 컬럼
--   idx_exp_lat_lon_s  : B-tree (start_lat_s, start_lon_s)  STORED 컬럼
-- course_image_exp
--   코스마다 이미지 1행, course_id 유니크 (앱 쿼리의 LEFT JOIN과 같은 eq_ref 조인)
--
-- 데이터를 먼저 넣고 인덱스를 나중에 만든다 (행마다 인덱스를 갱신하는 것보다 한 번에 만드는 편이 빠르다).

DROP TABLE IF EXISTS course_image_exp;
DROP TABLE IF EXISTS course_exp;

CREATE TABLE course_exp LIKE course_srid;
ALTER TABLE course_exp
  DROP INDEX idx_course_location,
  DROP INDEX idx_course_lat_lon,
  DROP INDEX UK4xqvdpkafb91tt3hsb67ga3fj,
  DROP INDEX UKftj9sywcqetdlrcts15h17nx3,
  DROP INDEX fk_course_creator,
  ADD COLUMN start_lat_s DOUBLE AS (ST_Latitude(start_point)) STORED,
  ADD COLUMN start_lon_s DOUBLE AS (ST_Longitude(start_point)) STORED;

INSERT INTO course_exp
  (distance, duration, max_ele, min_ele, course_id, created_at, updated_at, external_id, gpx_path, name, area, level,
   start_point, member_id, spot_status)
SELECT distance, duration, max_ele, min_ele, course_id, created_at, updated_at, external_id, gpx_path, name, area, level,
       start_point, member_id, spot_status
FROM course_srid;

ALTER TABLE course_exp ADD SPATIAL INDEX idx_exp_location (start_point);
ALTER TABLE course_exp ADD INDEX idx_exp_lat_lon_v (start_lat, start_lon);
ALTER TABLE course_exp ADD INDEX idx_exp_lat_lon_s (start_lat_s, start_lon_s);

CREATE TABLE course_image_exp (
  course_img_id BIGINT NOT NULL AUTO_INCREMENT,
  course_id     BIGINT NOT NULL,
  img_url       VARCHAR(255) NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  PRIMARY KEY (course_img_id),
  UNIQUE KEY uk_course_image_exp_course (course_id)
);
INSERT INTO course_image_exp (course_id, img_url, created_at, updated_at)
SELECT course_id, CONCAT('https://seed.local/', course_id, '.png'), NOW(6), NOW(6) FROM course_exp;

ANALYZE TABLE course_exp, course_image_exp;

SELECT COUNT(*) AS course_exp_rows FROM course_exp;
SELECT COUNT(*) AS course_image_exp_rows FROM course_image_exp;
SHOW INDEX FROM course_exp;
