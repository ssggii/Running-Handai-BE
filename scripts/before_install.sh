#!/bin/bash
# scripts/before_install.sh

echo "Running BeforeInstall hook..."

# 1. appspec.yml의 destination에 해당하는 배포 디렉터리 정의
DEPLOY_DIR="/home/ubuntu/deploy/app"

# 2. 배포 디렉터리 정리
echo "Cleaning up target directory: $DEPLOY_DIR"

# 디렉터리가 존재하면 내용물만 삭제하고, 디렉터리가 없으면 생성한다.
if [ -d "$DEPLOY_DIR" ]; then
    sudo rm -rf "$DEPLOY_DIR"/*
else
    mkdir -p "$DEPLOY_DIR"
fi

# 3. 이전에 실패한 배포로 인해 남아있을 수 있는 컨테이너 정리
echo "Cleaning up any old containers that might have failed to deploy..."
docker rm runninghandai-blue-8081 2>/dev/null || true
docker rm runninghandai-green-8082 2>/dev/null || true

echo "BeforeInstall hook finished."