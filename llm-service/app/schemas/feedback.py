"""Feedback generation schemas."""

from typing import Optional
from pydantic import BaseModel, Field


class FeedbackRequest(BaseModel):
    """Request schema for feedback generation."""

    system_prompt: str = Field(..., description="System instruction for feedback")
    user_prompt: str = Field(..., description="Content to generate feedback for")


class FeedbackResponse(BaseModel):
    """Response schema for feedback generation."""

    feedback: str = Field(..., description="Detailed feedback content")
    score: int = Field(..., ge=0, le=10, description="Numerical score (0-10)")
    is_passed: bool = Field(..., description="Whether the answer passed the criteria")
    improvement_suggestion: Optional[str] = Field(
        None,
        description="Specific suggestions for improvement"
    )
