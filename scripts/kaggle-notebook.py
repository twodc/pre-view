# =============================================================================
# Pre-View STT + TTS Service (Kaggle Notebook)
# VibeVoice-ASR + Qwen3-TTS
# =============================================================================

# %% [markdown]
# # Pre-View 음성 서비스
# - **STT**: Microsoft VibeVoice-ASR (음성 -> 텍스트)
# - **TTS**: Qwen3-TTS (텍스트 -> 음성)
#
# ## 사용 방법
# 1. Kaggle에서 이 노트북 실행
# 2. 생성된 Gradio URL 복사
# 3. Pre-View 관리자 페이지에서 URL 입력
# 4. 면접 시 음성 기능 사용 가능
#
# ## 세션 유지
# - Keep-alive 코드가 포함되어 있어 브라우저 탭만 열어두면 최대 12시간 유지
# - 탭을 닫으면 30분~1시간 후 종료될 수 있음

# %% Cell 0: Keep-alive (세션 유지) - 가장 먼저 실행
import threading
import time
from datetime import datetime

def keep_session_alive():
    """5분마다 출력을 발생시켜 Kaggle 세션 유지"""
    count = 0
    while True:
        time.sleep(300)  # 5분
        count += 1
        current_time = datetime.now().strftime("%H:%M:%S")
        print(f"[Keep-alive] {current_time} - {count * 5}분 경과 - 세션 유지 중...")

# 백그라운드 데몬 스레드로 실행 (메인 프로세스 종료 시 같이 종료)
keep_alive_thread = threading.Thread(target=keep_session_alive, daemon=True)
keep_alive_thread.start()
print(f"[Keep-alive] 시작됨 - 5분마다 세션 유지 체크")
print(f"[Keep-alive] 브라우저 탭을 열어두면 최대 12시간 유지됩니다.")
print()

# %% Cell 1: 의존성 설치
!pip install -q gradio transformers accelerate torch soundfile scipy
!pip install -q bitsandbytes  # 양자화
!pip install -q pydub  # 오디오 형식 변환
!apt-get install -qq ffmpeg  # pydub 백엔드

# %% Cell 2: 라이브러리 임포트
import torch
import gradio as gr
import soundfile as sf
import tempfile
import numpy as np
import base64
import io
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
    print("VibeVoice-ASR loaded successfully!")
except Exception as e:
    print(f"VibeVoice-ASR failed: {e}")
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
    print("Whisper-large-v3 loaded successfully!")

# %% Cell 4: TTS 모델 로드 (Qwen3-TTS 또는 대안)
print("\nLoading TTS model...")

tts_model = None
TTS_MODEL_NAME = "Not available"

# 방법 1: edge-tts (무료, 안정적)
try:
    import edge_tts
    TTS_MODEL_NAME = "Edge-TTS"
    print("Edge-TTS loaded successfully!")
except ImportError:
    print("Installing edge-tts...")
    import subprocess
    subprocess.run(["pip", "install", "-q", "edge-tts"], check=True)
    import edge_tts
    TTS_MODEL_NAME = "Edge-TTS"
    print("Edge-TTS installed and loaded!")

# %% Cell 5: STT 함수 정의
def convert_to_wav(input_path):
    """pydub을 사용해 다양한 오디오 형식을 WAV로 변환"""
    try:
        from pydub import AudioSegment

        # 파일 확장자 확인
        ext = input_path.split('.')[-1].lower() if '.' in input_path else 'wav'

        # 형식에 따라 로드
        if ext == 'webm':
            audio = AudioSegment.from_file(input_path, format='webm')
        elif ext == 'mp3':
            audio = AudioSegment.from_mp3(input_path)
        elif ext == 'ogg':
            audio = AudioSegment.from_ogg(input_path)
        elif ext == 'm4a':
            audio = AudioSegment.from_file(input_path, format='m4a')
        else:
            audio = AudioSegment.from_file(input_path)

        # 16kHz 모노 WAV로 변환 (Whisper 최적)
        audio = audio.set_frame_rate(16000).set_channels(1)

        # 임시 WAV 파일로 저장
        wav_path = tempfile.NamedTemporaryFile(suffix=".wav", delete=False).name
        audio.export(wav_path, format="wav")

        print(f"오디오 변환 완료: {ext} -> wav (16kHz mono)")
        return wav_path
    except Exception as e:
        print(f"pydub 변환 실패: {e}")
        return None

def transcribe(audio_input, language="korean"):
    """음성을 텍스트로 변환"""
    if audio_input is None:
        return "오디오 파일을 업로드해주세요."

    try:
        lang_map = {"korean": "ko", "english": "en", "japanese": "ja", "chinese": "zh"}
        lang_code = lang_map.get(language.lower(), "ko")

        audio_path = None

        # audio_input 타입에 따라 처리
        if isinstance(audio_input, dict):
            # Gradio API 호출 시 {"name": "...", "data": "data:...;base64,..."} 형태
            print(f"[STT] 딕셔너리 입력 감지: keys={audio_input.keys()}")
            if "data" in audio_input:
                data_str = audio_input["data"]
                if data_str.startswith("data:"):
                    data_str = data_str.split(",")[1]
                audio_bytes = base64.b64decode(data_str)

                # 확장자 결정 (원본 파일명에서 추출)
                name = audio_input.get("name", "audio.wav")
                ext = name.split('.')[-1].lower() if '.' in name else 'wav'
                print(f"[STT] Base64 디코딩 완료: {len(audio_bytes)} bytes, ext={ext}")

                with tempfile.NamedTemporaryFile(suffix=f".{ext}", delete=False) as f:
                    f.write(audio_bytes)
                    audio_path = f.name
            elif "path" in audio_input:
                audio_path = audio_input["path"]
            else:
                return f"오류: 알 수 없는 딕셔너리 형식 - {audio_input.keys()}"

        elif isinstance(audio_input, str):
            if audio_input.startswith("data:") or len(audio_input) > 1000:
                # Base64 데이터
                if audio_input.startswith("data:"):
                    audio_input = audio_input.split(",")[1]
                audio_bytes = base64.b64decode(audio_input)

                # 임시 파일로 저장
                with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
                    f.write(audio_bytes)
                    audio_path = f.name
            else:
                # 파일 경로
                audio_path = audio_input
        elif isinstance(audio_input, tuple):
            # Gradio Audio 컴포넌트에서 (sample_rate, audio_data) 튜플로 올 수 있음
            sample_rate, audio_data = audio_input
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
                sf.write(f.name, audio_data, sample_rate)
                audio_path = f.name
        else:
            print(f"[STT] 알 수 없는 입력 타입: {type(audio_input)}")
            audio_path = audio_input

        print(f"입력 오디오: {audio_path}")

        # pydub으로 WAV 변환 시도
        wav_path = convert_to_wav(audio_path)
        if wav_path:
            audio_path = wav_path
        else:
            # pydub 실패 시 soundfile로 시도
            try:
                audio_data, sample_rate = sf.read(audio_path)
                if sample_rate != 16000:
                    from scipy import signal
                    number_of_samples = round(len(audio_data) * 16000 / sample_rate)
                    audio_data = signal.resample(audio_data, number_of_samples)
                    sample_rate = 16000
                normalized_path = tempfile.NamedTemporaryFile(suffix=".wav", delete=False).name
                sf.write(normalized_path, audio_data, sample_rate)
                audio_path = normalized_path
                print("soundfile로 변환 완료")
            except Exception as e:
                print(f"soundfile 변환도 실패, 원본 사용: {e}")

        result = stt_pipe(
            audio_path,
            generate_kwargs={"language": lang_code},
            return_timestamps=False,
        )

        return result["text"]
    except Exception as e:
        import traceback
        traceback.print_exc()
        return f"오류 발생: {str(e)}"

# %% Cell 6: TTS 함수 정의
import asyncio

async def _synthesize_edge_tts(text, voice):
    """Edge-TTS로 음성 합성 (비동기)"""
    communicate = edge_tts.Communicate(text, voice)

    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as f:
        await communicate.save(f.name)
        return f.name

def synthesize(text, language="Korean"):
    """텍스트를 음성으로 변환"""
    if not text or not text.strip():
        return None

    try:
        # 언어별 음성 선택
        voice_map = {
            "Korean": "ko-KR-SunHiNeural",      # 한국어 여성
            "English": "en-US-JennyNeural",     # 영어 여성
            "Chinese": "zh-CN-XiaoxiaoNeural",  # 중국어 여성
            "Japanese": "ja-JP-NanamiNeural",   # 일본어 여성
        }
        voice = voice_map.get(language, "ko-KR-SunHiNeural")

        # 비동기 함수 실행
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        audio_path = loop.run_until_complete(_synthesize_edge_tts(text, voice))
        loop.close()

        return audio_path
    except Exception as e:
        print(f"TTS error: {e}")
        return None

def synthesize_base64(text, language="Korean"):
    """텍스트를 음성으로 변환 후 Base64 반환 (API용)"""
    audio_path = synthesize(text, language)
    if audio_path is None:
        return None

    with open(audio_path, "rb") as f:
        audio_data = f.read()

    return base64.b64encode(audio_data).decode("utf-8")

# %% Cell 7: Gradio UI 생성
with gr.Blocks(title="Pre-View 음성 서비스") as demo:
    gr.Markdown(f"""
    # Pre-View 음성 서비스

    | 기능 | 모델 |
    |------|------|
    | **STT** (음성->텍스트) | {STT_MODEL_NAME} |
    | **TTS** (텍스트->음성) | {TTS_MODEL_NAME} |

    ---
    **Spring Boot 연동 방법:**
    1. 아래 공개 URL 복사
    2. Pre-View 관리자 페이지에서 URL 설정
    3. 음성 면접 기능 활성화
    """)

    with gr.Tab("STT (음성 인식)"):
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

    with gr.Tab("TTS (음성 합성)"):
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
print("Starting Pre-View Voice Service...")
print("="*50)

demo.launch(share=True)  # share=True로 공개 URL 생성
