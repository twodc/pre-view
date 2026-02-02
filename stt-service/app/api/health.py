"""Health Check Endpoint"""
from fastapi import APIRouter
from ..schemas import HealthResponse
from ..core.vibevoice_model import get_model
from ..config import settings

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """헬스체크 엔드포인트"""
    model = get_model()

    return HealthResponse(
        status="healthy" if model.is_loaded() else "unhealthy",
        model=settings.MODEL_NAME,
        device=settings.DEVICE
    )
