"""Health Check Endpoints"""
import logging
from fastapi import APIRouter
from pydantic import BaseModel

from app.config import settings
from app.core.qwen_tts import get_tts_instance

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Health"])


class HealthResponse(BaseModel):
    """헬스체크 응답"""

    status: str
    service: str
    version: str
    device: str
    model_loaded: bool


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    헬스체크 엔드포인트

    TTS 서비스의 상태를 확인합니다.
    """
    try:
        tts = get_tts_instance()

        return HealthResponse(
            status="healthy",
            service=settings.SERVICE_NAME,
            version=settings.SERVICE_VERSION,
            device=settings.DEVICE,
            model_loaded=tts._initialized,
        )

    except Exception as e:
        logger.error(f"헬스체크 실패: {str(e)}")
        return HealthResponse(
            status="unhealthy",
            service=settings.SERVICE_NAME,
            version=settings.SERVICE_VERSION,
            device=settings.DEVICE,
            model_loaded=False,
        )


@router.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "status": "running",
        "docs": "/docs",
    }
