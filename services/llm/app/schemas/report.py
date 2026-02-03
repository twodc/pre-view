"""Report generation schemas."""

from typing import List, Optional
from pydantic import BaseModel, Field


class QuestionFeedback(BaseModel):
    """Feedback for a single question."""

    question: str = Field(..., description="The question asked")
    answer_summary: str = Field(..., description="Summary of the answer")
    score: int = Field(..., ge=0, le=10, description="Score for this question")
    feedback: str = Field(..., description="Detailed feedback")


class ReportRequest(BaseModel):
    """Request schema for report generation."""

    system_prompt: str = Field(..., description="System instruction for report")
    user_prompt: str = Field(..., description="Interview data to analyze")


class ReportResponse(BaseModel):
    """Response schema for interview report."""

    summary: str = Field(..., description="Overall summary of performance")
    strengths: List[str] = Field(..., description="List of strengths identified")
    improvements: List[str] = Field(..., description="List of areas for improvement")
    recommendations: List[str] = Field(..., description="Recommendations for candidates")
    overall_score: int = Field(
        ...,
        ge=0,
        le=100,
        description="Overall score (0-100)"
    )
    question_feedbacks: Optional[List[QuestionFeedback]] = Field(
        None,
        description="Per-question feedback"
    )
