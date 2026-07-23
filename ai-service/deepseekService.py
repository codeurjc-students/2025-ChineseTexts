from flask import Flask, request, jsonify
from flask_cors import CORS
from openai import OpenAI
import os
import json
import re
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
CORS(app)

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")
DEEPSEEK_API_URL = "https://api.deepseek.com"

# Structured prompts ask for a JSON OBJECT (not a bare array) because DeepSeek's
# strict JSON mode (response_format json_object) only guarantees an object at the
# top level. The endpoints unwrap it back to the array shapes the backend expects.
PROMPT_GENERATE_TEXT = """
Genera un texto en chino mandarín de nivel {level} para aprender chino.
El texto debe tener entre 3 y 6 frases. Cada frase debe terminar con un punto chino (。).
El vocabulario y la gramática deben ser apropiados para el nivel {level}.
{topic_instruction}
No incluyas nada más, solo el texto en chino.
"""

PROMPT_GET_TITLES = """
Inventa un TÍTULO corto y atractivo (máximo 8 palabras) para el siguiente texto en chino,
en inglés y en español. NO traduzcas el texto: solo un título breve que resuma de qué trata.
Responde SOLO con un objeto JSON válido con exactamente este formato:
{"english": "Short English title", "spanish": "Título corto en español"}
No incluyas explicaciones ni texto adicional. Solo el JSON.

Texto: "{input_text}"
"""

PROMPT_TRANSLATE_SENTENCE = """
Traduce la siguiente frase en chino al inglés y al español.
La traducción no debe contener ningún punto (.) ni punto chino (。). Pero otros signos de puntuación sí.
Responde SOLO con un objeto JSON válido con exactamente este formato:
{"english": "English translation", "spanish": "Traducción en español"}
No incluyas explicaciones ni texto adicional. Solo el JSON.
Frase: "{sentence}"
"""

PROMPT_GET_DESCRIPTIONS = """
Genera una breve descripción atractiva (1 o 2 frases) del siguiente texto en chino,
en inglés y en español. NO traduzcas el texto: describe de qué trata, como un resumen editorial.
Responde SOLO con un objeto JSON válido con exactamente este formato:
{"english": "Short English description", "spanish": "Breve descripción en español"}
No incluyas explicaciones ni texto adicional. Solo el JSON.
Texto: "{input_text}"
"""

PROMPT_GET_MISSING_WORDS = """
Toma el siguiente array de palabras en chino que no están en el diccionario.
Para cada palabra devuelve su pinyin, su traducción al inglés y al español.
Responde SOLO con un objeto JSON válido con exactamente este formato:
{"words": [{"chinese": "词", "pinyin": "cí", "english": "word", "spanish": "palabra"}]}
Incluye exactamente una entrada por cada palabra del array de entrada, sin inventar palabras nuevas.
No incluyas explicaciones ni texto adicional. Solo el JSON.
Array: {input_text}
"""

PROMPT_WORD_CHAT_SYSTEM = """You are a friendly, expert Chinese tutor helping a learner understand ONE specific word as it appears in a text they are reading.

The word: {word}
The sentence it appears in: {sentence}
The full text being read: {text}
Translation of the text: {translation}
{level_line}

Your job: explain THIS word IN THIS CONTEXT and answer the learner's follow-up questions about it — its meaning here, pronunciation, the characters, grammar, usage, nuances, common collocations, similar or related words, and example sentences.

Rules:
- Stay strictly on the topic of this word and its context. If the learner asks about something unrelated (other topics, general chit-chat, tasks that are not about this word), politely decline in one short sentence and steer them back to the word.
- Be concise, clear and encouraging: short explanations and examples a learner can follow.
- Write in a natural, conversational tone, as if you were a friendly tutor talking to the learner in person. Do NOT use Markdown or any formatting symbols: no asterisks for bold or italics, no "#" headings, no numbered "1." section headers, no bullet-point markers. Use plain sentences and short paragraphs; a line break between ideas is fine. You may still write Chinese characters and pinyin inline.
- ALWAYS reply in {language_full}, whatever language the question is in.
"""

CHINESE_CHAR_RE = re.compile(r"[一-鿿]")

# Sentence-final terminators — the SAME set the backend and frontend use, so the
# sentence boundaries agree end to end (this is what keeps the full translation
# splittable back into exactly one part per Chinese sentence).
SENTENCE_TERMINATORS = "。！？…；.!?;"
SENTENCE_SPLIT_RE = re.compile(r"(?<=[。！？…；.!?;])")
TERMINAL_BORROW = {"？": "?", "?": "?", "！": "!", "!": "!", "…": "…", "；": ";", ";": ";"}


def ensure_terminal(translation: str, chinese_sentence: str) -> str:
    """Ensures a sentence translation ends with sentence-final punctuation,
    borrowing the terminator from its Chinese sentence when the AI drops it."""
    t = (translation or "").strip()
    if not t:
        return ""
    if t[-1] in SENTENCE_TERMINATORS:
        return t
    zh = (chinese_sentence or "").strip()
    return t + TERMINAL_BORROW.get(zh[-1] if zh else "", ".")


class AiFormatError(ValueError):
    """The model's reply did not match the required JSON format/schema."""

    def __init__(self, message: str, raw: str = ""):
        super().__init__(message)
        self.raw = raw


def call_deepseek(prompt: str, json_mode: bool = False) -> str:
    client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_API_URL)
    extra = {"response_format": {"type": "json_object"}} if json_mode else {}
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}],
        stream=False,
        **extra
    )
    return (response.choices[0].message.content or "").strip()


def call_deepseek_chat(system_prompt: str, history: list) -> str:
    """Multi-turn chat: a system prompt plus the prior user/assistant messages."""
    client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_API_URL)
    messages = [{"role": "system", "content": system_prompt}]
    for m in history:
        role = m.get("role")
        content = (m.get("content") or "").strip()
        if role in ("user", "assistant") and content:
            messages.append({"role": role, "content": content})
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=messages,
        stream=False
    )
    return response.choices[0].message.content.strip()


def strip_code_fences(raw: str) -> str:
    """Remove a surrounding markdown code fence (```json ... ```), if present."""
    text = raw.strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z]*\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    return text.strip()


def ask_deepseek_json(prompt: str, validate):
    """Call DeepSeek in strict JSON mode, parse and schema-validate the reply.

    One retry on a malformed/invalid reply; after two failed attempts raises
    AiFormatError carrying the last raw reply for diagnostics.
    """
    last_error = None
    for attempt in (1, 2):
        raw = call_deepseek(prompt, json_mode=True)
        try:
            parsed = json.loads(strip_code_fences(raw))
            if not isinstance(parsed, dict):
                raise ValueError("Expected a JSON object")
            return validate(parsed)
        except (json.JSONDecodeError, ValueError) as e:
            last_error = AiFormatError(f"Invalid AI response (attempt {attempt}/2): {e}", raw=raw)
            print(f"[ai-service] {last_error} — raw: {raw[:300]!r}")
    raise last_error


def require_string(obj: dict, key: str) -> str:
    value = obj.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"'{key}' must be a non-empty string")
    return value.strip()


def validate_bilingual_pair(parsed: dict) -> list:
    """{"english": ..., "spanish": ...} -> [english, spanish] (the backend's shape)."""
    return [require_string(parsed, "english"), require_string(parsed, "spanish")]


def make_words_validator(requested_words: list):
    """Validator for /getMissingWords tied to the words that were actually asked for.

    Drops entries with missing/empty fields and entries for words the model
    invented (not in the request), and returns the survivors deduplicated in the
    input order. If nothing survives, raises so ask_deepseek_json retries.
    """
    requested = list(dict.fromkeys(
        w for w in (str(word).strip() for word in requested_words) if w
    ))
    requested_set = set(requested)

    def validate(parsed: dict) -> list:
        entries = parsed.get("words")
        if not isinstance(entries, list):
            raise ValueError("'words' must be a list")
        by_chinese = {}
        for entry in entries:
            if not isinstance(entry, dict):
                print(f"[ai-service] Dropping non-object word entry: {entry!r}")
                continue
            try:
                word = {key: require_string(entry, key)
                        for key in ("chinese", "pinyin", "english", "spanish")}
            except ValueError as e:
                print(f"[ai-service] Dropping invalid word entry {entry!r}: {e}")
                continue
            if word["chinese"] not in requested_set:
                print(f"[ai-service] Dropping word not in the request: {word['chinese']!r}")
                continue
            by_chinese.setdefault(word["chinese"], word)
        if requested and not by_chinese:
            raise ValueError("No valid entries for any of the requested words")
        return [by_chinese[w] for w in requested if w in by_chinese]

    return validate


def clean_generated_text(raw: str) -> str:
    """Strip fences/wrapping quotes and require actual Chinese characters."""
    text = strip_code_fences(raw).strip().strip('"“”').strip()
    if not CHINESE_CHAR_RE.search(text):
        raise ValueError("Generated text contains no Chinese characters")
    return text


def translate_sentence_pair(sentence: str) -> list:
    """[english, spanish] for one Chinese sentence; raises AiFormatError on failure."""
    prompt = PROMPT_TRANSLATE_SENTENCE.replace("{sentence}", sentence)
    return ask_deepseek_json(prompt, validate_bilingual_pair)


@app.route("/generate", methods=["POST"])
def generate_text():
    data = request.get_json()
    if not data or not str(data.get("level", "")).strip():
        return jsonify({"error": "level is required"}), 400

    level = str(data["level"]).strip()
    topic = str(data.get("topic", "")).strip()

    topic_instruction = ""
    if topic:
        topic_instruction = f"El texto debe tratar sobre el siguiente tema o seguir estas directrices: {topic}"

    prompt = PROMPT_GENERATE_TEXT\
        .replace("{level}", level)\
        .replace("{topic_instruction}", topic_instruction)

    last_error = None
    for attempt in (1, 2):
        raw = call_deepseek(prompt)
        try:
            return jsonify({"text": clean_generated_text(raw)})
        except ValueError as e:
            last_error = e
            print(f"[ai-service] Invalid generated text (attempt {attempt}/2): {e} — raw: {raw[:300]!r}")
    return jsonify({"error": f"Invalid generated text: {last_error}"}), 500


@app.route("/getTitles", methods=["POST"])
def get_titles():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    prompt = PROMPT_GET_TITLES.replace("{input_text}", data["text"])
    try:
        return jsonify(ask_deepseek_json(prompt, validate_bilingual_pair))
    except AiFormatError as e:
        return jsonify({"error": str(e), "raw": e.raw}), 500


@app.route("/getTranslations", methods=["POST"])
def get_translations():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    input_text = data["text"]

    # Dividimos por TODOS los terminadores de frase (。！？…；.!?;) — los mismos que
    # usan backend y frontend — y cada traducción conserva SU puntuación final
    # (prestada de la frase china si el modelo la pierde). Así el texto completo
    # se vuelve a partir exactamente en una frase por frase china.
    sentences = [s.strip() for s in SENTENCE_SPLIT_RE.split(input_text) if s.strip()]

    english_parts = []
    spanish_parts = []

    for sentence in sentences:
        try:
            english, spanish = translate_sentence_pair(sentence)
            english_parts.append(ensure_terminal(english, sentence))
            spanish_parts.append(ensure_terminal(spanish, sentence))
        except AiFormatError as e:
            print(f"Error traduciendo frase '{sentence}': {e}")
            english_parts.append('')
            spanish_parts.append('')

    english_full = ' '.join(p for p in english_parts if p)
    spanish_full = ' '.join(p for p in spanish_parts if p)

    return jsonify([english_full, spanish_full])


@app.route("/translateSentences", methods=["POST"])
def translate_sentences():
    data = request.get_json()
    if not data or "sentences" not in data:
        return jsonify({"error": "sentences is required"}), 400

    sentences = data["sentences"]
    if not isinstance(sentences, list):
        return jsonify({"error": "sentences must be a list"}), 400

    # Traducimos frase a frase para que la salida quede alineada 1:1 con la entrada
    # (mismo número de elementos y mismo orden). Si una frase falla, devolvemos
    # ['', ''] para esa posición sin romper la alineación del resto.
    result = []
    for sentence in sentences:
        s = (sentence or "").strip()
        if not s:
            result.append(["", ""])
            continue
        try:
            result.append(translate_sentence_pair(s))
        except AiFormatError as e:
            print(f"Error traduciendo frase '{s}': {e}")
            result.append(["", ""])

    return jsonify(result)


@app.route("/getDescriptions", methods=["POST"])
def get_descriptions():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    prompt = PROMPT_GET_DESCRIPTIONS.replace("{input_text}", data["text"])
    try:
        return jsonify(ask_deepseek_json(prompt, validate_bilingual_pair))
    except AiFormatError as e:
        return jsonify({"error": str(e), "raw": e.raw}), 500


@app.route("/getMissingWords", methods=["POST"])
def get_missing_words():
    data = request.get_json()
    if not data or "words" not in data:
        return jsonify({"error": "words is required"}), 400

    words = data["words"]
    if not isinstance(words, list):
        return jsonify({"error": "words must be a list"}), 400

    if not any(str(w).strip() for w in words):
        return jsonify([])

    prompt = PROMPT_GET_MISSING_WORDS.replace("{input_text}", str(words))
    try:
        return jsonify(ask_deepseek_json(prompt, make_words_validator(words)))
    except AiFormatError as e:
        return jsonify({"error": str(e), "raw": e.raw}), 500


@app.route("/chatWord", methods=["POST"])
def chat_word():
    data = request.get_json()
    if not data or not str(data.get("word", "")).strip():
        return jsonify({"error": "word is required"}), 400

    history = data.get("history", [])
    if not isinstance(history, list) or len(history) == 0:
        return jsonify({"error": "history must be a non-empty list"}), 400

    word = str(data.get("word", "")).strip()
    sentence = str(data.get("sentence", "")).strip()
    text = str(data.get("text", "")).strip()
    translation = str(data.get("translation", "")).strip()
    level = str(data.get("level", "")).strip()
    language = str(data.get("language", "en")).strip().lower()

    language_full = "Spanish (español)" if language == "es" else "English"
    level_line = f"The learner's HSK level: {level}" if level else ""

    system_prompt = PROMPT_WORD_CHAT_SYSTEM\
        .replace("{word}", word)\
        .replace("{sentence}", sentence or word)\
        .replace("{text}", text or sentence or word)\
        .replace("{translation}", translation or "(not provided)")\
        .replace("{level_line}", level_line)\
        .replace("{language_full}", language_full)

    reply = call_deepseek_chat(system_prompt, history)
    if not reply:
        return jsonify({"error": "Empty reply from AI"}), 500
    return jsonify({"reply": reply})


# Sonda de vida para el agregador /api/health del backend. Solo confirma que el
# proceso Flask responde: NO llama a ninguna API externa (no cuesta dinero).
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

if __name__ == "__main__":
    # Debug (auto-reload + Werkzeug debugger) only when explicitly requested in
    # local development; the Docker container must never run with it enabled.
    debug = os.getenv("FLASK_DEBUG", "0").lower() in ("1", "true")
    app.run(host="0.0.0.0", port=5001, debug=debug)
