#!/bin/bash
# E(트랜잭션 범위) 측정 준비와 정리.
#   ./setup.sh        측정용 회원(member_id 1) 준비 + 이전 측정이 만든 코스 삭제
#   ./setup.sh clean  이전 측정이 만든 코스만 삭제
# 코스 57개와 트랙포인트는 C의 setup.sh로 운영 규모를 맞춘 상태를 그대로 쓴다.
set -euo pipefail

CONTAINER=runninghandai-local-db

mysql_exec() {
  # 비밀번호와 DB 이름은 컨테이너 환경변수로만 쓴다
  docker exec -i "$CONTAINER" sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' 2>&1 | grep -v 'Using a password' || true
}

clean() {
  # 측정 API가 만든 코스 이름은 "측정xxxxxxxx-끝xxxxxxxx" 형식이다
  mysql_exec <<SQL
CREATE TEMPORARY TABLE e_courses AS SELECT course_id FROM course WHERE name LIKE '측정%';
DELETE t FROM track_point t JOIN e_courses e ON e.course_id = t.course_id;
DELETE t FROM road_condition t JOIN e_courses e ON e.course_id = t.course_id;
DELETE t FROM course_image t JOIN e_courses e ON e.course_id = t.course_id;
DELETE t FROM course_themes t JOIN e_courses e ON e.course_id = t.course_course_id;
DELETE c FROM course c JOIN e_courses e ON e.course_id = c.course_id;
SELECT COUNT(*) AS courses FROM course;
SQL
}

if [ "${1:-}" = clean ]; then
  clean
  exit 0
fi

mysql_exec <<SQL
INSERT INTO member (member_id, nickname, email, provider_id, provider, role, created_at, updated_at)
VALUES (1, 'measure-e', 'measure-e@local', 'measure-e', 'KAKAO', 'USER', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE nickname = nickname;
SQL
clean
