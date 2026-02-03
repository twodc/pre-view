"""Feedback generation API endpoint."""

import json
import logging
from fastapi import APIRouter, HTTPException

from app.core import LLMEngine
from app.core.prompt_builder import PromptBuilder
from app.schemas.feedback import FeedbackRequest, FeedbackResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/feedback", tags=["feedback"])


@router.post("", response_model=FeedbackResponse)
async def generate_feedback(request: FeedbackRequest) -> FeedbackResponse:
    """
    Generate structured feedback in JSON format.

    Args:
        request: Feedback request with system and user prompts

    Returns:
        Structured feedback with score, pass/fail, and suggestions
    """
    try:
        engine = LLMEngine()

        # Enhance system prompt with JSON instruction
        enhanced_system = PromptBuilder.add_json_instruction(request.system_prompt)

        # Build messages
        messages = PromptBuilder.build_messages(
            system_prompt=enhanced_system,
            user_prompt=request.user_prompt,
        )

        # Generate feedback with JSON mode enabled
        response = engine.chat_completion(
            messages=messages,
            json_mode=True,
        )

        # Parse JSON response
        content = engine.get_content(response)
        feedback_data = json.loads(content)

        logger.info(f"Feedback generated: score={feedback_data.get('score')}")

        return FeedbackResponse(**feedback_data)

    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in feedback response: {e}")
        raise HTTPException(status_code=500, detail="Invalid JSON response from LLM")
    except Exception as e:
        logger.error(f"Error generating feedback: {e}")
        raise HTTPException(status_code=500, detail=str(e))
