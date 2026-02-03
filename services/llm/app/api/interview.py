"""Interview agent API endpoint."""

import json
import logging
from fastapi import APIRouter, HTTPException

from app.core import LLMEngine
from app.core.prompt_builder import PromptBuilder
from app.schemas.interview import InterviewRequest, InterviewResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/interview", tags=["interview"])


@router.post("/step", response_model=InterviewResponse)
async def process_interview_step(request: InterviewRequest) -> InterviewResponse:
    """
    Process a single interview step with agent decision-making.

    The agent analyzes the user's response and decides:
    - FOLLOW_UP: Ask deeper question on current topic
    - NEW_TOPIC: Move to a different topic
    - NEXT_PHASE: Complete current interview phase

    Args:
        request: Interview request with system and user prompts

    Returns:
        Agent's thought process, action, and next message
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

        # Generate interview step response with JSON mode
        response = engine.chat_completion(
            messages=messages,
            json_mode=True,
        )

        # Parse JSON response
        content = engine.get_content(response)
        interview_data = json.loads(content)

        logger.info(
            f"Interview step processed: action={interview_data.get('action')}"
        )

        return InterviewResponse(**interview_data)

    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in interview response: {e}")
        raise HTTPException(status_code=500, detail="Invalid JSON response from LLM")
    except Exception as e:
        logger.error(f"Error processing interview step: {e}")
        raise HTTPException(status_code=500, detail=str(e))
