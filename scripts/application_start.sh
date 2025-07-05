#!/bin/bash
# scripts/application_start.sh

echo "Running ApplicationStart hook..."

# 1. 비활성 포트(NEW_PORT) 및 컨테이너 이름 결정
CURRENT_ACTIVE_PORT=$(cat /etc/nginx/conf.d/proxy.conf 2>/dev/null | grep -oP '127.0.0.1:\K(8081|8082)' || echo 8081)

if [ "$CURRENT_ACTIVE_PORT" -eq 8081 ]; then
    NEW_PORT=8082
    TARGET_CONTAINER_NAME="runninghandai-green-8082"
else
    NEW_PORT=8081
    TARGET_CONTAINER_NAME="runninghandai-blue-8081"
fi

echo "Deploy Target Port: $NEW_PORT"
echo "Deploy Target Container Name: $TARGET_CONTAINER_NAME"
echo "====================================================="

# 2. ECR에 Docker 로그인
echo "Logging in to Amazon ECR..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 033504885739.dkr.ecr.ap-northeast-2.amazonaws.com

# 로그인 실패 시 스크립트 중단
if [ $? -ne 0 ]; then
    echo "ECR login failed!"
    exit 1
fi
echo "ECR login successful."

# 3. 최신 Docker 이미지 pull
DEPLOY_DIR="/home/ubuntu/deploy/app"
ECR_IMAGE_URI=$(cat "$DEPLOY_DIR"/image_uri.txt)

if [ -z "$ECR_IMAGE_URI" ]; then
    echo "Error: ECR image URI not found in image_uri.txt"
    exit 1
fi

echo "Pulling Docker image from ECR: $ECR_IMAGE_URI"
docker pull "$ECR_IMAGE_URI"

# pull 실패 시 스크립트 중단
if [ $? -ne 0 ]; then
    echo "Docker pull failed!"
    exit 1
fi
echo "====================================================="

# 4. 새로운 Docker 컨테이너 실행
docker rm "$TARGET_CONTAINER_NAME" 2>/dev/null || true

echo "Running new container on port $NEW_PORT with name $TARGET_CONTAINER_NAME..."

docker run -d -p "$NEW_PORT":"$NEW_PORT" --name "$TARGET_CONTAINER_NAME" \
           --log-driver=awslogs \
           --log-opt awslogs-region=ap-northeast-2 \
           --log-opt awslogs-group="RunninghandaiLogs" \
           --log-opt awslogs-stream-prefix="$TARGET_CONTAINER_NAME" \
           -e SERVER_PORT="$NEW_PORT" \
           --env-file /home/ubuntu/config/prod.env \
           "$ECR_IMAGE_URI"

echo "====================================================="
echo "ApplicationStart hook finished."