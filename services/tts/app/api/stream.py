"""Streaming Synthesis Endpoints (Optional)"""
import logging
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
import asyncio
from io import BytesIO

from app.schemas import SynthesisRequest
from app.core.qwen_tts import get_tts_instance
from app.core.audio_encoder import AudioEncoder
from app.config import settings

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["Streaming"])


@router.post("/synthesize/stream")
async def synthesize_speech_stream(request: SynthesisRequest):
    """
    텍스트를 음성으로 변환 (스트리밍)

    실시간으로 오디오 청크를 스트리밍합니다.

    Args:
        request: 음성 합성 요청

    Returns:
        StreamingResponse: 오디오 스트림

    Raises:
        HTTPException: 합성 실패 시
    """
    try:
        logger.info(f"스트리밍 합성 요청 - Text: {request.text[:50]}...")

        async def generate_audio_stream():
            """오디오 청크를 생성하는 제너레이터"""
            try:
                # TTS 인스턴스 가져오기
                tts = get_tts_instance()

                # 모델 초기화 (최초 1회)
                if not tts._initialized:
                    tts.initialize()

                # 음성 합성
                audio_data = await asyncio.to_thread(
                    tts.synthesize,
                    text=request.text,
                    voice=request.voice,
                    speed=request.speed,
                )

                # 포맷에 따라 바이트로 변환
                if request.format == "wav":
                    audio_bytes = AudioEncoder._to_wav_bytes(
                        audio_data, settings.SAMPLE_RATE
                    )
                elif request.format == "mp3":
                    audio_bytes = AudioEncoder._to_mp3_bytes(
                        audio_data, settings.SAMPLE_RATE
                    )
                else:
                    raise ValueError(f"지원하지 않는 포맷: {request.format}")

                # 청크 단위로 전송 (16KB)
                chunk_size = 16 * 1024
                audio_bytes.seek(0)

                while True:
                    chunk = audio_bytes.read(chunk_size)
                    if not chunk:
                        break
                    yield chunk
                    await asyncio.sleep(0)  # 이벤트 루프에 제어 양보

                logger.info("스트리밍 완료")

            except Exception as e:
                logger.error(f"스트리밍 중 오류: {str(e)}")
                raise

        # 미디어 타입 결정
        media_type = "audio/wav" if request.format == "wav" else "audio/mpeg"

        return StreamingResponse(
            generate_audio_stream(),
            media_type=media_type,
            headers={
                "Content-Disposition": f'attachment; filename="speech.{request.format}"',
                "Cache-Control": "no-cache",
            },
        )

    except Exception as e:
        logger.error(f"스트리밍 합성 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
