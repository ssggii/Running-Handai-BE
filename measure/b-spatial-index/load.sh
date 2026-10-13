#!/bin/bash
# B(반경 검색) 측정용 시드를 로컬 측정 DB(runninghandai-local-db)에 넣는다.
#
# 사용법 (measure/b-spatial-index 에서 실행):
#   ./load.sh reset     # course, course_image 비우기
#   ./load.sh current   # 57개 (운영과 같은 분포). reset 후에 실행
#   ./load.sh part1     # 누적 4.2만 개. reset 후에 실행
#   ./load.sh part2     # 누적 42만 개. part1 다음에 실행
#   ./load.sh part3     # 누적 420만 개. part2 다음에 실행
#   ./load.sh check     # 전체 코스 수, 해운대 반경 5km 안 코스 수 확인
#
# CSV는 seed_courses.py로 먼저 만든다: python3 -I seed_courses.py data
set -euo pipefail

CONTAINER=runninghandai-local-db
DATA_DIR="$(cd "$(dirname "$0")" && pwd)/data"

mysql_exec() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다
  docker exec -i "$CONTAINER" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

load_part() {
  local name=$1
  local csv="$DATA_DIR/$name.csv"
  [ -f "$csv" ] || { echo "$csv 없음. seed_courses.py를 먼저 실행하세요"; exit 1; }

  docker cp "$csv" "$CONTAINER:/var/lib/mysql-files/$name.csv"
  local first last
  first=$(head -1 "$csv" | cut -d, -f1)
  last=$(tail -1 "$csv" | cut -d, -f1)

  echo "[$name] course_id $first ~ $last 적재 시작: $(date '+%H:%M:%S')"
  mysql_exec <<SQL
LOAD DATA INFILE '/var/lib/mysql-files/$name.csv'
INTO TABLE course
FIELDS TERMINATED BY ','
(course_id, name, distance, duration, max_ele, min_ele, level, @lat, @lon)
SET start_point = ST_GeomFromText(CONCAT('POINT(', @lat, ' ', @lon, ')'), 4326),
    gpx_path    = 'seed',
    area        = 'ETC',
    spot_status = 'COMPLETED',
    created_at  = NOW(6),
    updated_at  = NOW(6);

INSERT INTO course_image (course_id, img_url, created_at, updated_at)
SELECT course_id, CONCAT('https://seed.local/', course_id, '.png'), NOW(6), NOW(6)
FROM course WHERE course_id BETWEEN $first AND $last;

ANALYZE TABLE course, course_image;
SQL
  docker exec "$CONTAINER" rm -f "/var/lib/mysql-files/$name.csv"
  echo "[$name] 적재 완료: $(date '+%H:%M:%S')"
}

case "${1:-}" in
  reset)
    mysql_exec <<SQL
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE course_image;
TRUNCATE TABLE course;
SET FOREIGN_KEY_CHECKS = 1;
SQL
    echo "course, course_image 비움" ;;
  current|part1|part2|part3)
    load_part "$1" ;;
  check)
    mysql_exec <<SQL
SELECT COUNT(*) AS total_courses,
       SUM(ST_Distance_Sphere(start_point, ST_PointFromText('POINT(35.1587 129.1604)', 4326)) <= 5000) AS in_haeundae_5km,
       (SELECT COUNT(*) FROM course_image) AS images
FROM course;
SQL
    ;;
  *)
    sed -n '2,12p' "$0"; exit 1 ;;
esac
