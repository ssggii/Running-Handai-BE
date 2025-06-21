#!/bin/bash
# scripts/switch_nginx.sh
echo "Running Traffic Switching and Cleanup hook..."

# 1. 새로운 포트(NEW_PORT)와 이전 포트(OLD_PORT), 컨테이너 이름 찾기
CURRENT_ACTIVE_PORT=$(cat /etc/nginx/conf.d/proxy.conf 2>/dev/null | grep -oP '127.0.0.1:\K(8081|8082)' || echo 8081)

if [ "$CURRENT_ACTIVE_PORT" -eq 8081 ]; then
    NEW_PORT=8082
    OLD_PORT=8081
    OLD_CONTAINER_NAME="runninghandai-blue-8081"
else
    NEW_PORT=8081
    OLD_PORT=8082
    OLD_CONTAINER_NAME="runninghandai-green-8082"
fi

echo "Switching Nginx to port $NEW_PORT..."

# 2. Nginx 설정 파일(proxy.conf)의 포트 번호를 새로운 포트로 변경
# sudo 권한이 필요할 수 있으므로 appspec.yml에서 runas: root 설정 권장
echo "upstream current_backend { server 127.0.0.1:$NEW_PORT; }" | sudo tee /etc/nginx/conf.d/proxy.conf > /dev/null

# 3. Nginx 설정 재로드 (서비스 중단 없음)
sudo nginx -s reload

# Nginx 리로드 성공 여부 확인
if [ $? -eq 0 ]; then
    echo "Nginx reloaded successfully."

    # 4. 이전 버전의 컨테이너 중지 및 삭제
    echo "Stopping and removing old container $OLD_CONTAINER_NAME on port $OLD_PORT..."
    docker stop "$OLD_CONTAINER_NAME" 2>/dev/null || true # || true: 컨테이너가 없어도 에러 내지 않음
    docker rm "$OLD_CONTAINER_NAME" 2>/dev/null || true # || true: 컨테이너가 없어도 에러 내지 않음

    echo "Deployment successfully switched to port $NEW_PORT."
    exit 0
else
    echo "Error: Nginx reload failed."
    exit 1
fi