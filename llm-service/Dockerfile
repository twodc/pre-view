# Multi-stage Dockerfile for LLM Service
# Stage 1: Base image with CUDA support
FROM nvidia/cuda:12.1.0-runtime-ubuntu22.04 AS base

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

# Install Python 3.11 and system dependencies
RUN apt-get update && apt-get install -y \
    python3.11 \
    python3.11-dev \
    python3-pip \
    git \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create symbolic link for python
RUN ln -sf /usr/bin/python3.11 /usr/bin/python

# Upgrade pip
RUN python -m pip install --upgrade pip

# Stage 2: Dependencies installation
FROM base AS dependencies

# Set working directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Stage 3: Application
FROM dependencies AS application

# Copy application code
COPY app/ ./app/

# Create directory for models (can be mounted as volume)
RUN mkdir -p /models

# Expose ports
# 8000: FastAPI application
# 30000: SGLang server
EXPOSE 8000 30000

# Environment variables
ENV SGLANG_HOST=localhost
ENV SGLANG_PORT=30000
ENV APP_HOST=0.0.0.0
ENV APP_PORT=8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Default command: Run FastAPI application
# Note: SGLang server should be started separately (see docker-compose.yml)
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
