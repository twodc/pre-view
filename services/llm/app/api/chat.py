"""Chat completion API endpoint."""

import logging
from fastapi import APIRouter, HTTPException

from app.core import LLMEngine
from app.schemas.chat import ChatRequest, ChatResponse, TokenUsage

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/chat", tags=["chat"])


@router.post("/completions", response_model=ChatResponse)
async def chat_completion(request: ChatRequest) -> ChatResponse:
    """
    Generate chat completion using SGLang.

    OpenAI API compatible format.

    Args:
        request: Chat completion request with messages

    Returns:
        Generated content and token usage
    """
    try:
        engine = LLMEngine()

        # Convert Pydantic models to dicts
        messages = [msg.model_dump() for msg in request.messages]

        # Generate completion
        response = engine.chat_completion(
            messages=messages,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            json_mode=False,
        )

        # Extract content and usage
        content = engine.get_content(response)
        usage_dict = engine.get_usage(response)

        logger.info(
            f"Chat completion generated: {usage_dict['total_tokens']} tokens"
        )

        return ChatResponse(
            content=content,
            usage=TokenUsage(**usage_dict),
        )

    except Exception as e:
        logger.error(f"Error in chat completion: {e}")
        raise HTTPException(status_code=500, detail=str(e))
