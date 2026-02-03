"""Audio Encoding Utilities"""
import logging
import base64
from io import BytesIO
from typing import Literal
import numpy as np
import scipy.io.wavfile as wavfile

logger = logging.getLogger(__name__)


class AudioEncoder:
    """오디오 인코딩 유틸리티"""

    @staticmethod
    def encode_to_base64(
        audio_data: np.ndarray,
        sample_rate: int,
        format: Literal["wav", "mp3"] = "wav",
    ) -> str:
        """
        오디오 데이터를 Base64로 인코딩

        Args:
            audio_data: 오디오 샘플 데이터
            sample_rate: 샘플링 레이트
            format: 출력 포맷 (wav 또는 mp3)

        Returns:
            Base64 인코딩된 문자열
        """
        try:
            if format == "wav":
                audio_bytes = AudioEncoder._to_wav_bytes(audio_data, sample_rate)
            elif format == "mp3":
                audio_bytes = AudioEncoder._to_mp3_bytes(audio_data, sample_rate)
            else:
                raise ValueError(f"지원하지 않는 포맷: {format}")

            # Base64 인코딩
            encoded = base64.b64encode(audio_bytes.getvalue()).decode("utf-8")
            logger.info(f"{format.upper()} 인코딩 완료 - 크기: {len(encoded)} bytes")
            return encoded

        except Exception as e:
            logger.error(f"오디오 인코딩 실패: {str(e)}")
            raise

    @staticmethod
    def _to_wav_bytes(audio_data: np.ndarray, sample_rate: int) -> BytesIO:
        """오디오 데이터를 WAV 바이트로 변환"""
        wav_io = BytesIO()
        wavfile.write(wav_io, sample_rate, audio_data)
        wav_io.seek(0)
        return wav_io

    @staticmethod
    def _to_mp3_bytes(audio_data: np.ndarray, sample_rate: int) -> BytesIO:
        """오디오 데이터를 MP3 바이트로 변환 (pydub 사용)"""
        try:
            from pydub import AudioSegment

            # WAV로 먼저 변환
            wav_io = AudioEncoder._to_wav_bytes(audio_data, sample_rate)

            # AudioSegment로 로드
            audio_segment = AudioSegment.from_wav(wav_io)

            # MP3로 변환
            mp3_io = BytesIO()
            audio_segment.export(
                mp3_io,
                format="mp3",
                bitrate="192k",
                parameters=["-q:a", "0"],  # 최고 품질
            )
            mp3_io.seek(0)

            return mp3_io

        except ImportError:
            logger.error("pydub가 설치되지 않았습니다. WAV 포맷을 사용하세요.")
            raise
        except Exception as e:
            logger.error(f"MP3 변환 실패: {str(e)}")
            raise

    @staticmethod
    def get_audio_duration(audio_data: np.ndarray, sample_rate: int) -> float:
        """오디오 길이 계산 (초)"""
        return len(audio_data) / sample_rate

    @staticmethod
    def save_to_file(
        audio_data: np.ndarray,
        sample_rate: int,
        output_path: str,
        format: Literal["wav", "mp3"] = "wav",
    ) -> None:
        """
        오디오 데이터를 파일로 저장

        Args:
            audio_data: 오디오 샘플 데이터
            sample_rate: 샘플링 레이트
            output_path: 출력 파일 경로
            format: 출력 포맷
        """
        try:
            if format == "wav":
                wavfile.write(output_path, sample_rate, audio_data)
            elif format == "mp3":
                from pydub import AudioSegment

                wav_io = AudioEncoder._to_wav_bytes(audio_data, sample_rate)
                audio_segment = AudioSegment.from_wav(wav_io)
                audio_segment.export(output_path, format="mp3", bitrate="192k")
            else:
                raise ValueError(f"지원하지 않는 포맷: {format}")

            logger.info(f"오디오 파일 저장 완료: {output_path}")

        except Exception as e:
            logger.error(f"파일 저장 실패: {str(e)}")
            raise
