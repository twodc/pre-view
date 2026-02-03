"""Application configuration using Pydantic Settings."""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Model Configuration
    MODEL_NAME: str = "Qwen/Qwen3-32B-Instruct"
    DEVICE: str = "cuda"
    MAX_MODEL_LEN: int = 32768
    TENSOR_PARALLEL_SIZE: int = 1

    # SGLang Configuration
    SGLANG_HOST: str = "localhost"
    SGLANG_PORT: int = 30000
    SGLANG_API_KEY: str = "EMPTY"

    # Application Configuration
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000
    LOG_LEVEL: str = "INFO"

    # Request Configuration
    DEFAULT_TEMPERATURE: float = 0.7
    DEFAULT_MAX_TOKENS: int = 1000
    REQUEST_TIMEOUT: int = 300  # 5 minutes

    class Config:
        env_file = ".env"
        case_sensitive = True

    @property
    def sglang_base_url(self) -> str:
        """Get SGLang API base URL."""
        return f"http://{self.SGLANG_HOST}:{self.SGLANG_PORT}/v1"


# Global settings instance
settings = Settings()
