"""WebSocket Streaming Transcription Endpoint"""
import logging
import json
import asyncio
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import Optional

from ..core.vibevoice_model import get_model
from ..core.audio_processor import AudioProcessor
from ..core.streaming_buffer import StreamingBuffer
from ..schemas import StreamingChunk
from ..config import settings

logger = logging.getLogger(__name__)
router = APIRouter(tags=["streaming"])


@router.websocket("/ws/transcribe")
async def websocket_transcribe(websocket: WebSocket):
    """
    실시간 스트리밍 STT WebSocket

    Protocol:
    - Client sends: Binary PCM audio chunks (int16, 16kHz, mono)
    - Server sends: JSON with {"text": "...", "is_final": true/false}

    Connection params:
    - language: Language code (optional, e.g., ?language=ko)
    """
    await websocket.accept()
    logger.info(f"WebSocket connected: {websocket.client}")

    # Get language from query params
    language = websocket.query_params.get("language")

    # Initialize components
    processor = AudioProcessor()
    buffer = StreamingBuffer()
    model = get_model()

    try:
        while True:
            # Receive audio chunk
            data = await websocket.receive_bytes()

            # Process PCM chunk
            audio_chunk = processor.process_pcm_chunk(data, dtype="int16")
            buffer.add_audio(audio_chunk)

            # Check if we have a complete chunk to transcribe
            if buffer.has_complete_chunk():
                chunk_audio = buffer.get_chunk(keep_overlap=True)

                if chunk_audio is not None:
                    try:
                        # Transcribe chunk
                        result = model.transcribe(chunk_audio, language=language)

                        # Send partial result
                        response = StreamingChunk(
                            text=result["text"],
                            is_final=False
                        )
                        await websocket.send_text(response.model_dump_json())

                        logger.debug(f"Sent partial: {result['text'][:50]}")

                    except Exception as e:
                        logger.error(f"Transcription error: {e}")
                        # Send error but continue
                        await websocket.send_text(
                            json.dumps({"error": str(e), "is_final": False})
                        )

    except WebSocketDisconnect:
        logger.info(f"WebSocket disconnected: {websocket.client}")

        # Process remaining buffer
        remaining = buffer.get_remaining()
        if remaining is not None and len(remaining) > 0:
            try:
                result = model.transcribe(remaining, language=language)
                response = StreamingChunk(
                    text=result["text"],
                    is_final=True
                )
                await websocket.send_text(response.model_dump_json())
                logger.info(f"Sent final: {result['text']}")
            except Exception as e:
                logger.error(f"Final transcription error: {e}")

    except Exception as e:
        logger.error(f"WebSocket error: {e}", exc_info=True)
        try:
            await websocket.close(code=1011, reason=str(e))
        except:
            pass
