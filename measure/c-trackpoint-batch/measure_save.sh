#!/bin/bash
# 트랙포인트 저장 구간의 시간과 DB가 받은 쿼리 수를 잰다.
#
# 사용법 (measure/c-trackpoint-batch 에서 실행, 앱은 local,measure 프로필로 떠 있어야 한다):
#   ./measure_save.sh <라벨> <create|replace> <포인트 수>
#   예) ./measure_save.sh before create 1156
#       ./measure_save.sh before replace 1299
#
#   create : GPX 코스 생성과 같은 흐름 (새 코스 1건 저장 + 트랙포인트 저장)
#   replace: 두루누비 동기화와 같은 흐름 (코스 조회 + 기존 트랙포인트 삭제 + 새 트랙포인트 저장)
#            매 회 같은 포인트 수의 코스를 먼저 만들어 두고(측정 밖), 그 코스를 교체한다
#
# 기록하는 값
#   - 시간: 앱이 잰 저장 구간 시간(메서드 호출 ~ flush). 워밍업 후 여러 번의 중앙값, 최솟값, 최댓값
#   - 쿼리 수: 측정 호출 한 번 동안 MySQL이 실제로 받은 INSERT, DELETE, SELECT, UPDATE 문 수
#     (SHOW GLOBAL STATUS의 Com_* 증가분. 드라이버가 여러 행을 한 문장으로 묶어 보내면 1로 센다)
#     create는 코스 INSERT 1건, replace는 코스 SELECT 1건이 포함된다
# 측정 전후로 측정용 코스(measure-c-*)와 그 트랙포인트를 지운다. 결과는 results.tsv에 쌓인다.
#
# 시간을 잴 때는 p6spy 로그를 끈 상태로 앱을 띄운다:
#   SPRING_PROFILES_ACTIVE=local,measure DECORATOR_DATASOURCE_ENABLED=false docker compose up -d app
set -euo pipefail

LABEL=${1:?라벨을 주세요. 예: before}
MODE=${2:?create 또는 replace}
POINTS=${3:?포인트 수를 주세요. 예: 1156}
WARMUP=${WARMUP:-3}
RUNS=${RUNS:-10}
APP=${APP:-http://localhost:8080}
CONTAINER=runninghandai-local-db
DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS="$DIR/results.tsv"

case "$MODE" in create|replace) ;; *) echo "모드는 create 또는 replace"; exit 1 ;; esac

sql() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다
  docker exec -i "$CONTAINER" sh -c 'mysql -N -B -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

# Com_delete, Com_insert, Com_select, Com_update 순서 (SHOW STATUS는 Com_select에 잡히지 않는다)
counters() {
  echo "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_insert', 'Com_delete', 'Com_select', 'Com_update');" | sql | sort | awk '{printf "%s ", $2}'
}

cleanup() {
  sql <<SQL
DELETE tp FROM track_point tp JOIN course c ON c.course_id = tp.course_id WHERE c.name LIKE 'measure-c-%';
DELETE FROM course WHERE name LIKE 'measure-c-%';
SQL
}

call() {
  curl -sf -X POST "$APP/measure/track-points/$1" | python3 -I -c 'import json,sys; d=json.load(sys.stdin); print(d["courseId"], d["elapsedMs"])'
}

cleanup
rows=()
for i in $(seq 1 $((WARMUP + RUNS))); do
  if [ "$MODE" = create ]; then
    path="create?points=$POINTS"
  else
    read -r course_id _ <<<"$(call "create?points=$POINTS")"
    path="replace?courseId=$course_id&points=$POINTS"
  fi
  before=$(counters)
  read -r _ elapsed <<<"$(call "$path")"
  after=$(counters)
  if [ "$i" -gt "$WARMUP" ]; then
    rows+=("$elapsed $before$after")
  fi
  echo "  $i/$((WARMUP + RUNS)) ${elapsed}ms"
done
cleanup

printf '%s\n' "${rows[@]}" | python3 -I -c '
import datetime, os, statistics, sys
label, mode, points, results = sys.argv[1:5]
times, deltas = [], set()
for line in sys.stdin.read().split("\n"):
    if not line.strip():
        continue
    v = line.split()
    times.append(float(v[0]))
    b, a = list(map(int, v[1:5])), list(map(int, v[5:9]))
    d = [y - x for x, y in zip(b, a)]  # delete, insert, select, update
    d[2] -= 1  # 측정 후 카운터를 읽으려고 mysql 클라이언트가 접속할 때 SELECT 1건이 늘어난다 (호출 없이 연속으로 읽어 확인)
    deltas.add(tuple(d))
cols = list(zip(*sorted(deltas)))
fmt = lambda xs: "/".join(str(x) for x in sorted(set(xs)))
line = [datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), label, mode, points, len(times),
        f"{statistics.median(times):.1f}", f"{min(times):.1f}", f"{max(times):.1f}",
        fmt(cols[1]), fmt(cols[0]), fmt(cols[2]), fmt(cols[3])]
header = ["time", "label", "mode", "points", "runs", "median_ms", "min_ms", "max_ms", "inserts", "deletes", "selects", "updates"]
new = not os.path.exists(results)
with open(results, "a") as f:
    if new:
        f.write("\t".join(header) + "\n")
    f.write("\t".join(map(str, line)) + "\n")
print("\t".join(header)); print("\t".join(map(str, line)))
' "$LABEL" "$MODE" "$POINTS" "$RESULTS"
