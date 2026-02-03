"""TTS API Schemas"""
from typing import Literal
from pydantic import BaseModel, Field, field_validator


class SynthesisRequest(BaseModel):
    """음성 합성 요청"""

    text: str = Field(
        ...,
        description="음성으로 변환할 텍스트",
        min_length=1,
        max_length=2000,
    )
    voice: str = Field(
        default="female_calm",
        description="사용할 음성 스타일",
    )
    format: Literal["wav", "mp3"] = Field(
        default="wav",
        description="출력 오디오 포맷",
    )
    speed: float = Field(
        default=1.0,
        ge=0.5,
        le=2.0,
        description="음성 속도 (0.5 ~ 2.0)",
    )

    @field_validator("text")
    @classmethod
    def validate_text(cls, v: str) -> str:
        """텍스트 유효성 검증"""
        text = v.strip()
        if not text:
            raise ValueError("텍스트가 비어있습니다")
        return text


class SynthesisResponse(BaseModel):
    """음성 합성 응답"""

    success: bool = Field(
        ...,
        description="합성 성공 여부",
    )
    audio_url: str | None = Field(
        default=None,
        description="생성된 오디오 파일 URL",
    )
    audio_base64: str | None = Field(
        default=None,
        description="Base64 인코딩된 오디오 데이터",
    )
    duration: float | None = Field(
        default=None,
        description="오디오 길이 (초)",
    )
    format: str | None = Field(
        default=None,
        description="오디오 포맷",
    )
    error: str | None = Field(
        default=None,
        description="에러 메시지",
    )


class VoiceInfo(BaseModel):
    """음성 정보"""

    id: str = Field(
        ...,
        description="음성 ID",
    )
    name: str = Field(
        ...,
        description="음성 이름",
    )
    language: str = Field(
        ...,
        description="지원 언어",
    )
    gender: Literal["male", "female", "neutral"] = Field(
        ...,
        description="음성 성별",
    )
    description: str = Field(
        ...,
        description="음성 설명",
    )


class VoiceListResponse(BaseModel):
    """음성 목록 응답"""

    voices: list[VoiceInfo] = Field(
        ...,
        description="사용 가능한 음성 목록",
    )
