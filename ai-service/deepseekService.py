from flask import Flask, request, jsonify
from flask_cors import CORS
from openai import OpenAI
import os
import ast
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
CORS(app)

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")
DEEPSEEK_API_URL = "https://api.deepseek.com"

PROMPT_GENERATE_TEXT = """
Genera un texto en chino mandarín de nivel {level} para aprender chino.
El texto debe tener entre 3 y 6 frases. Cada frase debe terminar con un punto chino (。).
El vocabulario y la gramática deben ser apropiados para el nivel {level}.
{topic_instruction}
No incluyas nada más, solo el texto en chino.
"""

PROMPT_GET_TITLES = """
Toma el siguiente texto en chino y devuelve un título en inglés y otro en español.
No devuelvas nada más. Solo el array con este formato exacto:
["English title", "Título en español"]

Texto: "{input_text}"
"""

PROMPT_TRANSLATE_SENTENCE = """
Traduce la siguiente frase en chino al inglés y al español. 
La frase no debe contener ningún punto (.) ni punto chino (。). Pero otros signos de puntuación sí.
Devuelve solo un array con dos elementos con este formato (comillas simples, formato Python):
['English translation', 'Traducción en español']
No incluyas explicaciones ni texto adicional. Solo el array.
Frase: "{sentence}"
"""

PROMPT_GET_DESCRIPTIONS = """
Toma el siguiente texto en chino y genera una breve descripción atractiva en inglés y en español.
Devuelve solo un array con dos elementos con este formato exacto:
["English description", "Descripción en español"]
No incluyas explicaciones ni texto adicional. Solo el array.
Texto: "{input_text}"
"""

PROMPT_GET_MISSING_WORDS = """
Toma el siguiente array de palabras en chino que no están en el diccionario.
Para cada palabra devuelve su pinyin, su traducción al inglés y al español.
Devuelve exactamente un array de objetos Python con este formato:
[{{"chinese": "词", "pinyin": "cí", "english": "word", "spanish": "palabra"}}]
No incluyas explicaciones ni texto adicional. Solo el array.
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


def call_deepseek(prompt: str) -> str:
    client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_API_URL)
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}],
        stream=False
    )
    return response.choices[0].message.content.strip()


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


@app.route("/generate", methods=["POST"])
def generate_text():
    data = request.get_json()
    if not data or "level" not in data:
        return jsonify({"error": "level is required"}), 400

    level = data["level"]
    topic = data.get("topic", "").strip()

    topic_instruction = ""
    if topic:
        topic_instruction = f"El texto debe tratar sobre el siguiente tema o seguir estas directrices: {topic}"

    prompt = PROMPT_GENERATE_TEXT\
        .replace("{level}", level)\
        .replace("{topic_instruction}", topic_instruction)

    text = call_deepseek(prompt)
    return jsonify({"text": text})


@app.route("/getTitles", methods=["POST"])
def get_titles():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    prompt = PROMPT_GET_TITLES.replace("{input_text}", data["text"])
    raw = call_deepseek(prompt)
    try:
        parsed = ast.literal_eval(raw)
        if not isinstance(parsed, list) or len(parsed) != 2:
            raise ValueError("Expected list of 2 elements")
        return jsonify(parsed)
    except Exception as e:
        return jsonify({"error": f"Invalid format: {str(e)}", "raw": raw}), 500


@app.route("/getTranslations", methods=["POST"])
def get_translations():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    input_text = data["text"]

    # Dividimos por 。 y filtramos vacíos
    sentences = [s.strip() for s in input_text.split('。') if s.strip()]

    english_parts = []
    spanish_parts = []

    for sentence in sentences:
        prompt = PROMPT_TRANSLATE_SENTENCE.replace("{sentence}", sentence)
        raw = call_deepseek(prompt)
        try:
            parsed = ast.literal_eval(raw)
            if not isinstance(parsed, list) or len(parsed) != 2:
                raise ValueError("Expected list of 2")
            english_parts.append(parsed[0].strip().rstrip('.') + '.')
            spanish_parts.append(parsed[1].strip().rstrip('.') + '.')
        except Exception as e:
            print(f"Error traduciendo frase '{sentence}': {e}")
            english_parts.append('')
            spanish_parts.append('')

    english_full = ' '.join(english_parts)
    spanish_full = ' '.join(spanish_parts)

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
        prompt = PROMPT_TRANSLATE_SENTENCE.replace("{sentence}", s)
        raw = call_deepseek(prompt)
        try:
            parsed = ast.literal_eval(raw)
            if not isinstance(parsed, list) or len(parsed) != 2:
                raise ValueError("Expected list of 2")
            result.append([str(parsed[0]).strip(), str(parsed[1]).strip()])
        except Exception as e:
            print(f"Error traduciendo frase '{s}': {e}")
            result.append(["", ""])

    return jsonify(result)


@app.route("/getDescriptions", methods=["POST"])
def get_descriptions():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    prompt = PROMPT_GET_DESCRIPTIONS.replace("{input_text}", data["text"])
    raw = call_deepseek(prompt)
    try:
        parsed = ast.literal_eval(raw)
        if not isinstance(parsed, list) or len(parsed) != 2:
            raise ValueError("Expected list of 2 elements")
        return jsonify(parsed)
    except Exception as e:
        return jsonify({"error": f"Invalid format: {str(e)}", "raw": raw}), 500


@app.route("/getMissingWords", methods=["POST"])
def get_missing_words():
    data = request.get_json()
    if not data or "words" not in data:
        return jsonify({"error": "words is required"}), 400

    prompt = PROMPT_GET_MISSING_WORDS.replace("{input_text}", str(data["words"]))
    raw = call_deepseek(prompt)
    try:
        parsed = ast.literal_eval(raw)
        if not isinstance(parsed, list):
            raise ValueError("Expected a list")
        return jsonify(parsed)
    except Exception as e:
        return jsonify({"error": f"Invalid format: {str(e)}", "raw": raw}), 500


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
    return jsonify({"reply": reply})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
