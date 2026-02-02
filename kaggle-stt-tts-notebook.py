# =============================================================================
# Pre-View STT + TTS Service (Kaggle Notebook)
# VibeVoice-ASR + Qwen3-TTS
# =============================================================================

# %% [markdown]
# # Pre-View 음성 서비스
# - **STT**: Microsoft VibeVoice-ASR (음성 → 텍스트)
# - **TTS**: Qwen3-TTS (텍스트 → 음성)

# %% Cell 1: 의존성 설치
!pip install -q gradio transformers accelerate torch soundfile scipy
!pip install -q qwen-tts bitsandbytes  # Qwen3-TTS + 양자화

# %% Cell 2: 라이브러리 임포트
import torch
import gradio as gr
import soundfile as sf
import tempfile
import numpy as np
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline, BitsAndBytesConfig

print(f"PyTorch version: {torch.__version__}")
print(f"CUDA available: {torch.cuda.is_available()}")
if torch.cuda.is_available():
    print(f"GPU: {torch.cuda.get_device_name(0)}")
    print(f"VRAM: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB")

# %% Cell 3: STT 모델 로드 (VibeVoice-ASR with INT8 양자화)
print("Loading VibeVoice-ASR model...")

# 양자화 설정 (메모리 절약)
quantization_config = BitsAndBytesConfig(
    load_in_8bit=True,  # INT8 양자화
)

try:
    # VibeVoice-ASR 로드 시도
    stt_model = AutoModelForSpeechSeq2Seq.from_pretrained(
        "microsoft/VibeVoice-ASR",
        quantization_config=quantization_config,
        device_map="auto",
        trust_remote_code=True,
    )
    stt_processor = AutoProcessor.from_pretrained(
        "microsoft/VibeVoice-ASR",
        trust_remote_code=True,
    )
    stt_pipe = pipeline(
        "automatic-speech-recognition",
        model=stt_model,
        tokenizer=stt_processor.tokenizer,
        feature_extractor=stt_processor.feature_extractor,
        device_map="auto",
    )
    STT_MODEL_NAME = "VibeVoice-ASR"
    print("✅ VibeVoice-ASR loaded successfully!")
except Exception as e:
    print(f"⚠️ VibeVoice-ASR failed: {e}")
    print("Falling back to Whisper-large-v3...")

    # Fallback: Whisper-large-v3
    stt_model = AutoModelForSpeechSeq2Seq.from_pretrained(
        "openai/whisper-large-v3",
        torch_dtype=torch.float16,
        low_cpu_mem_usage=True,
        use_safetensors=True,
    )
    stt_model.to("cuda")
    stt_processor = AutoProcessor.from_pretrained("openai/whisper-large-v3")
    stt_pipe = pipeline(
        "automatic-speech-recognition",
        model=stt_model,
        tokenizer=stt_processor.tokenizer,
        feature_extractor=stt_processor.feature_extractor,
        torch_dtype=torch.float16,
        device="cuda",
    )
    STT_MODEL_NAME = "Whisper-large-v3"
    print("✅ Whisper-large-v3 loaded successfully!")

# %% Cell 4: TTS 모델 로드 (Qwen3-TTS)
print("\nLoading Qwen3-TTS model...")

try:
    from qwen_tts import Qwen3TTSModel

    tts_model = Qwen3TTSModel.from_pretrained(
        "Qwen/Qwen3-TTS-12Hz-0.6B-Base",  # 경량 버전 (0.6B)
        device_map="cuda:0",
        dtype=torch.float16,
    )
    TTS_MODEL_NAME = "Qwen3-TTS-0.6B"
    print("✅ Qwen3-TTS loaded successfully!")
except Exception as e:
    print(f"⚠️ Qwen3-TTS failed: {e}")
    tts_model = None
    TTS_MODEL_NAME = "Not available"

# %% Cell 5: STT 함수 정의
def transcribe(audio_path, language="korean"):
    """음성을 텍스트로 변환"""
    if audio_path is None:
        return "오디오 파일을 업로드해주세요."

    try:
        lang_map = {"korean": "ko", "english": "en", "japanese": "ja", "chinese": "zh"}
        lang_code = lang_map.get(language.lower(), "ko")

        result = stt_pipe(
            audio_path,
            generate_kwargs={"language": lang_code},
            return_timestamps=False,
        )

        return result["text"]
    except Exception as e:
        return f"오류 발생: {str(e)}"

# %% Cell 6: TTS 함수 정의
def synthesize(text, language="Korean"):
    """텍스트를 음성으로 변환"""
    if not text or not text.strip():
        return None

    if tts_model is None:
        # Fallback: 더미 오디오 (사인파)
        sample_rate = 24000
        duration = 1.0
        t = np.linspace(0, duration, int(sample_rate * duration))
        audio = np.sin(2 * np.pi * 440 * t) * 0.3

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            sf.write(f.name, audio, sample_rate)
            return f.name

    try:
        wavs, sr = tts_model.generate_voice_clone(
            text=text,
            language=language,
            ref_audio=None,  # 기본 음성 사용
        )

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            sf.write(f.name, wavs[0], sr)
            return f.name
    except Exception as e:
        print(f"TTS error: {e}")
        return None

# %% Cell 7: Gradio UI 생성
with gr.Blocks(title="Pre-View 음성 서비스") as demo:
    gr.Markdown(f"""
    # 🎙️ Pre-View 음성 서비스

    | 기능 | 모델 |
    |------|------|
    | **STT** (음성→텍스트) | {STT_MODEL_NAME} |
    | **TTS** (텍스트→음성) | {TTS_MODEL_NAME} |
    """)

    with gr.Tab("🎤 STT (음성 인식)"):
        gr.Markdown("### 음성 파일을 업로드하면 텍스트로 변환합니다.")
        with gr.Row():
            with gr.Column():
                stt_audio = gr.Audio(type="filepath", label="오디오 파일")
                stt_language = gr.Dropdown(
                    choices=["korean", "english", "japanese", "chinese"],
                    value="korean",
                    label="언어"
                )
                stt_btn = gr.Button("변환하기", variant="primary")
            with gr.Column():
                stt_output = gr.Textbox(label="변환 결과", lines=5)

        stt_btn.click(transcribe, inputs=[stt_audio, stt_language], outputs=stt_output)

    with gr.Tab("🔊 TTS (음성 합성)"):
        gr.Markdown("### 텍스트를 입력하면 음성으로 변환합니다.")
        with gr.Row():
            with gr.Column():
                tts_text = gr.Textbox(
                    label="텍스트",
                    placeholder="안녕하세요, 면접을 시작하겠습니다.",
                    lines=3
                )
                tts_language = gr.Dropdown(
                    choices=["Korean", "English", "Chinese", "Japanese"],
                    value="Korean",
                    label="언어"
                )
                tts_btn = gr.Button("음성 생성", variant="primary")
            with gr.Column():
                tts_output = gr.Audio(label="생성된 음성")

        tts_btn.click(synthesize, inputs=[tts_text, tts_language], outputs=tts_output)

# %% Cell 8: 서비스 실행
print("\n" + "="*50)
print("🚀 Starting Pre-View Voice Service...")
print("="*50)

demo.launch(share=True)  # share=True로 공개 URL 생성
