"""TTS Service FastAPI Application"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.core.qwen_tts import get_tts_instance
from app.api import health, synthesize, stream, voices

# 로깅 설정
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 라이프사이클 관리"""
    # Startup
    logger.info("=" * 60)
    logger.info(f"{settings.SERVICE_NAME} v{settings.SERVICE_VERSION} 시작")
    logger.info(f"Device: {settings.DEVICE}")
    logger.info(f"Model: {settings.MODEL_NAME}")
    logger.info("=" * 60)

    # TTS 인스턴스 초기화 (선택적 - 요청 시 초기화도 가능)
    try:
        logger.info("TTS 모델 사전 로딩 시작...")
        tts = get_tts_instance()
        tts.initialize()
        logger.info("TTS 모델 사전 로딩 완료")
    except Exception as e:
        logger.warning(f"TTS 모델 사전 로딩 실패 (요청 시 재시도): {str(e)}")

    yield

    # Shutdown
    logger.info(f"{settings.SERVICE_NAME} 종료")


# FastAPI 앱 생성
app = FastAPI(
    title=settings.SERVICE_NAME,
    version=settings.SERVICE_VERSION,
    description="Qwen3-TTS 기반 음성 합성 서비스",
    lifespan=lifespan,
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 제한 필요
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(health.router)
app.include_router(synthesize.router)
app.include_router(stream.router)
app.include_router(voices.router)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=False,  # 프로덕션에서는 False
        log_level=settings.LOG_LEVEL.lower(),
    )
