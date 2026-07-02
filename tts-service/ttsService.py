from flask import Flask, request, jsonify, Response
from flask_cors import CORS
from google.cloud import texttospeech

app = Flask(__name__)
CORS(app)

# El cliente se inicializa automáticamente usando GOOGLE_APPLICATION_CREDENTIALS.
client = texttospeech.TextToSpeechClient()

# Límite de seguridad: la API de Google acepta hasta 5000 bytes por petición.
# Un carácter chino ocupa 3 bytes en UTF-8, así que ~1600 caracteres es un tope
# prudente. Los textos de la app son mucho más cortos; esto solo evita que un
# texto anormalmente largo rompa la petición.
MAX_CHARS = 1600

# Voz neuronal (WaveNet) de mandarín. Ritmo ligeramente más lento (0.9) porque
# la app es para estudiantes que están aprendiendo a leer.
LANGUAGE_CODE = "cmn-CN"
VOICE_NAME = "cmn-CN-Wavenet-A"
DEFAULT_RATE = 0.9


@app.route("/synthesize", methods=["POST"])
def synthesize():
    data = request.get_json(silent=True) or {}
    text = (data.get("text") or "").strip()

    if not text:
        return jsonify({"error": "No text received"}), 400

    if len(text) > MAX_CHARS:
        text = text[:MAX_CHARS]

    # Ritmo opcional (0.25–4.0 según Google); por defecto algo más lento.
    try:
        rate = float(data.get("rate", DEFAULT_RATE))
    except (TypeError, ValueError):
        rate = DEFAULT_RATE
    rate = max(0.25, min(rate, 4.0))

    try:
        synthesis_input = texttospeech.SynthesisInput(text=text)
        voice = texttospeech.VoiceSelectionParams(
            language_code=LANGUAGE_CODE,
            name=VOICE_NAME,
        )
        audio_config = texttospeech.AudioConfig(
            audio_encoding=texttospeech.AudioEncoding.MP3,
            speaking_rate=rate,
        )

        response = client.synthesize_speech(
            input=synthesis_input,
            voice=voice,
            audio_config=audio_config,
        )

        return Response(response.audio_content, mimetype="audio/mpeg")

    except Exception as e:
        print(f"Error en TTS: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5002, debug=False)
