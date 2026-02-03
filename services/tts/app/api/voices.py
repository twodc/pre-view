"""Voice Management Endpoints"""
import logging
from fastapi import APIRouter

from app.schemas import VoiceInfo, VoiceListResponse
from app.core.qwen_tts import get_tts_instance

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["Voices"])


@router.get("/voices", response_model=VoiceListResponse)
async def get_available_voices():
    """
    사용 가능한 음성 목록 조회

    Returns:
        VoiceListResponse: 음성 목록
    """
    try:
        tts = get_tts_instance()
        available_voices = tts.get_available_voices()

        voices = [
            VoiceInfo(
                id=voice_id,
                name=voice_data["name"],
                language=voice_data["language"],
                gender=voice_data["gender"],
                description=voice_data["description"],
            )
            for voice_id, voice_data in available_voices.items()
        ]

        logger.info(f"음성 목록 조회 - 총 {len(voices)}개")

        return VoiceListResponse(voices=voices)

    except Exception as e:
        logger.error(f"음성 목록 조회 실패: {str(e)}")
        raise
