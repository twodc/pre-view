"""Pydantic schemas for request/response validation."""

from .chat import ChatRequest, ChatResponse
from .feedback import FeedbackRequest, FeedbackResponse
from .interview import InterviewRequest, InterviewResponse, InterviewAction
from .report import ReportRequest, ReportResponse, QuestionFeedback

__all__ = [
    "ChatRequest",
    "ChatResponse",
    "FeedbackRequest",
    "FeedbackResponse",
    "InterviewRequest",
    "InterviewResponse",
    "InterviewAction",
    "ReportRequest",
    "ReportResponse",
    "QuestionFeedback",
]
