"""Chat API schemas."""

from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class Message(BaseModel):
    """Single chat message."""

    role: str = Field(..., description="Role: system, user, or assistant")
    content: str = Field(..., description="Message content")


class ChatRequest(BaseModel):
    """Request schema for chat completion."""

    messages: List[Message] = Field(..., description="List of chat messages")
    temperature: Optional[float] = Field(
        None,
        ge=0.0,
        le=2.0,
        description="Sampling temperature"
    )
    max_tokens: Optional[int] = Field(
        None,
        ge=1,
        le=32768,
        description="Maximum tokens to generate"
    )


class TokenUsage(BaseModel):
    """Token usage information."""

    prompt_tokens: int = Field(..., description="Number of tokens in prompt")
    completion_tokens: int = Field(..., description="Number of tokens in completion")
    total_tokens: int = Field(..., description="Total tokens used")


class ChatResponse(BaseModel):
    """Response schema for chat completion."""

    content: str = Field(..., description="Generated content")
    usage: TokenUsage = Field(..., description="Token usage information")
