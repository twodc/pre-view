"""Audio Preprocessing Module"""
import logging
import numpy as np
import librosa
from typing import Optional, Tuple
from io import BytesIO

from ..config import settings

logger = logging.getLogger(__name__)


class AudioProcessor:
    """오디오 전처리 프로세서"""

    def __init__(self, target_sr: int = settings.SAMPLE_RATE):
        self.target_sr = target_sr

    def load_audio(
        self,
        audio_bytes: bytes,
        original_sr: Optional[int] = None
    ) -> np.ndarray:
        """
        바이트 데이터에서 오디오 로드

        Args:
            audio_bytes: Raw audio bytes
            original_sr: Original sample rate (if known)

        Returns:
            Float32 audio array normalized to [-1, 1]
        """
        try:
            # Load with librosa
            audio, sr = librosa.load(
                BytesIO(audio_bytes),
                sr=self.target_sr,
                mono=True
            )

            logger.debug(f"Loaded audio: shape={audio.shape}, sr={sr}")
            return audio

        except Exception as e:
            logger.error(f"Failed to load audio: {e}")
            raise

    def load_audio_file(self, file_path: str) -> np.ndarray:
        """
        파일에서 오디오 로드

        Args:
            file_path: Path to audio file

        Returns:
            Float32 audio array
        """
        try:
            audio, sr = librosa.load(
                file_path,
                sr=self.target_sr,
                mono=True
            )

            logger.debug(f"Loaded audio file: {file_path}, shape={audio.shape}")
            return audio

        except Exception as e:
            logger.error(f"Failed to load audio file {file_path}: {e}")
            raise

    def process_pcm_chunk(
        self,
        pcm_bytes: bytes,
        dtype: str = "int16",
        channels: int = 1
    ) -> np.ndarray:
        """
        PCM 청크 처리 (WebSocket 스트리밍용)

        Args:
            pcm_bytes: Raw PCM bytes
            dtype: PCM data type (int16, float32)
            channels: Number of audio channels

        Returns:
            Float32 audio array normalized to [-1, 1]
        """
        try:
            # Convert bytes to numpy array
            if dtype == "int16":
                audio = np.frombuffer(pcm_bytes, dtype=np.int16)
                # Normalize to [-1, 1]
                audio = audio.astype(np.float32) / 32768.0
            elif dtype == "float32":
                audio = np.frombuffer(pcm_bytes, dtype=np.float32)
            else:
                raise ValueError(f"Unsupported dtype: {dtype}")

            # Handle multi-channel audio
            if channels > 1:
                audio = audio.reshape(-1, channels)
                # Convert to mono by averaging channels
                audio = audio.mean(axis=1)

            return audio

        except Exception as e:
            logger.error(f"Failed to process PCM chunk: {e}")
            raise

    def reduce_noise(self, audio: np.ndarray) -> np.ndarray:
        """
        간단한 노이즈 제거 (스펙트럴 게이팅)

        Args:
            audio: Input audio array

        Returns:
            Noise-reduced audio array
        """
        try:
            # Simple noise reduction using spectral gating
            # Compute STFT
            stft = librosa.stft(audio)
            magnitude, phase = librosa.magphase(stft)

            # Estimate noise floor (median of magnitude)
            noise_floor = np.median(magnitude, axis=1, keepdims=True)

            # Apply soft gate
            mask = magnitude > (noise_floor * 1.5)  # Threshold
            magnitude_clean = magnitude * mask

            # Reconstruct audio
            stft_clean = magnitude_clean * phase
            audio_clean = librosa.istft(stft_clean)

            return audio_clean

        except Exception as e:
            logger.warning(f"Noise reduction failed, returning original: {e}")
            return audio

    def normalize_audio(self, audio: np.ndarray) -> np.ndarray:
        """
        오디오 정규화

        Args:
            audio: Input audio array

        Returns:
            Normalized audio array
        """
        # Peak normalization
        max_val = np.abs(audio).max()
        if max_val > 0:
            audio = audio / max_val

        return audio

    def get_audio_duration(self, audio: np.ndarray) -> float:
        """
        오디오 길이 계산 (초)

        Args:
            audio: Audio array

        Returns:
            Duration in seconds
        """
        return len(audio) / self.target_sr

    def resample(self, audio: np.ndarray, orig_sr: int) -> np.ndarray:
        """
        오디오 리샘플링

        Args:
            audio: Input audio array
            orig_sr: Original sample rate

        Returns:
            Resampled audio array
        """
        if orig_sr == self.target_sr:
            return audio

        try:
            audio_resampled = librosa.resample(
                audio,
                orig_sr=orig_sr,
                target_sr=self.target_sr
            )
            return audio_resampled

        except Exception as e:
            logger.error(f"Resampling failed: {e}")
            raise
