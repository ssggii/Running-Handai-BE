#!/bin/bash
# B 7단계: 반경 검색 통제 실험. 같은 테이블(course_exp, controlled_setup.sql)에서 한 번에 변수 하나만 바꾼다.
#
# 사용법 (measure/b-spatial-index 에서 실행):
#   ./controlled.sh base <지점>                    # 지점의 기준 결과 ID 저장 (원래 조건, 전체 스캔). 대조 기준
#   ./controlled.sh run <칸> <지점> [warm|cold]    # 칸 하나 측정 (기본 warm)
#   ./controlled.sh e <직전상태> [반복수]          # 실험 E: 옵티마이저가 고르는 계획과 비용 기록
#   ./controlled.sh sql <칸> <지점>                # 실행할 SQL만 출력
#
# 지점: haeundae(해운대) | gangnam(강남역) | daejeon(대전) | taebaek(태백)
# 칸:
#   A1  전체 스캔 + 거리 조건만                     (원래 쿼리의 조건)
#   A2  전체 스캔 + 사각형(MBRContains) + 거리
#   A3  전체 스캔 + 위도·경도 범위(STORED) + 거리
#   B1  전체 스캔         ┐
#   B2  B-tree(STORED) 강제│ 조건절이 모두 같다: 사각형 + 위도·경도 범위(STORED) + 거리. 힌트만 다르다
#   B3  공간 인덱스 강제   │
#   B0  힌트 없음         ┘ (옵티마이저가 고른 계획)
#   N2  B-tree(STORED) 강제 + 위도·경도 범위 + 거리   (B-tree 방식의 원래 조건)
#   N3  공간 인덱스 강제 + 사각형 + 거리              (앱 쿼리와 같은 조건)
# 직전 상태 (실험 E, 조건: 위도·경도 범위(가상 컬럼) + 거리, 힌트 없음. 4단계 후보 3과 같은 조건):
#   cold         MySQL 재시작 + 도커 VM 페이지 캐시 비움 직후
#   after_full   같은 테이블을 전체 스캔한 직후
#   after_index  가상 컬럼 B-tree를 강제로 탄 직후 (테이블 행 조회가 인덱스 페이지를 밀어내 인덱스는 거의 남지 않는다)
#   index_loaded 가상 컬럼 B-tree만 끝까지 읽은 직후 (인덱스 전체가 버퍼 풀에 있는 상태)
#
# warm: 워밍업 3회 후 10회 실행, 서버 측 시간 중앙값·최솟값·최댓값. 마지막에 EXPLAIN ANALYZE를 plans/에 저장
# cold: 매번 MySQL 재시작(버퍼 풀 덤프·복원 끔) + 도커 VM 페이지 캐시 비움 후 첫 실행. COLD_RUNS회(기본 3) 반복
# 결과: controlled.tsv (A·B·N), controlled-e.tsv (E), plans/ (SQL, 실행 계획, 옵티마이저 트레이스 원문)
set -euo pipefail

CMD=${1:?사용법은 파일 머리말을 보세요}
WARMUP=${WARMUP:-3}
RUNS=${RUNS:-10}
COLD_RUNS=${COLD_RUNS:-3}
CONTAINER=runninghandai-local-db
IMAGE=mysql:8.0.41
DIR="$(cd "$(dirname "$0")" && pwd)"
PLANS="$DIR/plans"
mkdir -p "$PLANS"

sql() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다. --comments: 측정 표시 주석을 서버로 보낸다
  docker exec -i "$CONTAINER" sh -c 'mysql --comments -N -B -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}
sql_raw() {  # 사람이 읽을 출력 (EXPLAIN FORMAT=TREE 등)
  docker exec -i "$CONTAINER" sh -c 'mysql --comments -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

point_coords() {
  case "$1" in
    haeundae) echo "35.1587 129.1604" ;;
    gangnam)  echo "37.4979 127.0276" ;;
    daejeon)  echo "36.3504 127.3845" ;;
    taebaek)  echo "37.1641 128.9856" ;;
    *) echo "지점: haeundae | gangnam | daejeon | taebaek" >&2; exit 1 ;;
  esac
}

# 반경 5km를 빠짐없이 감싸는 사각형. 앱의 GeoBoundingBox와 같은 계산 (지구 반지름 6,370,986m, 반경 1% 여유, 경도는 정확한 구면식)
bounding_box() {
  python3 -I - "$1" "$2" <<'PY'
import math, sys
lat, lon = float(sys.argv[1]), float(sys.argv[2])
d = 5000 * 1.01 / 6370986
dlat = math.degrees(d)
dlon = math.degrees(math.asin(min(1.0, math.sin(d) / math.cos(math.radians(lat)))))
print(f"{lat - dlat:.7f} {lat + dlat:.7f} {lon - dlon:.7f} {lon + dlon:.7f}")
PY
}

build_query() {  # $1 칸, $2 지점 → QUERY, IDQUERY, HINT, WHERE 설정
  local cell=$1 point=$2 lat lon la0 la1 lo0 lo1
  read -r lat lon <<<"$(point_coords "$point")"
  read -r la0 la1 lo0 lo1 <<<"$(bounding_box "$lat" "$lon")"
  local user="ST_PointFromText('POINT($lat $lon)', 4326)"
  DIST="ST_Distance_Sphere(c.start_point, $user)"
  local mbr="MBRContains(ST_GeomFromText('POLYGON(($la0 $lo0, $la0 $lo1, $la1 $lo1, $la1 $lo0, $la0 $lo0))', 4326), c.start_point)"
  local lat_s="c.start_lat_s BETWEEN $la0 AND $la1 AND c.start_lon_s BETWEEN $lo0 AND $lo1"
  local lat_v="c.start_lat BETWEEN $la0 AND $la1 AND c.start_lon BETWEEN $lo0 AND $lo1"
  local all="idx_exp_location, idx_exp_lat_lon_v, idx_exp_lat_lon_s"
  case "$cell" in
    A1) HINT="IGNORE INDEX ($all)";               WHERE="$DIST <= 5000" ;;
    A2) HINT="IGNORE INDEX ($all)";               WHERE="$mbr AND $DIST <= 5000" ;;
    A3) HINT="IGNORE INDEX ($all)";               WHERE="$lat_s AND $DIST <= 5000" ;;
    B1) HINT="IGNORE INDEX ($all)";               WHERE="$mbr AND $lat_s AND $DIST <= 5000" ;;
    B2) HINT="FORCE INDEX (idx_exp_lat_lon_s)";   WHERE="$mbr AND $lat_s AND $DIST <= 5000" ;;
    B3) HINT="FORCE INDEX (idx_exp_location)";    WHERE="$mbr AND $lat_s AND $DIST <= 5000" ;;
    B0) HINT="";                                  WHERE="$mbr AND $lat_s AND $DIST <= 5000" ;;
    N2) HINT="FORCE INDEX (idx_exp_lat_lon_s)";   WHERE="$lat_s AND $DIST <= 5000" ;;
    N3) HINT="FORCE INDEX (idx_exp_location)";    WHERE="$mbr AND $DIST <= 5000" ;;
    E)  HINT="";                                  WHERE="$lat_v AND $DIST <= 5000" ;;
    EF) HINT="FORCE INDEX (idx_exp_lat_lon_v)";   WHERE="$lat_v AND $DIST <= 5000" ;;
    *) echo "칸: A1 A2 A3 B0 B1 B2 B3 N2 N3" >&2; exit 1 ;;
  esac
  # 앱 쿼리와 같은 모양 (CourseRepository.findCoursesNearbyUser). 칸마다 힌트와 WHERE만 다르다
  QUERY="SELECT c.course_id AS id, c.name, ci.img_url AS thumbnailUrl, c.distance, c.duration, c.max_ele AS maxElevation, ($DIST / 1000) AS distanceFromUser FROM course_exp c $HINT LEFT JOIN course_image_exp ci ON c.course_id = ci.course_id WHERE $WHERE ORDER BY distanceFromUser ASC"
  IDQUERY="SELECT c.course_id FROM course_exp c $HINT WHERE $WHERE"
}

enable_history() {
  sql <<SQL >/dev/null
UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME = 'events_statements_history_long';
TRUNCATE TABLE performance_schema.events_statements_history_long;
SQL
}

status_reads() {
  echo "SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads';" | sql
}

classic_plan() {  # EXPLAIN의 c 줄 요약
  echo "EXPLAIN $QUERY;" | sql | awk -F'\t' '$3=="c" {printf "type=%s key=%s rows=%s extra=%s", $5, $7, $10, $12}'
}

# 버퍼 풀과 OS 캐시를 비운다: 버퍼 풀 덤프·복원을 끄고 재시작, 그동안 도커 VM의 페이지 캐시를 비운다
cold_reset() {
  echo "SET GLOBAL innodb_buffer_pool_dump_at_shutdown = OFF;" | sql >/dev/null
  docker exec "$CONTAINER" rm -f /var/lib/mysql/ib_buffer_pool
  docker stop "$CONTAINER" >/dev/null
  docker run --rm --privileged --entrypoint sh "$IMAGE" -c 'sync; echo 3 > /proc/sys/vm/drop_caches'
  docker start "$CONTAINER" >/dev/null
  until docker exec "$CONTAINER" sh -c 'mysqladmin -uroot -p"$MYSQL_ROOT_PASSWORD" ping' 2>/dev/null | grep -q alive; do sleep 1; done
  until echo "SELECT 1;" | sql | grep -q '^1$'; do sleep 1; done
}

compare_ids() {  # 기준 결과와 차집합: "빠진 수 더해진 수"
  sql <<SQL
DROP TABLE IF EXISTS b_ctrl_cand;
CREATE TABLE b_ctrl_cand (course_id BIGINT PRIMARY KEY) AS $IDQUERY;
SELECT (SELECT COUNT(*) FROM b_ctrl_base_$POINT b WHERE NOT EXISTS (SELECT 1 FROM b_ctrl_cand x WHERE x.course_id = b.course_id)),
       (SELECT COUNT(*) FROM b_ctrl_cand x WHERE NOT EXISTS (SELECT 1 FROM b_ctrl_base_$POINT b WHERE b.course_id = x.course_id));
DROP TABLE b_ctrl_cand;
SQL
}

case "$CMD" in
  sql)
    POINT=${3:?지점}; build_query "${2:?칸}" "$POINT"; echo "$QUERY;" ;;

  base)
    POINT=${2:?지점}; build_query A1 "$POINT"
    echo "DROP TABLE IF EXISTS b_ctrl_base_$POINT; CREATE TABLE b_ctrl_base_$POINT (course_id BIGINT PRIMARY KEY) AS $IDQUERY;" | sql
    echo "[$POINT] 기준 결과: $(echo "SELECT COUNT(*) FROM b_ctrl_base_$POINT;" | sql)개" ;;

  run)
    CELL=${2:?칸}; POINT=${3:?지점}; CACHE=${4:-warm}
    build_query "$CELL" "$POINT"
    PLAN_FILE="$PLANS/${CELL}_${POINT}.txt"
    echo "[$CELL $POINT $CACHE] $(classic_plan)"
    times=""; reads_total=0
    if [ "$CACHE" = warm ]; then
      enable_history
      for i in $(seq 1 "$WARMUP"); do echo "/* bctrl:$CELL:$POINT:warmup */ $QUERY;"; done | sql >/dev/null
      r0=$(status_reads)
      for i in $(seq 1 "$RUNS"); do echo "/* bctrl:$CELL:$POINT:run */ $QUERY;"; done | sql >/dev/null
      r1=$(status_reads); reads_total=$((r1 - r0))
      stats=$(echo "SELECT TIMER_WAIT / 1000000000, ROWS_SENT, ROWS_EXAMINED FROM performance_schema.events_statements_history_long WHERE SQL_TEXT LIKE '/* bctrl:$CELL:$POINT:run */%' ORDER BY EVENT_ID;" | sql)
      {
        echo "-- 칸 $CELL / 지점 $POINT / $(date '+%Y-%m-%d %H:%M')"
        echo "$QUERY;"
        echo; echo "-- EXPLAIN"; echo "EXPLAIN $QUERY;" | sql_raw
        echo; echo "-- EXPLAIN FORMAT=TREE"; echo "EXPLAIN FORMAT=TREE $QUERY;" | sql | sed 's/\\n/\n/g'
        echo; echo "-- EXPLAIN ANALYZE"; echo "EXPLAIN ANALYZE $QUERY;" | sql | sed 's/\\n/\n/g'
      } > "$PLAN_FILE"
    else
      stats=""
      for i in $(seq 1 "$COLD_RUNS"); do
        cold_reset; enable_history
        r0=$(status_reads)
        echo "/* bctrl:$CELL:$POINT:cold */ $QUERY;" | sql >/dev/null
        r1=$(status_reads); reads_total=$((reads_total + r1 - r0))
        stats+=$(echo "SELECT TIMER_WAIT / 1000000000, ROWS_SENT, ROWS_EXAMINED FROM performance_schema.events_statements_history_long WHERE SQL_TEXT LIKE '/* bctrl:$CELL:$POINT:cold */%';" | sql)$'\n'
        echo "  cold $i/$COLD_RUNS 완료"
      done
    fi
    diff=$(compare_ids)
    python3 -I - "$CELL" "$POINT" "$CACHE" "$reads_total" "$(classic_plan)" "$DIR/controlled.tsv" <<PY
import statistics, sys, os, datetime
cell, point, cache, reads, plan, results = sys.argv[1:7]
rows = [l.split("\t") for l in """$stats""".strip().splitlines() if l.strip()]
times = [float(r[0]) for r in rows]
missing, extra = """$diff""".split()
line = [datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), cell, point, cache, len(times),
        f"{statistics.median(times):.1f}", f"{min(times):.1f}", f"{max(times):.1f}",
        "/".join(sorted({r[1] for r in rows})), "/".join(sorted({r[2] for r in rows})), reads, missing, extra, plan]
header = ["time", "cell", "point", "cache", "runs", "median_ms", "min_ms", "max_ms", "rows_sent", "rows_examined",
          "disk_reads", "missing_vs_base", "extra_vs_base", "plan"]
new = not os.path.exists(results)
with open(results, "a") as f:
    if new:
        f.write("\t".join(header) + "\n")
    f.write("\t".join(map(str, line)) + "\n")
for h, v in zip(header, line):
    print(f"{h:>16}: {v}")
PY
    ;;

  e)
    PRE=${2:?직전 상태: cold | after_full | after_index}; ATTEMPTS=${3:-3}; POINT=haeundae
    for n in $(seq 1 "$ATTEMPTS"); do
      case "$PRE" in
        cold) cold_reset ;;
        after_full)  build_query A1 "$POINT"; echo "$QUERY;" | sql >/dev/null ;;
        after_index) build_query EF "$POINT"; echo "$QUERY;" | sql >/dev/null ;;
        # 인덱스만 끝까지 읽어(테이블 행 조회 없음) 가상 컬럼 B-tree 전체(142MB)를 버퍼 풀에 올린다
        index_loaded) echo "SELECT COUNT(start_lon) FROM course_exp FORCE INDEX (idx_exp_lat_lon_v);" | sql >/dev/null ;;
        *) echo "직전 상태: cold | after_full | after_index | index_loaded"; exit 1 ;;
      esac
      build_query E "$POINT"
      # 옵티마이저 판단 전 상태: 버퍼 풀에 올라와 있는 인덱스별 페이지, 통계 갱신 시각 (통계가 바뀌지 않았음을 확인)
      bp=$(echo "SELECT GROUP_CONCAT(CONCAT(INDEX_NAME, '=', cnt) ORDER BY INDEX_NAME) FROM (SELECT INDEX_NAME, COUNT(*) cnt FROM information_schema.INNODB_BUFFER_PAGE WHERE TABLE_NAME LIKE '%course_exp\`' GROUP BY INDEX_NAME) t;" | sql)
      stats_at=$(echo "SELECT MAX(last_update) FROM mysql.innodb_index_stats WHERE table_name = 'course_exp';" | sql)
      TRACE_FILE="$PLANS/E_${PRE}_${n}.json"
      # 같은 세션에서 트레이스를 켜고 EXPLAIN → 트레이스를 꺼낸다
      sql <<SQL | sed 's/\\n/\n/g; s/\\t/\t/g' > "$TRACE_FILE"
SET optimizer_trace = 'enabled=on', optimizer_trace_max_mem_size = 10000000;
EXPLAIN $QUERY;
SELECT TRACE FROM information_schema.OPTIMIZER_TRACE;
SQL
      enable_history
      echo "/* bctrl:E:$PRE:$n */ $QUERY;" | sql >/dev/null
      took=$(echo "SELECT TIMER_WAIT / 1000000000 FROM performance_schema.events_statements_history_long WHERE SQL_TEXT LIKE '/* bctrl:E:$PRE:$n */%';" | sql)
      python3 -I - "$PRE" "$n" "$TRACE_FILE" "$bp" "$stats_at" "$took" "$DIR/controlled-e.tsv" <<'PY'
import json, sys, os, datetime
pre, n, trace_file, bp, stats_at, took, results = sys.argv[1:8]
text = open(trace_file).read()
start = text.index("{")
trace = json.loads(text[start:text.rindex("}") + 1])
found = {"table_scan": None, "ranges": [], "chosen": None}
def walk(o):
    if isinstance(o, dict):
        if "table_scan" in o and isinstance(o["table_scan"], dict) and found["table_scan"] is None:
            found["table_scan"] = o["table_scan"]
        if "range_scan_alternatives" in o:
            found["ranges"] += o["range_scan_alternatives"]
        if "best_access_path" in o and "course_exp" in str(o.get("table", "")):
            for p in o["best_access_path"].get("considered_access_paths", []):
                if p.get("chosen"):
                    found["chosen"] = p
        for v in o.values():
            walk(v)
    elif isinstance(o, list):
        for v in o:
            walk(v)
walk(trace)
ts = found["table_scan"] or {}
rng = [r for r in found["ranges"] if r.get("index") == "idx_exp_lat_lon_v"]
r = rng[0] if rng else {}
ch = found["chosen"] or {}
line = [datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), pre, n, ch.get("access_type", "?"),
        ts.get("rows"), ts.get("cost"), r.get("rows"), r.get("cost"), r.get("chosen"), r.get("cause", ""),
        f"{float(took):.1f}" if took else "", bp, stats_at]
header = ["time", "pre", "attempt", "chosen_access", "scan_rows", "scan_cost", "range_rows", "range_cost",
          "range_chosen", "range_cause", "took_ms", "buffer_pages_by_index", "stats_last_update"]
new = not os.path.exists(results)
with open(results, "a") as f:
    if new:
        f.write("\t".join(header) + "\n")
    f.write("\t".join(map(str, line)) + "\n")
print("  ".join(f"{h}={v}" for h, v in zip(header[1:], line[1:])))
PY
    done ;;

  *) sed -n '2,30p' "$0"; exit 1 ;;
esac
