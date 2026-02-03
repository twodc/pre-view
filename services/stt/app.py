"""
VibeVoice-ASR HuggingFace Spaces App
Microsoft VibeVoice-ASR for speech-to-text
"""
import gradio as gr
import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline
import numpy as np

# Model configuration
MODEL_ID = "microsoft/VibeVoice-ASR"
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
TORCH_DTYPE = torch.float16 if DEVICE == "cuda" else torch.float32

print(f"Loading VibeVoice-ASR on {DEVICE}...")

# Load model and processor
model = AutoModelForSpeechSeq2Seq.from_pretrained(
    MODEL_ID,
    torch_dtype=TORCH_DTYPE,
    low_cpu_mem_usage=True,
    use_safetensors=True,
)
model.to(DEVICE)

processor = AutoProcessor.from_pretrained(MODEL_ID)

# Create pipeline
pipe = pipeline(
    "automatic-speech-recognition",
    model=model,
    tokenizer=processor.tokenizer,
    feature_extractor=processor.feature_extractor,
    torch_dtype=TORCH_DTYPE,
    device=DEVICE,
)

print("Model loaded successfully!")


def transcribe(audio_path: str, language: str = "korean") -> dict:
    """
    Transcribe audio file to text

    Args:
        audio_path: Path to audio file
        language: Target language (korean, english, etc.)

    Returns:
        Transcription result with text and metadata
    """
    if audio_path is None:
        return {"error": "No audio provided"}

    try:
        # Language mapping
        lang_map = {
            "korean": "ko",
            "english": "en",
            "japanese": "ja",
            "chinese": "zh",
        }
        lang_code = lang_map.get(language.lower(), "ko")

        # Transcribe
        result = pipe(
            audio_path,
            generate_kwargs={"language": lang_code},
            return_timestamps=True,
        )

        return {
            "text": result["text"],
            "chunks": result.get("chunks", []),
            "language": lang_code,
        }

    except Exception as e:
        return {"error": str(e)}


# Gradio Interface
demo = gr.Interface(
    fn=transcribe,
    inputs=[
        gr.Audio(type="filepath", label="Audio Input"),
        gr.Dropdown(
            choices=["korean", "english", "japanese", "chinese"],
            value="korean",
            label="Language"
        ),
    ],
    outputs=gr.JSON(label="Transcription Result"),
    title="VibeVoice-ASR Speech-to-Text",
    description="Microsoft VibeVoice-ASR for high-quality speech recognition. Supports 50+ languages.",
    examples=[],
    cache_examples=False,
)

# API endpoint for programmatic access
app = gr.mount_gradio_app(demo, path="/")

if __name__ == "__main__":
    demo.launch(server_name="0.0.0.0", server_port=7860)
