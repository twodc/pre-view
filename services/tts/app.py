"""
Qwen3-TTS HuggingFace Spaces App
Alibaba Qwen3-TTS for text-to-speech
"""
import gradio as gr
import torch
import soundfile as sf
import tempfile
import os

# Check if qwen-tts is available
try:
    from qwen_tts import Qwen3TTSModel
    QWEN_TTS_AVAILABLE = True
except ImportError:
    QWEN_TTS_AVAILABLE = False
    print("qwen-tts not installed, using fallback")

# Model configuration
MODEL_ID = "Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice"
DEVICE = "cuda:0" if torch.cuda.is_available() else "cpu"

# Available voices (CustomVoice model)
AVAILABLE_VOICES = {
    "Vivian": "차분한 여성 (영어)",
    "Aria": "밝은 여성 (영어)",
    "Nova": "전문적인 여성 (영어)",
    "David": "중저음 남성 (영어)",
    "Lucas": "친근한 남성 (영어)",
}

# Available languages
AVAILABLE_LANGUAGES = [
    "Korean", "English", "Chinese", "Japanese",
    "German", "French", "Spanish", "Italian",
    "Portuguese", "Russian"
]

model = None

def load_model():
    """Load Qwen3-TTS model"""
    global model
    if model is None and QWEN_TTS_AVAILABLE:
        print(f"Loading Qwen3-TTS on {DEVICE}...")
        model = Qwen3TTSModel.from_pretrained(
            MODEL_ID,
            device_map=DEVICE,
            dtype=torch.bfloat16 if DEVICE.startswith("cuda") else torch.float32,
        )
        print("Model loaded successfully!")
    return model


def synthesize(text: str, language: str, speaker: str) -> str:
    """
    Synthesize text to speech

    Args:
        text: Text to convert to speech
        language: Target language
        speaker: Voice to use

    Returns:
        Path to generated audio file
    """
    if not text or not text.strip():
        return None

    try:
        model = load_model()

        if model is None:
            # Fallback: generate silence (for testing without model)
            import numpy as np
            sample_rate = 24000
            duration = 1.0
            samples = np.zeros(int(sample_rate * duration), dtype=np.float32)

            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
                sf.write(f.name, samples, sample_rate)
                return f.name

        # Generate speech using CustomVoice
        wavs, sr = model.generate_custom_voice(
            text=text,
            language=language,
            speaker=speaker,
        )

        # Save to temp file
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            sf.write(f.name, wavs[0], sr)
            return f.name

    except Exception as e:
        print(f"Synthesis error: {e}")
        return None


# Gradio Interface
demo = gr.Interface(
    fn=synthesize,
    inputs=[
        gr.Textbox(
            label="Text",
            placeholder="Enter text to convert to speech...",
            lines=3,
        ),
        gr.Dropdown(
            choices=AVAILABLE_LANGUAGES,
            value="Korean",
            label="Language"
        ),
        gr.Dropdown(
            choices=list(AVAILABLE_VOICES.keys()),
            value="Vivian",
            label="Voice"
        ),
    ],
    outputs=gr.Audio(label="Generated Speech", type="filepath"),
    title="Qwen3-TTS Text-to-Speech",
    description="Alibaba Qwen3-TTS for high-quality multilingual speech synthesis. Supports 10 languages including Korean.",
    examples=[
        ["안녕하세요, 면접을 시작하겠습니다.", "Korean", "Vivian"],
        ["자기소개를 해주세요.", "Korean", "Vivian"],
        ["Hello, let's begin the interview.", "English", "David"],
    ],
    cache_examples=False,
)

if __name__ == "__main__":
    demo.launch(server_name="0.0.0.0", server_port=7860)
