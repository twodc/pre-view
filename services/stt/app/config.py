"""Application Configuration"""
import os
import torch
from typing import Literal

class Settings:
    """STT Service Settings"""

    # Model Configuration (Microsoft VibeVoice-ASR)
    MODEL_NAME: str = os.getenv("MODEL_NAME", "microsoft/VibeVoice-ASR")

    # Audio Processing
    SAMPLE_RATE: int = int(os.getenv("SAMPLE_RATE", "16000"))
    CHUNK_DURATION: float = float(os.getenv("CHUNK_DURATION", "3.0"))  # seconds

    # Device Configuration
    DEVICE: Literal["cuda", "cpu"] = "cuda" if torch.cuda.is_available() else "cpu"

    # API Configuration
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8001"))

    # WebSocket Configuration
    WS_MAX_SIZE: int = 16 * 1024 * 1024  # 16MB
    WS_TIMEOUT: int = 300  # 5 minutes

    # File Upload
    MAX_FILE_SIZE: int = 100 * 1024 * 1024  # 100MB
    ALLOWED_AUDIO_FORMATS: set = {".wav", ".mp3", ".flac", ".ogg", ".m4a"}

    # Logging
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    def __repr__(self) -> str:
        return f"Settings(model={self.MODEL_NAME}, device={self.DEVICE}, sample_rate={self.SAMPLE_RATE})"


settings = Settings()
