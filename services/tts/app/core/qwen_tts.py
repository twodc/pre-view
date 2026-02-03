"""Qwen3-TTS Model Wrapper"""
import logging
from typing import Dict
import torch
import numpy as np
from transformers import AutoModelForCausalLM, AutoTokenizer
import scipy.io.wavfile as wavfile
from io import BytesIO

from app.config import settings

logger = logging.getLogger(__name__)


class QwenTTS:
    """Qwen3-TTS 모델 래퍼"""

    # 사용 가능한 음성 스타일 정의
    AVAILABLE_VOICES = {
        "female_calm": {
            "name": "차분한 여성",
            "gender": "female",
            "language": "ko",
            "description": "부드럽고 차분한 여성 목소리",
        },
        "female_bright": {
            "name": "밝은 여성",
            "gender": "female",
            "language": "ko",
            "description": "밝고 활기찬 여성 목소리",
        },
        "male_deep": {
            "name": "중저음 남성",
            "gender": "male",
            "language": "ko",
            "description": "낮고 안정적인 남성 목소리",
        },
        "male_friendly": {
            "name": "친근한 남성",
            "gender": "male",
            "language": "ko",
            "description": "따뜻하고 친근한 남성 목소리",
        },
        "neutral_professional": {
            "name": "전문적인 중성",
            "gender": "neutral",
            "language": "ko",
            "description": "전문적이고 명확한 중성 목소리",
        },
    }

    def __init__(self):
        """모델 초기화"""
        self.device = settings.DEVICE
        self.sample_rate = settings.SAMPLE_RATE
        self.model = None
        self.tokenizer = None
        self._initialized = False

        logger.info(f"TTS 서비스 초기화 중... (Device: {self.device})")

    def initialize(self) -> None:
        """모델 로드 (지연 초기화)"""
        if self._initialized:
            logger.info("모델이 이미 초기화되어 있습니다.")
            return

        try:
            logger.info(f"Qwen3-TTS 모델 로딩 중: {settings.MODEL_NAME}")

            # 토크나이저 로드
            self.tokenizer = AutoTokenizer.from_pretrained(
                settings.MODEL_NAME,
                cache_dir=settings.CACHE_DIR if settings.ENABLE_MODEL_CACHE else None,
                trust_remote_code=True,
            )

            # 모델 로드
            self.model = AutoModelForCausalLM.from_pretrained(
                settings.MODEL_NAME,
                cache_dir=settings.CACHE_DIR if settings.ENABLE_MODEL_CACHE else None,
                torch_dtype=torch.float16 if self.device == "cuda" else torch.float32,
                trust_remote_code=True,
            )

            self.model.to(self.device)
            self.model.eval()

            self._initialized = True
            logger.info("Qwen3-TTS 모델 로딩 완료")

        except Exception as e:
            logger.error(f"모델 로딩 실패: {str(e)}")
            raise RuntimeError(f"TTS 모델 초기화 실패: {str(e)}")

    def synthesize(
        self,
        text: str,
        voice: str = "female_calm",
        speed: float = 1.0,
    ) -> np.ndarray:
        """
        텍스트를 음성으로 변환

        Args:
            text: 변환할 텍스트
            voice: 사용할 음성 스타일
            speed: 음성 속도 (0.5 ~ 2.0)

        Returns:
            numpy.ndarray: 오디오 데이터 (16-bit PCM)
        """
        if not self._initialized:
            self.initialize()

        if voice not in self.AVAILABLE_VOICES:
            logger.warning(f"알 수 없는 음성: {voice}, 기본값 사용")
            voice = settings.DEFAULT_VOICE

        try:
            logger.info(f"음성 합성 시작 - Voice: {voice}, Speed: {speed}")

            # 텍스트 전처리
            text = self._preprocess_text(text)

            # 프롬프트 생성 (음성 스타일에 따라)
            prompt = self._create_prompt(text, voice)

            # 토큰화
            inputs = self.tokenizer(
                prompt,
                return_tensors="pt",
                padding=True,
                truncation=True,
                max_length=settings.MAX_TEXT_LENGTH,
            ).to(self.device)

            # 음성 생성
            with torch.no_grad():
                # TTS 모델 추론
                outputs = self.model.generate(
                    **inputs,
                    max_length=512,
                    do_sample=True,
                    temperature=0.7,
                    top_p=0.9,
                )

                # 오디오 데이터 추출 (모델 출력에 따라 조정 필요)
                # 실제 Qwen3-TTS API에 맞게 수정 필요
                audio_data = self._extract_audio(outputs)

            # 속도 조정
            if speed != 1.0:
                audio_data = self._adjust_speed(audio_data, speed)

            logger.info(f"음성 합성 완료 - 길이: {len(audio_data) / self.sample_rate:.2f}초")
            return audio_data

        except Exception as e:
            logger.error(f"음성 합성 실패: {str(e)}")
            raise RuntimeError(f"TTS 변환 실패: {str(e)}")

    def _preprocess_text(self, text: str) -> str:
        """텍스트 전처리"""
        # 공백 정리
        text = " ".join(text.split())
        # 특수 문자 처리 등 추가 가능
        return text

    def _create_prompt(self, text: str, voice: str) -> str:
        """음성 스타일에 맞는 프롬프트 생성"""
        voice_info = self.AVAILABLE_VOICES[voice]

        # 음성 특성을 반영한 프롬프트
        prompt = f"[TTS] {voice_info['description']}: {text}"
        return prompt

    def _extract_audio(self, outputs: torch.Tensor) -> np.ndarray:
        """
        모델 출력에서 오디오 데이터 추출

        주의: 실제 Qwen3-TTS API에 맞게 수정 필요
        현재는 placeholder 구현
        """
        # TODO: 실제 Qwen3-TTS 출력 형식에 맞게 수정
        # 현재는 더미 오디오 생성 (1초, 440Hz 사인파)
        duration = 1.0
        t = np.linspace(0, duration, int(self.sample_rate * duration))
        audio_data = np.sin(2 * np.pi * 440 * t) * 0.3

        # 16-bit PCM으로 변환
        audio_data = (audio_data * 32767).astype(np.int16)
        return audio_data

    def _adjust_speed(self, audio_data: np.ndarray, speed: float) -> np.ndarray:
        """오디오 속도 조정 (리샘플링)"""
        try:
            from scipy import signal

            # 리샘플링을 통한 속도 조정
            num_samples = int(len(audio_data) / speed)
            adjusted_audio = signal.resample(audio_data, num_samples)
            return adjusted_audio.astype(np.int16)

        except Exception as e:
            logger.warning(f"속도 조정 실패, 원본 반환: {str(e)}")
            return audio_data

    def get_available_voices(self) -> Dict[str, dict]:
        """사용 가능한 음성 목록 반환"""
        return self.AVAILABLE_VOICES

    def to_wav_bytes(self, audio_data: np.ndarray) -> BytesIO:
        """오디오 데이터를 WAV 바이트로 변환"""
        wav_io = BytesIO()
        wavfile.write(wav_io, self.sample_rate, audio_data)
        wav_io.seek(0)
        return wav_io


# 전역 인스턴스 (싱글톤)
_tts_instance = None


def get_tts_instance() -> QwenTTS:
    """TTS 인스턴스 가져오기 (싱글톤 패턴)"""
    global _tts_instance
    if _tts_instance is None:
        _tts_instance = QwenTTS()
    return _tts_instance
