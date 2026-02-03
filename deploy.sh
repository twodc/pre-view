#!/bin/bash

# ===========================================
# PreView EC2 배포 스크립트
# ===========================================

set -e  # 에러 발생 시 스크립트 중단

echo "=========================================="
echo "PreView 배포 시작"
echo "=========================================="

# 환경변수 파일 확인
if [ ! -f .env ]; then
    echo "❌ .env 파일이 없습니다!"
    echo "   .env.example을 참고하여 .env 파일을 생성해주세요."
    echo "   cp .env.example .env"
    exit 1
fi

echo "✅ .env 파일 확인 완료"

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다!"
    echo "   sudo dnf install -y docker"
    echo "   sudo systemctl start docker"
    echo "   sudo systemctl enable docker"
    exit 1
fi

echo "✅ Docker 확인 완료"

# Docker Compose 설치 확인 (Docker Compose V2)
if ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다!"
    echo "   sudo dnf install -y docker-compose-plugin"
    exit 1
fi

echo "✅ Docker Compose 확인 완료"

# 기존 컨테이너 중지 및 제거
echo ""
echo "🔄 기존 컨테이너 정리 중..."
docker compose -f infra/docker/compose.yml down --remove-orphans || true

# 이미지 빌드 및 컨테이너 실행
echo ""
echo "🔨 이미지 빌드 중... (시간이 걸릴 수 있습니다)"
docker compose -f infra/docker/compose.yml build --no-cache

echo ""
echo "🚀 컨테이너 시작 중..."
docker compose -f infra/docker/compose.yml up -d

# 상태 확인
echo ""
echo "=========================================="
echo "📊 컨테이너 상태"
echo "=========================================="
docker compose -f infra/docker/compose.yml ps

echo ""
echo "=========================================="
echo "✅ 배포 완료!"
echo "=========================================="
echo ""
echo "🌐 접속 URL: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_EC2_IP')"
echo ""
echo "📝 유용한 명령어:"
echo "   로그 확인:     docker compose -f infra/docker/compose.yml logs -f"
echo "   백엔드 로그:   docker compose -f infra/docker/compose.yml logs -f backend"
echo "   프론트 로그:   docker compose -f infra/docker/compose.yml logs -f frontend"
echo "   상태 확인:     docker compose -f infra/docker/compose.yml ps"
echo "   중지:          docker compose -f infra/docker/compose.yml down"
echo "   재시작:        docker compose -f infra/docker/compose.yml restart"
echo ""
