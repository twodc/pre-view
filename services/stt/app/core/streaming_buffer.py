"""Streaming Audio Buffer"""
import logging
import numpy as np
from typing import Optional, List
from collections import deque

from ..config import settings

logger = logging.getLogger(__name__)


class StreamingBuffer:
    """스트리밍 오디오 버퍼 관리"""

    def __init__(
        self,
        chunk_duration: float = settings.CHUNK_DURATION,
        sample_rate: int = settings.SAMPLE_RATE,
        overlap_duration: float = 0.5  # Overlap for context
    ):
        """
        Args:
            chunk_duration: Duration of each chunk in seconds
            sample_rate: Audio sample rate
            overlap_duration: Overlap between chunks in seconds
        """
        self.chunk_duration = chunk_duration
        self.sample_rate = sample_rate
        self.overlap_duration = overlap_duration

        # Calculate sizes
        self.chunk_size = int(chunk_duration * sample_rate)
        self.overlap_size = int(overlap_duration * sample_rate)

        # Buffer
        self.buffer: deque = deque()
        self.total_samples = 0

        logger.info(
            f"StreamingBuffer initialized: chunk_size={self.chunk_size}, "
            f"overlap_size={self.overlap_size}"
        )

    def add_audio(self, audio_chunk: np.ndarray):
        """
        오디오 청크 추가

        Args:
            audio_chunk: Audio samples to add
        """
        self.buffer.extend(audio_chunk.tolist())
        self.total_samples += len(audio_chunk)

    def has_complete_chunk(self) -> bool:
        """완전한 청크가 버퍼에 있는지 확인"""
        return len(self.buffer) >= self.chunk_size

    def get_chunk(self, keep_overlap: bool = True) -> Optional[np.ndarray]:
        """
        버퍼에서 청크 추출

        Args:
            keep_overlap: Whether to keep overlap samples in buffer

        Returns:
            Audio chunk as numpy array, or None if not enough samples
        """
        if not self.has_complete_chunk():
            return None

        # Extract chunk
        chunk_list = []
        for _ in range(self.chunk_size):
            if self.buffer:
                chunk_list.append(self.buffer.popleft())

        chunk = np.array(chunk_list, dtype=np.float32)

        # Re-add overlap if requested
        if keep_overlap and self.overlap_size > 0:
            overlap = chunk[-self.overlap_size:]
            self.buffer.extendleft(reversed(overlap.tolist()))

        return chunk

    def get_remaining(self) -> Optional[np.ndarray]:
        """
        버퍼에 남은 모든 샘플 추출 (스트림 종료 시)

        Returns:
            Remaining audio samples, or None if buffer is empty
        """
        if len(self.buffer) == 0:
            return None

        remaining = np.array(list(self.buffer), dtype=np.float32)
        self.buffer.clear()

        return remaining

    def clear(self):
        """버퍼 초기화"""
        self.buffer.clear()
        self.total_samples = 0

    def get_buffer_duration(self) -> float:
        """현재 버퍼의 길이 (초)"""
        return len(self.buffer) / self.sample_rate

    def get_total_duration(self) -> float:
        """처리된 전체 오디오 길이 (초)"""
        return self.total_samples / self.sample_rate

    def __len__(self) -> int:
        """버퍼에 있는 샘플 수"""
        return len(self.buffer)

    def __repr__(self) -> str:
        return (
            f"StreamingBuffer(samples={len(self.buffer)}, "
            f"duration={self.get_buffer_duration():.2f}s)"
        )
