"""File Transcription Endpoint"""
import logging
import time
from fastapi import APIRouter, UploadFile, File, HTTPException, Form
from typing import Optional

from ..schemas import TranscriptionResponse
from ..core.vibevoice_model import get_model
from ..core.audio_processor import AudioProcessor
from ..config import settings

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api", tags=["transcription"])


@router.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_file(
    file: UploadFile = File(..., description="Audio file to transcribe"),
    language: Optional[str] = Form(None, description="Language code (ko, en, etc.)")
):
    """
    파일 업로드 전사 엔드포인트

    - **file**: 오디오 파일 (.wav, .mp3, .flac, .ogg, .m4a)
    - **language**: 언어 코드 (선택사항)
    """
    start_time = time.time()

    try:
        # Validate file extension
        file_ext = "." + file.filename.split(".")[-1].lower() if "." in file.filename else ""
        if file_ext not in settings.ALLOWED_AUDIO_FORMATS:
            raise HTTPException(
                status_code=400,
                detail=f"Unsupported file format. Allowed: {settings.ALLOWED_AUDIO_FORMATS}"
            )

        # Read file
        audio_bytes = await file.read()

        # Check file size
        if len(audio_bytes) > settings.MAX_FILE_SIZE:
            raise HTTPException(
                status_code=413,
                detail=f"File too large. Max size: {settings.MAX_FILE_SIZE / (1024*1024):.0f}MB"
            )

        # Process audio
        logger.info(f"Processing file: {file.filename}, size: {len(audio_bytes)} bytes")
        processor = AudioProcessor()
        audio = processor.load_audio(audio_bytes)

        # Optional: noise reduction
        # audio = processor.reduce_noise(audio)

        # Transcribe
        model = get_model()
        result = model.transcribe(audio, language=language)

        duration = time.time() - start_time
        logger.info(f"Transcription completed in {duration:.2f}s: {result['text'][:100]}")

        return TranscriptionResponse(
            text=result["text"],
            duration=duration,
            language=result.get("language")
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Transcription failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}")
