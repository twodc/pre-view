"""Prompt builder utilities for constructing messages."""

from typing import Dict, List, Optional


class PromptBuilder:
    """Helper class for building chat messages."""

    @staticmethod
    def build_messages(
        system_prompt: str,
        user_prompt: str,
        assistant_prefix: Optional[str] = None
    ) -> List[Dict[str, str]]:
        """
        Build chat messages from system and user prompts.

        Args:
            system_prompt: System instruction
            user_prompt: User query
            assistant_prefix: Optional prefix to start assistant response

        Returns:
            List of message dicts
        """
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

        if assistant_prefix:
            messages.append({"role": "assistant", "content": assistant_prefix})

        return messages

    @staticmethod
    def add_json_instruction(system_prompt: str) -> str:
        """
        Add JSON format instruction to system prompt.

        Args:
            system_prompt: Original system prompt

        Returns:
            Enhanced system prompt with JSON instruction
        """
        json_instruction = (
            "\n\nIMPORTANT: You must respond with a valid JSON object. "
            "Do not include any text outside the JSON structure."
        )
        return system_prompt + json_instruction
