# LLM Service

SGLang + Qwen3-Instruct 기반 Python LLM 서비스

## 개요

이 서비스는 [SGLang](https://github.com/sgl-project/sglang)을 사용하여 Qwen3-32B-Instruct 모델을 제공합니다.
FastAPI 기반으로 구축되었으며, AI 면접 시스템을 위한 다양한 LLM 기능을 제공합니다.

## 주요 기능

- **일반 채팅 완료**: OpenAI 호환 API 형식
- **피드백 생성**: 구조화된 JSON 피드백
- **면접 에이전트**: 다단계 면접 진행 및 의사결정
- **리포트 생성**: 종합 면접 리포트 생성

## 기술 스택

- **LLM 엔진**: SGLang (OpenAI 호환 서버)
- **모델**: Qwen/Qwen3-32B-Instruct
- **프레임워크**: FastAPI + Uvicorn
- **검증**: Pydantic v2
- **런타임**: Python 3.11 + CUDA 12.1

## 디렉토리 구조

```
llm-service/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI 애플리케이션
│   ├── config.py               # 설정 (Pydantic Settings)
│   ├── api/                    # API 엔드포인트
│   │   ├── chat.py             # 일반 채팅
│   │   ├── feedback.py         # 피드백 생성
│   │   ├── interview.py        # 면접 에이전트
│   │   ├── report.py           # 리포트 생성
│   │   └── health.py           # 헬스체크
│   ├── core/                   # 핵심 로직
│   │   ├── llm_engine.py       # SGLang 래퍼 (싱글톤)
│   │   └── prompt_builder.py   # 프롬프트 빌더
│   └── schemas/                # Pydantic 스키마
│       ├── chat.py
│       ├── feedback.py
│       ├── interview.py
│       └── report.py
├── Dockerfile
├── requirements.txt
├── .env.example
└── README.md
```

## 설치 및 실행

### 1. 환경 설정

```bash
# .env 파일 생성
cp .env.example .env

# 필요시 설정 수정
nano .env
```

### 2. SGLang 서버 시작

먼저 SGLang 서버를 별도 프로세스로 실행해야 합니다:

```bash
# SGLang 설치 (GPU 필요)
pip install "sglang[all]"

# 서버 시작
python -m sglang.launch_server \
    --model Qwen/Qwen3-32B-Instruct \
    --port 30000 \
    --host 0.0.0.0
```

### 3. FastAPI 애플리케이션 실행

```bash
# 의존성 설치
pip install -r requirements.txt

# 애플리케이션 시작
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. Docker 실행

```bash
# Docker 이미지 빌드
docker build -t llm-service:latest .

# 컨테이너 실행
docker run -d \
    --name llm-service \
    --gpus all \
    -p 8000:8000 \
    -p 30000:30000 \
    -v $(pwd)/models:/models \
    --env-file .env \
    llm-service:latest
```

## API 엔드포인트

### 헬스체크

```bash
GET /health
```

### 1. 채팅 완료

```bash
POST /api/chat/completions
Content-Type: application/json

{
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

**응답:**

```json
{
  "content": "Hello! How can I help you today?",
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 10,
    "total_tokens": 25
  }
}
```

### 2. 피드백 생성

```bash
POST /api/feedback
Content-Type: application/json

{
  "system_prompt": "You are an expert evaluator. Provide feedback in JSON format.",
  "user_prompt": "Evaluate this answer: ..."
}
```

**응답:**

```json
{
  "feedback": "답변이 명확하고 구조적입니다.",
  "score": 8,
  "is_passed": true,
  "improvement_suggestion": "더 구체적인 예시를 추가하면 좋겠습니다."
}
```

### 3. 면접 스텝 처리

```bash
POST /api/interview/step
Content-Type: application/json

{
  "system_prompt": "You are an AI interviewer. Decide next action in JSON.",
  "user_prompt": "User's answer: ..."
}
```

**응답:**

```json
{
  "thought": "답변이 충분하지 않으므로 추가 질문이 필요합니다.",
  "action": "FOLLOW_UP",
  "message": "조금 더 구체적으로 설명해주시겠어요?",
  "evaluation": "기본 개념은 이해하고 있으나 깊이가 부족합니다."
}
```

**Action 타입:**
- `FOLLOW_UP`: 현재 주제에 대한 추가 질문
- `NEW_TOPIC`: 새로운 주제로 이동
- `NEXT_PHASE`: 현재 페이즈 완료

### 4. 리포트 생성

```bash
POST /api/report
Content-Type: application/json

{
  "system_prompt": "Generate a comprehensive interview report in JSON.",
  "user_prompt": "Interview data: ..."
}
```

**응답:**

```json
{
  "summary": "전반적으로 우수한 기술 역량을 보여주었습니다.",
  "strengths": [
    "알고리즘 이해도가 높음",
    "코드 작성 능력이 우수함"
  ],
  "improvements": [
    "시간 복잡도 분석 능력 향상 필요",
    "엣지 케이스 고려 부족"
  ],
  "recommendations": [
    "자료구조 심화 학습 권장",
    "코딩 테스트 연습 지속"
  ],
  "overall_score": 85,
  "question_feedbacks": [
    {
      "question": "배열의 최댓값을 찾는 알고리즘을 설명하세요.",
      "answer_summary": "선형 탐색으로 O(n) 복잡도 설명",
      "score": 9,
      "feedback": "정확하고 명확한 설명"
    }
  ]
}
```

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MODEL_NAME` | `Qwen/Qwen3-32B-Instruct` | 사용할 모델 이름 |
| `DEVICE` | `cuda` | 디바이스 (cuda/cpu) |
| `MAX_MODEL_LEN` | `32768` | 최대 컨텍스트 길이 |
| `TENSOR_PARALLEL_SIZE` | `1` | 텐서 병렬화 크기 |
| `SGLANG_HOST` | `localhost` | SGLang 서버 호스트 |
| `SGLANG_PORT` | `30000` | SGLang 서버 포트 |
| `APP_HOST` | `0.0.0.0` | FastAPI 서버 호스트 |
| `APP_PORT` | `8000` | FastAPI 서버 포트 |
| `LOG_LEVEL` | `INFO` | 로그 레벨 |
| `DEFAULT_TEMPERATURE` | `0.7` | 기본 temperature |
| `DEFAULT_MAX_TOKENS` | `1000` | 기본 max_tokens |
| `REQUEST_TIMEOUT` | `300` | 요청 타임아웃 (초) |

## 개발

### API 문서

서버 실행 후 다음 URL에서 자동 생성된 API 문서를 확인할 수 있습니다:

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### 로깅

애플리케이션은 구조화된 로깅을 사용합니다. 로그 레벨은 `LOG_LEVEL` 환경 변수로 설정할 수 있습니다:

- `DEBUG`: 상세한 디버깅 정보
- `INFO`: 일반 정보 (기본값)
- `WARNING`: 경고 메시지
- `ERROR`: 오류 메시지

### 테스트

```bash
# 헬스체크
curl http://localhost:8000/health

# 채팅 완료 테스트
curl -X POST http://localhost:8000/api/chat/completions \
    -H "Content-Type: application/json" \
    -d '{
        "messages": [
            {"role": "user", "content": "Hello!"}
        ]
    }'
```

## 성능 최적화

### GPU 메모리 관리

- `MAX_MODEL_LEN`: 컨텍스트 길이를 줄여 메모리 사용량 감소
- `TENSOR_PARALLEL_SIZE`: 여러 GPU에 모델 분산

### 동시 요청 처리

SGLang은 자동으로 배치 처리를 수행하여 동시 요청을 효율적으로 처리합니다.

## 문제 해결

### SGLang 서버가 시작되지 않음

```bash
# CUDA 버전 확인
nvidia-smi

# SGLang 재설치
pip uninstall sglang
pip install "sglang[all]" --upgrade
```

### Out of Memory 오류

- `MAX_MODEL_LEN` 값을 줄이기
- 더 작은 모델 사용 고려 (예: Qwen3-14B)
- `TENSOR_PARALLEL_SIZE` 증가 (GPU가 여러 개인 경우)

### JSON 파싱 오류

LLM이 유효한 JSON을 반환하지 않는 경우:
- 시스템 프롬프트에 JSON 형식 예시 추가
- temperature를 낮춰 더 일관된 출력 생성

## 라이센스

이 프로젝트는 pre-view 프로젝트의 일부입니다.

## 기여

버그 리포트나 기능 요청은 GitHub Issues를 통해 제출해주세요.
