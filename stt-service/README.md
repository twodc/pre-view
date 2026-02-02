# STT Service

VibeVoice-ASR 기반 Speech-to-Text 마이크로서비스

## Features

- **실시간 스트리밍 STT**: WebSocket을 통한 실시간 음성 인식
- **파일 전사**: REST API를 통한 오디오 파일 전사
- **GPU 가속**: CUDA 지원 시 자동으로 GPU 활용
- **오디오 전처리**: 자동 리샘플링, 노이즈 제거
- **다국어 지원**: 한국어, 영어 등 다양한 언어 지원

## Quick Start

### 1. Docker로 실행

```bash
# Build
docker build -t stt-service .

# Run (CPU)
docker run -p 8001:8001 stt-service

# Run (GPU)
docker run --gpus all -p 8001:8001 stt-service
```

### 2. 로컬 실행

```bash
# 의존성 설치
pip install -r requirements.txt

# 서비스 실행
python -m app.main
```

서비스는 `http://localhost:8001`에서 실행됩니다.

## API Documentation

### 1. Health Check

```bash
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "model": "samedii/VibeVoice-ASR-v1.0-1B",
  "device": "cuda",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 2. File Transcription

```bash
POST /api/transcribe
Content-Type: multipart/form-data

- file: audio file (.wav, .mp3, .flac, .ogg, .m4a)
- language: language code (optional, e.g., "ko", "en")
```

**Example:**
```bash
curl -X POST http://localhost:8001/api/transcribe \
  -F "file=@audio.wav" \
  -F "language=ko"
```

**Response:**
```json
{
  "text": "안녕하세요, 음성 인식 테스트입니다.",
  "duration": 1.23,
  "language": "ko",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3. WebSocket Streaming

```javascript
// Connect
const ws = new WebSocket('ws://localhost:8001/ws/transcribe?language=ko');

// Send PCM audio chunks (int16, 16kHz, mono)
ws.send(audioChunk);

// Receive transcription
ws.onmessage = (event) => {
  const result = JSON.parse(event.data);
  console.log(result.text, result.is_final);
};
```

**Message Format:**
```json
{
  "text": "부분 전사 결과",
  "is_final": false,
  "confidence": 0.95
}
```

## Configuration

환경변수를 통해 설정 가능:

```bash
# Model
MODEL_NAME=samedii/VibeVoice-ASR-v1.0-1B

# Audio
SAMPLE_RATE=16000
CHUNK_DURATION=3.0

# Server
HOST=0.0.0.0
PORT=8001
LOG_LEVEL=INFO

# Limits
MAX_FILE_SIZE=104857600  # 100MB
```

## Architecture

```
stt-service/
├── app/
│   ├── main.py                 # FastAPI application
│   ├── config.py               # Configuration
│   ├── api/                    # API endpoints
│   │   ├── health.py           # Health check
│   │   ├── transcribe.py       # File transcription
│   │   └── websocket.py        # Streaming STT
│   ├── core/                   # Core logic
│   │   ├── vibevoice_model.py  # Model wrapper
│   │   ├── audio_processor.py  # Audio preprocessing
│   │   └── streaming_buffer.py # Streaming buffer
│   └── schemas/                # Pydantic schemas
│       └── transcription.py
├── Dockerfile
├── requirements.txt
└── README.md
```

## Development

### Testing

```bash
# Install dev dependencies
pip install pytest pytest-asyncio httpx

# Run tests
pytest
```

### API Documentation

Swagger UI: `http://localhost:8001/docs`
ReDoc: `http://localhost:8001/redoc`

## Performance

- **GPU (RTX 4090)**: ~0.1x realtime (1분 오디오 → 6초 처리)
- **CPU (16 cores)**: ~1.0x realtime (1분 오디오 → 60초 처리)

## License

MIT License

## References

- [VibeVoice-ASR](https://huggingface.co/samedii/VibeVoice-ASR-v1.0-1B)
- [FastAPI](https://fastapi.tiangolo.com/)
- [Transformers](https://huggingface.co/docs/transformers/)
