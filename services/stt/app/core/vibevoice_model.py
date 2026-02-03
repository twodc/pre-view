"""Whisper ASR Model Wrapper"""
import logging
import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline
from typing import Optional, Dict, Any
import numpy as np

from ..config import settings

logger = logging.getLogger(__name__)


class WhisperModel:
    """OpenAI Whisper 모델 래퍼"""

    def __init__(self):
        self.model_name = settings.MODEL_NAME
        self.device = settings.DEVICE
        self.model = None
        self.processor = None
        self.pipe = None

    def load_model(self):
        """모델 로드 및 초기화"""
        try:
            logger.info(f"Loading Whisper model: {self.model_name} on {self.device}")

            # Model configuration
            torch_dtype = torch.float16 if self.device == "cuda" else torch.float32

            # Load model
            self.model = AutoModelForSpeechSeq2Seq.from_pretrained(
                self.model_name,
                torch_dtype=torch_dtype,
                low_cpu_mem_usage=True,
                use_safetensors=True
            )
            self.model.to(self.device)

            # Load processor
            self.processor = AutoProcessor.from_pretrained(self.model_name)

            # Create pipeline
            self.pipe = pipeline(
                "automatic-speech-recognition",
                model=self.model,
                tokenizer=self.processor.tokenizer,
                feature_extractor=self.processor.feature_extractor,
                torch_dtype=torch_dtype,
                device=self.device,
            )

            logger.info("Whisper model loaded successfully")

        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise

    def transcribe(
        self,
        audio: np.ndarray,
        language: Optional[str] = None,
        **kwargs
    ) -> Dict[str, Any]:
        """
        오디오 전사

        Args:
            audio: Audio array (float32, 16kHz)
            language: Language code (e.g., 'ko', 'en')
            **kwargs: Additional pipeline arguments

        Returns:
            Dict containing 'text' and optional metadata
        """
        if self.pipe is None:
            raise RuntimeError("Model not loaded. Call load_model() first.")

        try:
            # Prepare generation kwargs
            generate_kwargs = {}
            if language:
                generate_kwargs["language"] = language

            # Transcribe
            result = self.pipe(
                audio,
                generate_kwargs=generate_kwargs,
                return_timestamps=False,
                **kwargs
            )

            return {
                "text": result["text"].strip(),
                "language": language
            }

        except Exception as e:
            logger.error(f"Transcription failed: {e}")
            raise

    def is_loaded(self) -> bool:
        """모델 로드 여부 확인"""
        return self.pipe is not None

    def unload_model(self):
        """모델 언로드 (메모리 정리)"""
        if self.model is not None:
            del self.model
            del self.processor
            del self.pipe
            self.model = None
            self.processor = None
            self.pipe = None

            if self.device == "cuda":
                torch.cuda.empty_cache()

            logger.info("Model unloaded")


# Global model instance
_model_instance: Optional[WhisperModel] = None


def get_model() -> WhisperModel:
    """싱글톤 모델 인스턴스 반환"""
    global _model_instance

    if _model_instance is None:
        _model_instance = WhisperModel()
        _model_instance.load_model()

    return _model_instance
