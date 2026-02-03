"""FastAPI application entry point."""

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.core import LLMEngine
from app.api import chat, feedback, interview, report, health

# Configure logging
logging.basicConfig(
    level=settings.LOG_LEVEL,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """
    Application lifespan manager.

    Initializes resources on startup and cleans up on shutdown.
    """
    # Startup: Initialize LLM Engine
    logger.info("Starting LLM Service...")
    try:
        engine = LLMEngine()
        logger.info("LLM Engine initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize LLM Engine: {e}")
        raise

    yield

    # Shutdown
    logger.info("Shutting down LLM Service...")


# Create FastAPI application
app = FastAPI(
    title="LLM Service",
    description="SGLang + Qwen3-Instruct based LLM service for AI interviews",
    version="0.1.0",
    lifespan=lifespan,
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health.router)
app.include_router(chat.router)
app.include_router(feedback.router)
app.include_router(interview.router)
app.include_router(report.router)


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "llm-service",
        "version": "0.1.0",
        "description": "SGLang + Qwen3-Instruct based LLM service",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=False,
        log_level=settings.LOG_LEVEL.lower(),
    )
