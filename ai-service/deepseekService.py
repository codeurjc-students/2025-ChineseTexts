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
No incluyas nada más, solo el texto en chino.
"""

PROMPT_GET_TITLES = """
Toma el siguiente texto en chino y devuelve un título en inglés y otro en español.
No devuelvas nada más. Solo el array con este formato exacto:
["English title", "Título en español"]

Texto: "{input_text}"
"""

PROMPT_GET_TRANSLATIONS = """
Toma el siguiente texto en chino y tradúcelo completamente al inglés y al español.
El número de frases en cada traducción debe coincidir exactamente con el número de frases del texto original (cada frase termina en 。).
No uses puntos finales (.) en las traducciones. Usa otros signos si es necesario.
Devuelve solo un array con dos elementos exactamente con este formato (comillas simples, formato Python):
['Traducción completa al inglés', 'Traducción completa al español']
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
    prompt = PROMPT_GENERATE_TEXT.replace("{level}", level)
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

    prompt = PROMPT_GET_TRANSLATIONS.replace("{input_text}", data["text"])
    raw = call_deepseek(prompt)
    try:
        parsed = ast.literal_eval(raw)
        if not isinstance(parsed, list) or len(parsed) != 2:
            raise ValueError("Expected list of 2 elements")
        return jsonify(parsed)
    except Exception as e:
        return jsonify({"error": f"Invalid format: {str(e)}", "raw": raw}), 500


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
