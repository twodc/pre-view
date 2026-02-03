"""Health check endpoint."""

import logging
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.core import LLMEngine

logger = logging.getLogger(__name__)
router = APIRouter(tags=["health"])


class HealthResponse(BaseModel):
    """Health check response."""

    status: str
    service: str
    version: str


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    """
    Health check endpoint.

    Returns service status and basic information.
    """
    try:
        # Test LLM engine connectivity
        engine = LLMEngine()
        test_response = engine.chat_completion(
            messages=[{"role": "user", "content": "ping"}],
            max_tokens=5,
        )

        if not test_response.choices:
            raise HTTPException(status_code=503, detail="LLM engine not responding")

        logger.info("Health check passed")
        return HealthResponse(
            status="healthy",
            service="llm-service",
            version="0.1.0",
        )
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        raise HTTPException(status_code=503, detail=f"Service unhealthy: {str(e)}")
