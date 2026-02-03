"""Transcription Schemas"""
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class TranscriptionRequest(BaseModel):
    """File transcription request"""
    language: Optional[str] = Field(None, description="Language code (e.g., 'ko', 'en')")


class TranscriptionResponse(BaseModel):
    """Transcription response"""
    text: str = Field(..., description="Transcribed text")
    duration: float = Field(..., description="Processing duration in seconds")
    language: Optional[str] = Field(None, description="Detected language")
    timestamp: datetime = Field(default_factory=datetime.now)

    class Config:
        json_schema_extra = {
            "example": {
                "text": "안녕하세요, 음성 인식 테스트입니다.",
                "duration": 1.23,
                "language": "ko",
                "timestamp": "2024-01-01T12:00:00"
            }
        }


class StreamingChunk(BaseModel):
    """Streaming transcription chunk"""
    text: str = Field(..., description="Partial transcription")
    is_final: bool = Field(False, description="Whether this is the final chunk")
    confidence: Optional[float] = Field(None, description="Confidence score (0-1)")


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field("healthy", description="Service status")
    model: str = Field(..., description="Loaded model name")
    device: str = Field(..., description="Device (cuda/cpu)")
    timestamp: datetime = Field(default_factory=datetime.now)
