"""FastAPI STT Service Application"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import settings
from .core.vibevoice_model import get_model
from .api import health, transcribe, websocket

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    # Startup: Load model
    logger.info("Starting STT Service...")
    logger.info(f"Configuration: {settings}")

    try:
        model = get_model()
        logger.info("Model loaded successfully")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        raise

    yield

    # Shutdown: Cleanup
    logger.info("Shutting down STT Service...")
    if model:
        model.unload_model()


# Create FastAPI app
app = FastAPI(
    title="STT Service",
    description="VibeVoice-ASR based Speech-to-Text Service",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routers
app.include_router(health.router)
app.include_router(transcribe.router)
app.include_router(websocket.router)


@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": "STT Service",
        "model": settings.MODEL_NAME,
        "device": settings.DEVICE,
        "endpoints": {
            "health": "/health",
            "transcribe": "/api/transcribe",
            "websocket": "/ws/transcribe"
        }
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=False,
        log_level=settings.LOG_LEVEL.lower()
    )
