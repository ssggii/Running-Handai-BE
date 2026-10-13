#!/bin/bash
# C(트랙포인트 저장) 측정용 DB를 운영 규모로 맞춘다.
#   - course: 운영과 같은 분포의 57개 (B의 시드 current.csv 재사용)
#   - track_point: 코스마다 2,319개(운영 평균), 합계 132,183행 (운영 추정 약 13만 행)
#
# 사용법 (measure/c-trackpoint-batch 에서 실행): ./setup.sh
# B의 시드 CSV가 먼저 있어야 한다: cd ../b-spatial-index && python3 -I seed_courses.py data
set -euo pipefail

CONTAINER=runninghandai-local-db
DIR="$(cd "$(dirname "$0")" && pwd)"

mysql_exec() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다
  docker exec -i "$CONTAINER" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

echo "[1/3] track_point 비우기"
mysql_exec <<SQL
TRUNCATE TABLE track_point;
SQL

echo "[2/3] course 57개 적재 (B 시드)"
"$DIR/../b-spatial-index/load.sh" reset
"$DIR/../b-spatial-index/load.sh" current

echo "[3/3] 배경 track_point 적재 (코스마다 2,319개)"
mysql_exec <<SQL
SET SESSION cte_max_recursion_depth = 3000;
INSERT INTO track_point (course_id, lat, lon, ele, sequence, created_at, updated_at)
WITH RECURSIVE seq (n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 2319
)
SELECT c.course_id,
       ST_Latitude(c.start_point) + seq.n * 0.00001,
       ST_Longitude(c.start_point) + seq.n * 0.00001,
       10 + seq.n % 50,
       seq.n,
       NOW(6), NOW(6)
FROM course c CROSS JOIN seq;
ANALYZE TABLE track_point;
SELECT (SELECT COUNT(*) FROM course) AS courses, (SELECT COUNT(*) FROM track_point) AS track_points;
SQL
