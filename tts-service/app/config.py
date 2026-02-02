"""TTS Service Configuration"""
import os
from typing import Literal
import torch
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """TTS 서비스 설정"""

    # 서비스 설정
    SERVICE_NAME: str = "TTS Service"
    SERVICE_VERSION: str = "1.0.0"
    HOST: str = "0.0.0.0"
    PORT: int = 8002

    # Qwen3-TTS 모델 설정 (Alibaba Qwen, 2026년 최신)
    MODEL_NAME: str = "Qwen/Qwen3-TTS-12Hz-1.7B-Base"
    SAMPLE_RATE: int = 24000
    DEFAULT_VOICE: str = "female_calm"

    # 디바이스 설정 (자동 감지)
    DEVICE: str = "cuda" if torch.cuda.is_available() else "cpu"

    # 텍스트 제한
    MAX_TEXT_LENGTH: int = 2000

    # 오디오 설정
    AUDIO_FORMAT: Literal["wav", "mp3"] = "wav"
    AUDIO_BITRATE: str = "192k"

    # 캐싱 설정
    ENABLE_MODEL_CACHE: bool = True
    CACHE_DIR: str = "./model_cache"

    # 로깅
    LOG_LEVEL: str = "INFO"

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
