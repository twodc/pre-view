"""Speech Synthesis Endpoints"""
import logging
from fastapi import APIRouter, HTTPException
from fastapi.responses import Response

from app.schemas import SynthesisRequest, SynthesisResponse
from app.core.qwen_tts import get_tts_instance
from app.core.audio_encoder import AudioEncoder
from app.config import settings

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["Synthesis"])


@router.post("/synthesize", response_model=SynthesisResponse)
async def synthesize_speech(request: SynthesisRequest):
    """
    텍스트를 음성으로 변환

    Args:
        request: 음성 합성 요청

    Returns:
        SynthesisResponse: Base64 인코딩된 오디오 데이터

    Raises:
        HTTPException: 합성 실패 시
    """
    try:
        logger.info(
            f"음성 합성 요청 - Text: {request.text[:50]}..., "
            f"Voice: {request.voice}, Format: {request.format}"
        )

        # 텍스트 길이 검증
        if len(request.text) > settings.MAX_TEXT_LENGTH:
            raise HTTPException(
                status_code=400,
                detail=f"텍스트가 너무 깁니다 (최대 {settings.MAX_TEXT_LENGTH}자)",
            )

        # TTS 인스턴스 가져오기
        tts = get_tts_instance()

        # 모델 초기화 (최초 1회)
        if not tts._initialized:
            logger.info("TTS 모델 초기화 중...")
            tts.initialize()

        # 음성 합성
        audio_data = tts.synthesize(
            text=request.text,
            voice=request.voice,
            speed=request.speed,
        )

        # 오디오 길이 계산
        duration = AudioEncoder.get_audio_duration(audio_data, settings.SAMPLE_RATE)

        # Base64 인코딩
        audio_base64 = AudioEncoder.encode_to_base64(
            audio_data=audio_data,
            sample_rate=settings.SAMPLE_RATE,
            format=request.format,
        )

        logger.info(f"음성 합성 완료 - Duration: {duration:.2f}s, Format: {request.format}")

        return SynthesisResponse(
            success=True,
            audio_base64=audio_base64,
            duration=duration,
            format=request.format,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"음성 합성 실패: {str(e)}", exc_info=True)
        return SynthesisResponse(
            success=False,
            error=str(e),
        )


@router.post("/synthesize/binary")
async def synthesize_speech_binary(request: SynthesisRequest):
    """
    텍스트를 음성으로 변환 (바이너리 응답)

    Args:
        request: 음성 합성 요청

    Returns:
        Response: 오디오 바이너리 데이터

    Raises:
        HTTPException: 합성 실패 시
    """
    try:
        logger.info(
            f"음성 합성 요청 (바이너리) - Text: {request.text[:50]}..., "
            f"Voice: {request.voice}, Format: {request.format}"
        )

        # TTS 인스턴스 가져오기
        tts = get_tts_instance()

        # 모델 초기화 (최초 1회)
        if not tts._initialized:
            tts.initialize()

        # 음성 합성
        audio_data = tts.synthesize(
            text=request.text,
            voice=request.voice,
            speed=request.speed,
        )

        # 포맷에 따라 바이트로 변환
        if request.format == "wav":
            audio_bytes = AudioEncoder._to_wav_bytes(audio_data, settings.SAMPLE_RATE)
            media_type = "audio/wav"
        elif request.format == "mp3":
            audio_bytes = AudioEncoder._to_mp3_bytes(audio_data, settings.SAMPLE_RATE)
            media_type = "audio/mpeg"
        else:
            raise HTTPException(
                status_code=400,
                detail=f"지원하지 않는 포맷: {request.format}",
            )

        logger.info(f"음성 합성 완료 (바이너리) - Format: {request.format}")

        # 바이너리 응답 반환
        return Response(
            content=audio_bytes.getvalue(),
            media_type=media_type,
            headers={
                "Content-Disposition": f'attachment; filename="speech.{request.format}"'
            },
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"음성 합성 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
