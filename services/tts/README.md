# TTS Service (Qwen3-TTS)

Qwen3-TTS 모델 기반 음성 합성 서비스

## 기술 스택

- **Python**: 3.11
- **Framework**: FastAPI
- **TTS Model**: Qwen/Qwen3-TTS-1.7B
- **ML Library**: PyTorch, Transformers
- **Audio**: scipy, soundfile, pydub

## 디렉토리 구조

```
tts-service/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI 앱
│   ├── config.py               # 설정
│   ├── api/
│   │   ├── __init__.py
│   │   ├── synthesize.py       # 음성 합성 (REST)
│   │   ├── stream.py           # 스트리밍 합성
│   │   ├── voices.py           # 음성 목록 조회
│   │   └── health.py           # 헬스체크
│   ├── core/
│   │   ├── __init__.py
│   │   ├── qwen_tts.py         # Qwen3-TTS 래퍼
│   │   └── audio_encoder.py    # 오디오 인코딩
│   └── schemas/
│       ├── __init__.py
│       └── synthesis.py        # Pydantic 스키마
├── Dockerfile
├── requirements.txt
└── README.md
```

## API 엔드포인트

### 1. 음성 합성 (Base64)

```bash
POST /api/synthesize
```

**Request:**
```json
{
  "text": "안녕하세요, 음성 합성 테스트입니다.",
  "voice": "female_calm",
  "format": "wav",
  "speed": 1.0
}
```

**Response:**
```json
{
  "success": true,
  "audio_base64": "UklGRi4...",
  "duration": 3.5,
  "format": "wav"
}
```

### 2. 음성 합성 (바이너리)

```bash
POST /api/synthesize/binary
```

오디오 파일을 직접 다운로드합니다.

### 3. 스트리밍 합성

```bash
POST /api/synthesize/stream
```

실시간으로 오디오 청크를 스트리밍합니다.

### 4. 음성 목록 조회

```bash
GET /api/voices
```

**Response:**
```json
{
  "voices": [
    {
      "id": "female_calm",
      "name": "차분한 여성",
      "language": "ko",
      "gender": "female",
      "description": "부드럽고 차분한 여성 목소리"
    }
  ]
}
```

### 5. 헬스체크

```bash
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "service": "TTS Service",
  "version": "1.0.0",
  "device": "cuda",
  "model_loaded": true
}
```

## 사용 가능한 음성

| ID | 이름 | 성별 | 설명 |
|----|------|------|------|
| `female_calm` | 차분한 여성 | female | 부드럽고 차분한 여성 목소리 |
| `female_bright` | 밝은 여성 | female | 밝고 활기찬 여성 목소리 |
| `male_deep` | 중저음 남성 | male | 낮고 안정적인 남성 목소리 |
| `male_friendly` | 친근한 남성 | male | 따뜻하고 친근한 남성 목소리 |
| `neutral_professional` | 전문적인 중성 | neutral | 전문적이고 명확한 중성 목소리 |

## 환경 변수

```env
# 서비스 설정
SERVICE_NAME=TTS Service
SERVICE_VERSION=1.0.0
HOST=0.0.0.0
PORT=8002

# 모델 설정
MODEL_NAME=Qwen/Qwen3-TTS-1.7B
SAMPLE_RATE=24000
DEFAULT_VOICE=female_calm
DEVICE=cuda

# 제한
MAX_TEXT_LENGTH=2000

# 오디오 설정
AUDIO_FORMAT=wav
AUDIO_BITRATE=192k

# 캐싱
ENABLE_MODEL_CACHE=true
CACHE_DIR=./model_cache

# 로깅
LOG_LEVEL=INFO
```

## 로컬 실행

### 1. 의존성 설치

```bash
cd tts-service
pip install -r requirements.txt
```

### 2. 서비스 시작

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8002 --reload
```

### 3. API 문서 확인

http://localhost:8002/docs

## Docker 실행

### 1. 이미지 빌드

```bash
docker build -t tts-service:latest .
```

### 2. 컨테이너 실행 (GPU)

```bash
docker run -d \
  --name tts-service \
  --gpus all \
  -p 8002:8002 \
  -v $(pwd)/model_cache:/app/model_cache \
  tts-service:latest
```

### 3. 컨테이너 실행 (CPU)

```bash
docker run -d \
  --name tts-service \
  -p 8002:8002 \
  -e DEVICE=cpu \
  -v $(pwd)/model_cache:/app/model_cache \
  tts-service:latest
```

## 테스트

### cURL 테스트

```bash
# 음성 합성 (Base64)
curl -X POST http://localhost:8002/api/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "안녕하세요, 테스트입니다.",
    "voice": "female_calm",
    "format": "wav"
  }'

# 음성 합성 (바이너리)
curl -X POST http://localhost:8002/api/synthesize/binary \
  -H "Content-Type: application/json" \
  -d '{
    "text": "안녕하세요, 테스트입니다.",
    "voice": "male_deep",
    "format": "mp3"
  }' \
  --output speech.mp3

# 음성 목록 조회
curl http://localhost:8002/api/voices

# 헬스체크
curl http://localhost:8002/health
```

### Python 클라이언트

```python
import requests
import base64

# 음성 합성 요청
response = requests.post(
    "http://localhost:8002/api/synthesize",
    json={
        "text": "안녕하세요, 음성 합성 테스트입니다.",
        "voice": "female_calm",
        "format": "wav",
        "speed": 1.0
    }
)

if response.json()["success"]:
    # Base64 디코딩
    audio_data = base64.b64decode(response.json()["audio_base64"])

    # 파일 저장
    with open("output.wav", "wb") as f:
        f.write(audio_data)

    print(f"Duration: {response.json()['duration']}s")
```

## 주의사항

1. **모델 다운로드**: 최초 실행 시 Qwen3-TTS 모델(약 3.4GB)을 다운로드합니다.
2. **GPU 메모리**: GPU 사용 시 최소 4GB VRAM 필요
3. **CPU 모드**: CPU에서는 처리 속도가 느릴 수 있습니다.
4. **텍스트 길이**: 최대 2000자까지 지원
5. **MP3 인코딩**: ffmpeg 필요

## 라이선스

MIT License
