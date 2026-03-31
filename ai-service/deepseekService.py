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

PROMPT_GET_TRANSLATIONS = """
Toma el siguiente texto en chino y tradúcelo al inglés y al español.

REGLAS ESTRICTAS:
- El texto original tiene exactamente {sentence_count} frases, separadas por 。
- Tu traducción DEBE tener exactamente {sentence_count} frases.
- Cada frase traducida debe terminar con punto (.).
- Cuenta los puntos de tu respuesta antes de enviarla. Deben ser exactamente {sentence_count} en cada idioma.
- No unas ni dividas frases. Traduce frase por frase en el mismo orden.

Devuelve solo un array con dos elementos con este formato (comillas simples, formato Python):
['Frase1 inglés. Frase2 inglés.', 'Frase1 español. Frase2 español.']

No incluyas explicaciones ni texto adicional. Solo el array.

Texto: "{input_text}"
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

def call_deepseek(prompt: str) -> str:
    client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_API_URL)
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}],
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


def count_sentences(text: str) -> int:
    return text.count('。')

def count_periods(text: str) -> int:
    return text.count('.')

@app.route("/getTranslations", methods=["POST"])
def get_translations():
    data = request.get_json()
    if not data or "text" not in data:
        return jsonify({"error": "text is required"}), 400

    input_text = data["text"]
    sentence_count = count_sentences(input_text)

    max_retries = 3
    for attempt in range(max_retries):
        prompt = PROMPT_GET_TRANSLATIONS\
            .replace("{input_text}", input_text)\
            .replace("{sentence_count}", str(sentence_count))
        
        raw = call_deepseek(prompt)
        print(f"Intento {attempt + 1} - Respuesta: {raw}")

        try:
            parsed = ast.literal_eval(raw)
            if not isinstance(parsed, list) or len(parsed) != 2:
                raise ValueError("Expected list of 2 elements")
            
            english_periods = count_periods(parsed[0])
            spanish_periods = count_periods(parsed[1])
            
            if english_periods == sentence_count and spanish_periods == sentence_count:
                return jsonify(parsed)
            else:
                print(f"Número de frases incorrecto. Original: {sentence_count}, EN: {english_periods}, ES: {spanish_periods}. Reintentando...")
        except Exception as e:
            print(f"Error parseando respuesta: {e}")

    return jsonify({"error": f"Could not get matching sentence count after {max_retries} attempts"}), 500


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


if __name__ == "__main__":
    app.run(port=5001, debug=True)
