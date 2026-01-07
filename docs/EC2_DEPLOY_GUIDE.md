# PreView EC2 배포 가이드

## 사전 준비

### 1. EC2 인스턴스 생성
- **AMI**: Amazon Linux 2023
- **인스턴스 유형**: t2.micro (프리티어) 또는 t2.small (권장)
- **스토리지**: 최소 20GB
- **키 페어**: 생성 후 `.pem` 파일 안전하게 보관

### 2. 보안 그룹 설정 (인바운드 규칙)
| 유형 | 포트 | 소스 |
|------|------|------|
| SSH | 22 | 내 IP |
| HTTP | 80 | 0.0.0.0/0 |

---

## 배포 순서

### Step 1: EC2 접속
```bash
# 키 페어 권한 설정 (최초 1회)
chmod 400 ~/Downloads/pre-view-ec2-key.pem

# SSH 접속
ssh -i ~/Downloads/pre-view-ec2-key.pem ec2-user@[EC2_PUBLIC_IP]
```

### Step 2: Swap 메모리 추가 (필수!)
t2.micro는 RAM 1GB라서 Swap 없이는 빌드 중 메모리 부족으로 멈춥니다.

```bash
# Swap 2GB 생성
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 재부팅 후에도 유지
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# 확인 (Swap 2GB 표시되어야 함)
free -h
```

### Step 3: Docker 설치
```bash
# 패키지 업데이트
sudo dnf update -y

# Docker 설치
sudo dnf install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Docker Compose 플러그인 설정
sudo mkdir -p /usr/libexec/docker/cli-plugins
sudo ln -s /usr/local/bin/docker-compose /usr/libexec/docker/cli-plugins/docker-compose

# Docker Buildx 설치
mkdir -p ~/.docker/cli-plugins
curl -L "https://github.com/docker/buildx/releases/download/v0.19.3/buildx-v0.19.3.linux-amd64" -o ~/.docker/cli-plugins/docker-buildx
chmod +x ~/.docker/cli-plugins/docker-buildx
```

### Step 4: 재접속 (docker 그룹 적용)
```bash
exit
ssh -i ~/Downloads/pre-view-ec2-key.pem ec2-user@[EC2_PUBLIC_IP]

# 확인
docker --version
docker compose version
docker buildx version
```

### Step 5: 프로젝트 클론
```bash
git clone https://github.com/twodc/pre-view.git
cd pre-view
```

### Step 6: 환경변수 설정
```bash
cp .env.example .env
nano .env
```

`.env` 파일 내용:
```
# MySQL
MYSQL_ROOT_PASSWORD=안전한비밀번호
MYSQL_DATABASE=preview
MYSQL_USERNAME=preview_user
MYSQL_PASSWORD=MySQL사용자비밀번호

# JWT (아래 명령어로 생성)
# openssl rand -base64 32
JWT_SECRET=생성된JWT시크릿키

# Google OAuth2
GOOGLE_CLIENT_ID=구글클라이언트ID
GOOGLE_CLIENT_SECRET=구글클라이언트시크릿

# OAuth2 리다이렉트 URI (EC2 IP로 변경)
OAUTH2_REDIRECT_URI=http://[EC2_PUBLIC_IP]/oauth/callback

# Groq API
GROQ_API_KEY=groq_api_키

# Frontend
VITE_API_BASE_URL=/api/v1

# Cookie 설정 (HTTP 환경)
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
```

저장: `Ctrl+O` → `Enter` → `Ctrl+X`

### Step 7: 배포 실행
```bash
# 줄바꿈 문제 해결 (Windows에서 생성된 경우)
sed -i 's/\r$//' deploy.sh

# 배포 스크립트 실행
bash deploy.sh
```

빌드에 약 5-10분 소요됩니다.

### Step 8: Google OAuth 설정 업데이트
Google Cloud Console에서 승인된 리디렉션 URI 추가:
- `http://[EC2_PUBLIC_IP]/oauth2/callback`
- `http://[EC2_PUBLIC_IP]/oauth/callback`

---

## 배포 완료 후

### 접속 URL
```
http://[EC2_PUBLIC_IP]
```

### 유용한 명령어
```bash
# 컨테이너 상태 확인
docker compose ps

# 전체 로그 확인
docker compose logs -f

# 백엔드 로그만 확인
docker compose logs -f backend

# 컨테이너 재시작
docker compose restart

# 컨테이너 중지
docker compose down

# 메모리 확인
free -h
```

---

## 문제 해결

### SSH 접속 안 됨
- 보안 그룹에 SSH (포트 22) 규칙 확인
- 키 페어 권한: `chmod 400 키페어.pem`

### 접속이 매우 느림 / 멈춤
- 메모리 부족 → Swap 메모리 추가했는지 확인
- AWS 콘솔에서 인스턴스 **중지** → **시작** (재부팅 아님)

### 빌드 실패
```bash
# 로그 확인
docker compose logs backend

# 캐시 없이 다시 빌드
docker compose build --no-cache
docker compose up -d
```

### 웹사이트 접속 안 됨
```bash
# 컨테이너 상태 확인
docker compose ps

# 4개 컨테이너 모두 Up 상태인지 확인
# mysql, redis, backend, frontend
```

### Public IP 변경됨
인스턴스 중지/시작하면 IP가 바뀝니다.
1. AWS 콘솔에서 새 IP 확인
2. `.env` 파일의 `OAUTH2_REDIRECT_URI` 업데이트
3. Google Cloud Console 리디렉션 URI 업데이트
4. `docker compose up -d --build frontend` 실행

---

## 비용 주의사항

- **t2.micro**: 월 750시간 무료 (1년간)
- **스토리지**: 30GB까지 무료
- **데이터 전송**: 월 100GB까지 무료
- 사용 안 할 때는 **인스턴스 중지** 권장 (중지 시 과금 없음)
