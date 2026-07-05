from flask import Flask, request, jsonify
from flask_cors import CORS
from google.cloud import vision
from PIL import Image, ImageOps
import io
import os

app = Flask(__name__)
CORS(app)

# El cliente se inicializa automáticamente usando GOOGLE_APPLICATION_CREDENTIALS
client = vision.ImageAnnotatorClient()


@app.route('/ocr', methods=['POST'])
def ocr_image():
    uploaded_file = request.files.get('file')

    if not uploaded_file:
        return jsonify({"error": "No file received"}), 400

    print("Archivo recibido:", uploaded_file.filename)

    # Preprocesamos la imagen igual que antes
    img = Image.open(uploaded_file.stream).convert('RGB')
    img.thumbnail((1024, 1024))
    padding = 20
    img = ImageOps.expand(img, border=padding, fill='white')

    # Convertimos a bytes para enviarlo a Google Vision
    img_bytes = io.BytesIO()
    img.save(img_bytes, format='JPEG')
    img_bytes = img_bytes.getvalue()

    try:
        image = vision.Image(content=img_bytes)
        response = client.text_detection(image=image)

        if response.error.message:
            print(f"Error de Google Vision: {response.error.message}")
            return jsonify({"error": response.error.message}), 500

        texts = response.text_annotations
        if not texts:
            return jsonify({"error": "No text found in image"}), 422

        # El primer elemento contiene todo el texto detectado
        full_text = texts[0].description.strip()
        print(f"Texto detectado: {full_text}")

        # Por defecto (flujo admin) devolvemos texto continuo, sin saltos.
        # Si se pide preserve_layout (flujo de textos de usuario), conservamos la
        # disposición original (diálogos, conversaciones): quitamos espacios al
        # final de cada línea y colapsamos líneas en blanco de más.
        preserve_layout = str(request.form.get('preserve_layout', '')).lower() in ('1', 'true', 'yes')
        if preserve_layout:
            lines = [line.rstrip() for line in full_text.split('\n')]
            cleaned = '\n'.join(lines)
            while '\n\n\n' in cleaned:
                cleaned = cleaned.replace('\n\n\n', '\n\n')
            cleaned = cleaned.strip()
        else:
            cleaned = full_text.replace('\n', '')

        return jsonify({"text": cleaned})

    except Exception as e:
        print(f"Error en OCR: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=False)