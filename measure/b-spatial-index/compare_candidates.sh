#!/bin/bash
# B 4단계: 반경 검색 해결책 후보를 같은 조건에서 비교한다 (420만 규모).
#
# 사용법 (measure/b-spatial-index 에서 실행):
#   ./compare_candidates.sh baseline   # 원래 쿼리(course)의 결과 ID를 b_baseline_ids에 저장. 비교 기준 (약 22초)
#   ./compare_candidates.sh original   # 원래 쿼리 (course, SRID 없음, 거리 함수)
#   ./compare_candidates.sh c1         # 후보 1: 사각형(MBRContains) + 거리 재확인 (course_srid)
#   ./compare_candidates.sh c2         # 후보 2: ST_Within + ST_Buffer 원 다각형 (course_srid)
#   ./compare_candidates.sh c3         # 후보 3: 위도·경도 B-Tree 범위 + 거리 재확인 (course_srid의 가상 컬럼). 옵티마이저에게 맡김
#   ./compare_candidates.sh c3f        # 후보 3에 FORCE INDEX (idx_course_lat_lon): 인덱스를 강제로 쓰게 했을 때
#
# 기록하는 것: EXPLAIN의 course 줄(type, key, 추정 행 수, Extra를 plan 칸에 함께 저장), 실행 시간(워밍업 후 서버 측 시간 중앙값), 결과 행 수,
#             검사한 행 수, 원래 결과와의 차이(빠진 코스, 더 들어온 코스). 결과는 candidates.tsv에 쌓인다.
set -euo pipefail

LABEL=${1:?후보를 주세요: baseline | original | c1 | c2 | c3 | c3f}
WARMUP=${WARMUP:-3}
RUNS=${RUNS:-10}
CONTAINER=runninghandai-local-db
DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS="$DIR/candidates.tsv"

USER_POINT="ST_PointFromText('POINT(35.158700 129.160400)', 4326)"   # 해운대해수욕장
DIST="ST_Distance_Sphere(c.start_point, $USER_POINT)"
# 반경 5km 원을 감싸는 사각형. 필요한 최소 폭은 위도 ±0.0449662°, 경도 ±0.0550005° (구 반지름 6,370,986m 기준)
LAT_MIN=35.11365; LAT_MAX=35.20375; LON_MIN=129.10530; LON_MAX=129.21550
BOX="ST_GeomFromText('POLYGON(($LAT_MIN $LON_MIN, $LAT_MIN $LON_MAX, $LAT_MAX $LON_MAX, $LAT_MAX $LON_MIN, $LAT_MIN $LON_MIN))', 4326)"

case "$LABEL" in
  baseline|original) TABLE=course;      WHERE="$DIST <= 5000" ;;
  c1)                TABLE=course_srid; WHERE="MBRContains($BOX, c.start_point) AND $DIST <= 5000" ;;
  c2)                TABLE=course_srid; WHERE="ST_Within(c.start_point, ST_Buffer($USER_POINT, 5000))" ;;
  c3)                TABLE=course_srid; WHERE="c.start_lat BETWEEN $LAT_MIN AND $LAT_MAX AND c.start_lon BETWEEN $LON_MIN AND $LON_MAX AND $DIST <= 5000" ;;
  c3f)               TABLE="course_srid c FORCE INDEX (idx_course_lat_lon)"; WHERE="c.start_lat BETWEEN $LAT_MIN AND $LAT_MAX AND c.start_lon BETWEEN $LON_MIN AND $LON_MAX AND $DIST <= 5000" ;;
  *) sed -n '2,13p' "$0"; exit 1 ;;
esac

case "$TABLE" in *" c "*) FROM="$TABLE" ;; *) FROM="$TABLE c" ;; esac
# 앱 쿼리와 같은 모양 (CourseRepository.findCoursesNearbyUser). WHERE만 후보마다 다르다
QUERY="SELECT c.course_id AS id, c.name, ci.img_url AS thumbnailUrl, c.distance, c.duration, c.max_ele AS maxElevation, ($DIST / 1000) AS distanceFromUser FROM $FROM LEFT JOIN course_image ci ON c.course_id = ci.course_id WHERE $WHERE ORDER BY distanceFromUser ASC"

sql() {
  docker exec -i "$CONTAINER" sh -c 'mysql --comments -N -B -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

if [ "$LABEL" = baseline ]; then
  echo "DROP TABLE IF EXISTS b_baseline_ids; CREATE TABLE b_baseline_ids (course_id BIGINT PRIMARY KEY) AS SELECT c.course_id FROM course c WHERE $WHERE;" | sql
  echo "기준 결과 저장: $(echo 'SELECT COUNT(*) FROM b_baseline_ids;' | sql)개"
  exit 0
fi

echo "[$LABEL] EXPLAIN (course 줄)"
plan=$(echo "EXPLAIN $QUERY;" | sql | awk -F'\t' '$3=="c" {printf "type=%s key=%s rows=%s extra=%s", $5, $7, $10, $12}')
echo "  $plan"

sql <<SQL >/dev/null
UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME = 'events_statements_history_long';
TRUNCATE TABLE performance_schema.events_statements_history_long;
SQL
for i in $(seq 1 "$WARMUP"); do echo "/* bcand:$LABEL:warmup */ $QUERY;"; done | sql >/dev/null
for i in $(seq 1 "$RUNS"); do echo "/* bcand:$LABEL:run */ $QUERY;"; done | sql >/dev/null

stats=$(echo "SELECT TIMER_WAIT / 1000000000, ROWS_SENT, ROWS_EXAMINED FROM performance_schema.events_statements_history_long WHERE SQL_TEXT LIKE '/* bcand:$LABEL:run */%' ORDER BY EVENT_ID;" | sql)

# 원래 결과와 비교: 후보 결과 ID를 저장하고 기준과 차집합을 센다
diff=$(sql <<SQL
DROP TABLE IF EXISTS b_cand_ids;
CREATE TABLE b_cand_ids (course_id BIGINT PRIMARY KEY) AS SELECT c.course_id FROM $FROM WHERE $WHERE;
SELECT (SELECT COUNT(*) FROM b_baseline_ids b WHERE NOT EXISTS (SELECT 1 FROM b_cand_ids x WHERE x.course_id = b.course_id)),
       (SELECT COUNT(*) FROM b_cand_ids x WHERE NOT EXISTS (SELECT 1 FROM b_baseline_ids b WHERE b.course_id = x.course_id));
DROP TABLE b_cand_ids;
SQL
)

python3 -I - "$LABEL" "$TABLE" "$RESULTS" "$plan" <<PY
import statistics, sys, os, datetime
label, table, results, plan = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
rows = [l.split("\t") for l in """$stats""".strip().splitlines() if l.strip()]
times = [float(r[0]) for r in rows]
missing, extra = """$diff""".split()
line = [datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), label, table, len(times),
        f"{statistics.median(times):.3f}", f"{min(times):.3f}", f"{max(times):.3f}",
        "/".join(sorted({r[1] for r in rows})), "/".join(sorted({r[2] for r in rows})), missing, extra, plan]
header = ["time", "label", "table", "runs", "median_ms", "min_ms", "max_ms", "rows_sent", "rows_examined",
          "missing_vs_original", "extra_vs_original", "plan"]
new = not os.path.exists(results)
with open(results, "a") as f:
    if new:
        f.write("\t".join(header) + "\n")
    f.write("\t".join(map(str, line)) + "\n")
for h, v in zip(header, line):
    print(f"{h:>20}: {v}")
PY
