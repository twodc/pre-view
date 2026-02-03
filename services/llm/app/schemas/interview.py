"""Interview agent schemas."""

from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class InterviewAction(str, Enum):
    """Interview agent action types."""

    FOLLOW_UP = "FOLLOW_UP"  # Ask follow-up question on current topic
    NEW_TOPIC = "NEW_TOPIC"  # Move to new topic
    NEXT_PHASE = "NEXT_PHASE"  # Complete current phase


class InterviewRequest(BaseModel):
    """Request schema for interview step processing."""

    system_prompt: str = Field(..., description="System instruction for interview agent")
    user_prompt: str = Field(..., description="User's answer or context")


class InterviewResponse(BaseModel):
    """Response schema for interview step."""

    thought: str = Field(..., description="AI's reasoning process")
    action: InterviewAction = Field(..., description="Next action to take")
    message: Optional[str] = Field(
        None,
        description="Next question or message (null if action is NEXT_PHASE)"
    )
    evaluation: Optional[str] = Field(
        None,
        description="Brief evaluation of current answer"
    )
