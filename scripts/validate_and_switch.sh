#!/bin/bash
# 이 스크립트는 헬스 체크, Nginx 전환, 이전 컨테이너 정리를 모두 수행합니다.

echo "Running Validate, Switch, and Cleanup hook..."

# 1. 포트 및 컨테이너 이름 결정
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

HEALTH_CHECK_URL="http://127.0.0.1:$NEW_PORT/health"

# 2. 헬스 체크 및 로직 실행
echo "Checking application health on port $NEW_PORT..."

for i in $(seq 1 20); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL")
    if [ "$STATUS" -eq 200 ]; then
        echo "Application is healthy on port $NEW_PORT. Status: $STATUS"

        echo "Switching Nginx to port $NEW_PORT..."
        # Nginx 설정 변경 (sudo 필요)
        echo "upstream current_backend { server 127.0.0.1:$NEW_PORT; }" | sudo tee /etc/nginx/conf.d/proxy.conf > /dev/null
        # Nginx 리로드 (sudo 필요)
        sudo nginx -s reload

        echo "Stopping and removing old container $OLD_CONTAINER_NAME on port $OLD_PORT..."
        docker stop "$OLD_CONTAINER_NAME" 2>/dev/null || true
        docker rm "$OLD_CONTAINER_NAME" 2>/dev/null || true

        echo "Deployment successful on port $NEW_PORT."
        exit 0
    fi
    echo "Health check failed (status: $STATUS) on port $NEW_PORT. Retrying in 5 seconds..."
    sleep 5
done

echo "Health check failed after multiple retries. Aborting deployment."
exit 1