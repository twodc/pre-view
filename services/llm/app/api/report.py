"""Report generation API endpoint."""

import json
import logging
from fastapi import APIRouter, HTTPException

from app.core import LLMEngine
from app.core.prompt_builder import PromptBuilder
from app.schemas.report import ReportRequest, ReportResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/report", tags=["report"])


@router.post("", response_model=ReportResponse)
async def generate_report(request: ReportRequest) -> ReportResponse:
    """
    Generate comprehensive interview report.

    Analyzes interview data and produces:
    - Overall summary and score
    - Strengths and improvement areas
    - Recommendations
    - Per-question feedback (optional)

    Args:
        request: Report request with system and user prompts

    Returns:
        Structured interview report
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

        # Generate report with JSON mode enabled
        # Use higher max_tokens for comprehensive reports
        response = engine.chat_completion(
            messages=messages,
            json_mode=True,
            max_tokens=2000,
        )

        # Parse JSON response
        content = engine.get_content(response)
        report_data = json.loads(content)

        logger.info(
            f"Report generated: overall_score={report_data.get('overall_score')}"
        )

        return ReportResponse(**report_data)

    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in report response: {e}")
        raise HTTPException(status_code=500, detail="Invalid JSON response from LLM")
    except Exception as e:
        logger.error(f"Error generating report: {e}")
        raise HTTPException(status_code=500, detail=str(e))
