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


# 2. 최신 Docker 이미지 pull
# CodeDeploy가 전달한 파일 중, 이미지 주소가 담긴 파일을 읽어온다.
# image_uri.txt 파일은 GitHub Actions 단계에서 생성된다.
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


# 3. 새로운 Docker 컨테이너 실행
docker rm "$TARGET_CONTAINER_NAME" 2>/dev/null || true # 이전에 실패했을 경우를 대비해, 같은 이름의 컨테이너 삭제

echo "Running new container on port $NEW_PORT with name $TARGET_CONTAINER_NAME..."

docker run -d -p "$NEW_PORT":"$NEW_PORT" --name "$TARGET_CONTAINER_NAME" \
           -e SERVER_PORT="$NEW_PORT" \
           --env-file /home/ubuntu/config/prod.env \
           "$ECR_IMAGE_URI" # 이미지 이름으로 ECR 주소 사용

echo "====================================================="
echo "ApplicationStart hook finished."