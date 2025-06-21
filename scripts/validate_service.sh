#!/bin/bash
# scripts/validate_service.sh
echo "Running ValidateService hook..."

# 1. 새로 실행된 포트(NEW_PORT) 찾기
CURRENT_ACTIVE_PORT=$(cat /etc/nginx/conf.d/proxy.conf 2>/dev/null | grep -oP '127.0.0.1:\K(8081|8082)' || echo 8081)

if [ "$CURRENT_ACTIVE_PORT" -eq 8081 ]; then
    NEW_PORT=8082
else
    NEW_PORT=8081
fi

echo "Checking application health on port $NEW_PORT..."
HEALTH_CHECK_URL="http://127.0.0.1:$NEW_PORT/health" # 헬스체크 엔드포인트

# 2. 헬스 체크 시도 (최대 100초 대기)
for i in $(seq 1 20); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL")
    if [ "$STATUS" -eq 200 ]; then
        echo "Application is healthy on port $NEW_PORT. Status: $STATUS"
        # 헬스 체크 성공 시, 성공 코드로 스크립트 종료
        exit 0
    fi
    echo "Health check failed (status: $STATUS) on port $NEW_PORT. Retrying in 5 seconds..."
    sleep 5
done

# 3. 루프가 끝날 때까지 성공하지 못하면 실패 코드로 종료
echo "Health check failed after multiple retries. Aborting deployment."
exit 1