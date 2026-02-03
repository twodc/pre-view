# Pre-View Makefile
# 편의 명령어 모음

COMPOSE = docker compose -f infra/docker/compose.yml
COMPOSE_LOCAL = docker compose -f infra/docker/compose.local.yml

# === Docker ===
.PHONY: up down logs ps build restart

up:
	$(COMPOSE) up -d

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

build:
	$(COMPOSE) build --no-cache

restart:
	$(COMPOSE) restart

# === Local Development ===
.PHONY: local-up local-down

local-up:
	$(COMPOSE_LOCAL) up -d

local-down:
	$(COMPOSE_LOCAL) down

# === Backend ===
.PHONY: api-build api-run api-test

api-build:
	cd apps/api && ./gradlew build -x test

api-run:
	cd apps/api && ./gradlew bootRun

api-test:
	cd apps/api && ./gradlew test

# === Frontend ===
.PHONY: web-install web-dev web-build

web-install:
	cd apps/web && npm install

web-dev:
	cd apps/web && npm run dev

web-build:
	cd apps/web && npm run build

# === Help ===
.PHONY: help

help:
	@echo "사용 가능한 명령어:"
	@echo ""
	@echo "Docker:"
	@echo "  make up        - 전체 스택 시작"
	@echo "  make down      - 전체 스택 중지"
	@echo "  make logs      - 로그 확인"
	@echo "  make ps        - 컨테이너 상태"
	@echo "  make build     - 이미지 빌드"
	@echo "  make restart   - 재시작"
	@echo ""
	@echo "Local:"
	@echo "  make local-up  - 로컬 DB/Redis만 시작"
	@echo "  make local-down"
	@echo ""
	@echo "Backend (apps/api):"
	@echo "  make api-build - Gradle 빌드"
	@echo "  make api-run   - 로컬 실행"
	@echo "  make api-test  - 테스트"
	@echo ""
	@echo "Frontend (apps/web):"
	@echo "  make web-install - npm install"
	@echo "  make web-dev     - 개발 서버"
	@echo "  make web-build   - 프로덕션 빌드"
