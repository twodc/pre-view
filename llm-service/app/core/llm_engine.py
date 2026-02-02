"""SGLang-based LLM engine with singleton pattern."""

import logging
from typing import Any, Dict, List, Optional
from openai import OpenAI
from openai.types.chat import ChatCompletion

from app.config import settings

logger = logging.getLogger(__name__)


class LLMEngine:
    """Singleton LLM engine using SGLang OpenAI-compatible API."""

    _instance: Optional["LLMEngine"] = None
    _client: Optional[OpenAI] = None

    def __new__(cls) -> "LLMEngine":
        """Ensure only one instance exists (singleton pattern)."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        """Initialize OpenAI client connected to SGLang server."""
        if self._client is None:
            logger.info(
                f"Initializing LLM Engine with SGLang at {settings.sglang_base_url}"
            )
            self._client = OpenAI(
                base_url=settings.sglang_base_url,
                api_key=settings.SGLANG_API_KEY,
                timeout=settings.REQUEST_TIMEOUT,
            )
            logger.info("LLM Engine initialized successfully")

    def chat_completion(
        self,
        messages: List[Dict[str, str]],
        temperature: float = None,
        max_tokens: int = None,
        json_mode: bool = False,
    ) -> ChatCompletion:
        """
        Generate chat completion using SGLang.

        Args:
            messages: List of message dicts with 'role' and 'content'
            temperature: Sampling temperature (0.0 to 2.0)
            max_tokens: Maximum tokens to generate
            json_mode: Whether to enforce JSON response format

        Returns:
            ChatCompletion object from OpenAI SDK
        """
        if self._client is None:
            raise RuntimeError("LLM Engine not initialized")

        # Use default values from settings if not provided
        temperature = temperature if temperature is not None else settings.DEFAULT_TEMPERATURE
        max_tokens = max_tokens if max_tokens is not None else settings.DEFAULT_MAX_TOKENS

        try:
            kwargs: Dict[str, Any] = {
                "model": "default",  # SGLang uses "default" as model name
                "messages": messages,
                "temperature": temperature,
                "max_tokens": max_tokens,
            }

            # Enable JSON mode if requested
            if json_mode:
                kwargs["response_format"] = {"type": "json_object"}

            logger.debug(f"Sending chat completion request: {len(messages)} messages")
            response = self._client.chat.completions.create(**kwargs)
            logger.debug(f"Received response: {response.usage}")

            return response

        except Exception as e:
            logger.error(f"Error during chat completion: {e}")
            raise

    def get_content(self, response: ChatCompletion) -> str:
        """
        Extract text content from chat completion response.

        Args:
            response: ChatCompletion response from OpenAI SDK

        Returns:
            Generated text content
        """
        if not response.choices:
            raise ValueError("No choices in response")

        return response.choices[0].message.content or ""

    def get_usage(self, response: ChatCompletion) -> Dict[str, int]:
        """
        Extract token usage information from response.

        Args:
            response: ChatCompletion response from OpenAI SDK

        Returns:
            Dictionary with prompt_tokens, completion_tokens, total_tokens
        """
        if response.usage is None:
            return {
                "prompt_tokens": 0,
                "completion_tokens": 0,
                "total_tokens": 0,
            }

        return {
            "prompt_tokens": response.usage.prompt_tokens,
            "completion_tokens": response.usage.completion_tokens,
            "total_tokens": response.usage.total_tokens,
        }


# Global engine instance
llm_engine = LLMEngine()
