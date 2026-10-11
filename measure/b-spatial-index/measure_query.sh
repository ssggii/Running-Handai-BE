#!/bin/bash
# B(반경 검색) 쿼리 하나를 MySQL에서 직접 실행해 시간을 잰다. 앱의 N+1 조회와 네트워크 시간은 섞지 않는다.
#
# 사용법 (measure/b-spatial-index 에서 실행):
#   ./measure_query.sh <라벨>          예: ./measure_query.sh before-57
#   WARMUP=3 RUNS=10 ./measure_query.sh before-57
#
# 기록하는 것 (워밍업 실행은 버리고 측정 실행만 집계):
#   - 실행 시간: performance_schema의 서버 측 실행 시간(TIMER_WAIT). 중앙값, 최솟값, 최댓값
#   - 결과 행 수(ROWS_SENT), 검사한 행 수(ROWS_EXAMINED)
#   - 측정 실행 동안 디스크에서 읽은 페이지 수(Innodb_buffer_pool_reads 증가분)
#   - 측정 실행 동안 외부 정렬 병합 횟수(Sort_merge_passes 증가분, 0보다 크면 정렬이 임시 파일을 썼다)
#   - course, course_image 테이블 크기(데이터 + 인덱스)
# 결과는 results.tsv에 한 줄씩 쌓인다.
set -euo pipefail

LABEL=${1:?라벨을 주세요. 예: ./measure_query.sh before-57}
WARMUP=${WARMUP:-3}
RUNS=${RUNS:-10}
CONTAINER=runninghandai-local-db
DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS="$DIR/results.tsv"

# 앱이 실행하는 쿼리와 같다 (CourseRepository.findCoursesNearbyUser). 기준점은 해운대해수욕장
QUERY="SELECT c.course_id AS id, c.name, ci.img_url AS thumbnailUrl, c.distance, c.duration, c.max_ele AS maxElevation, (ST_Distance_Sphere(c.start_point, ST_PointFromText('POINT(35.158700 129.160400)', 4326)) / 1000) AS distanceFromUser FROM course c LEFT JOIN course_image ci ON c.course_id = ci.course_id WHERE ST_Distance_Sphere(c.start_point, ST_PointFromText('POINT(35.158700 129.160400)', 4326)) <= 5000 ORDER BY distanceFromUser ASC"

sql() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다. --comments: 측정 표시 주석을 서버로 보낸다
  docker exec -i "$CONTAINER" sh -c 'mysql --comments -N -B -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

repeat_query() {
  local tag=$1 count=$2
  for i in $(seq 1 "$count"); do echo "/* bmeasure:$LABEL:$tag */ $QUERY;"; done
}

sql <<SQL >/dev/null
UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME = 'events_statements_history_long';
TRUNCATE TABLE performance_schema.events_statements_history_long;
SQL

# 1. 워밍업 (결과는 버린다)
repeat_query warmup "$WARMUP" | sql >/dev/null

# 2. 측정 실행과 그동안의 디스크 읽기
reads_before=$(echo "SELECT GROUP_CONCAT(VARIABLE_VALUE ORDER BY VARIABLE_NAME) FROM performance_schema.global_status WHERE VARIABLE_NAME IN ('Innodb_buffer_pool_reads', 'Sort_merge_passes');" | sql)
repeat_query run "$RUNS" | sql >/dev/null
reads_after=$(echo "SELECT GROUP_CONCAT(VARIABLE_VALUE ORDER BY VARIABLE_NAME) FROM performance_schema.global_status WHERE VARIABLE_NAME IN ('Innodb_buffer_pool_reads', 'Sort_merge_passes');" | sql)

# 3. 집계
stats=$(sql <<SQL
SELECT TIMER_WAIT / 1000000000, ROWS_SENT, ROWS_EXAMINED
FROM performance_schema.events_statements_history_long
WHERE SQL_TEXT LIKE '/* bmeasure:$LABEL:run */%'
ORDER BY EVENT_ID;
SQL
)
sizes=$(sql <<SQL
SELECT COUNT(*) FROM course;
SELECT ROUND(SUM(DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 1) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('course', 'course_image');
SQL
)

python3 -I - "$LABEL" "$RESULTS" "$reads_before" "$reads_after" <<PY
import statistics, sys, os, datetime
label, results = sys.argv[1], sys.argv[2]
# 값 순서: Innodb_buffer_pool_reads, Sort_merge_passes (변수 이름순)
before = list(map(int, sys.argv[3].split(",")))
after = list(map(int, sys.argv[4].split(",")))
disk_reads, sort_merge_passes = after[0] - before[0], after[1] - before[1]
rows = [line.split("\t") for line in """$stats""".strip().splitlines() if line.strip()]
times = [float(r[0]) for r in rows]
sent = {r[1] for r in rows}
examined = {r[2] for r in rows}
courses, size_mb = """$sizes""".split()
med = statistics.median(times)
line = [datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), label, courses, size_mb, len(times),
        f"{med:.3f}", f"{min(times):.3f}", f"{max(times):.3f}", "/".join(sorted(sent)), "/".join(sorted(examined)), disk_reads, sort_merge_passes]
header = ["time", "label", "courses", "table_mb", "runs", "median_ms", "min_ms", "max_ms", "rows_sent", "rows_examined", "disk_reads", "sort_merge_passes"]
new = not os.path.exists(results)
with open(results, "a") as f:
    if new:
        f.write("\t".join(header) + "\n")
    f.write("\t".join(map(str, line)) + "\n")
for h, v in zip(header, line):
    print(f"{h:>14}: {v}")
PY
