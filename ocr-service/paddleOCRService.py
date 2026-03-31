from flask import Flask, request, jsonify
from flask_cors import CORS
import tempfile
import os
from paddleocr import PaddleOCR
from PIL import Image, ImageOps

app = Flask(__name__)
CORS(app)

ocr = PaddleOCR(use_angle_cls=True, lang='ch')


@app.route('/ocr', methods=['POST'])
def ocr_image():
    uploaded_file = request.files.get('file')

    if not uploaded_file:
        return jsonify({"error": "No file received"}), 400

    print("Archivo recibido:", uploaded_file.filename)

    img = Image.open(uploaded_file.stream)
    img.thumbnail((512, 512))
    padding = 50
    img = ImageOps.expand(img, border=padding, fill='white')

    with tempfile.NamedTemporaryFile(
        delete=True,
        suffix=os.path.splitext(uploaded_file.filename)[1] or '.jpg'
    ) as tmp:
        img.save(tmp.name)
        print("Archivo temporal:", tmp.name)

        result = ocr.predict(tmp.name)
        print("Resultado OCR:", result)

        if result and isinstance(result, list) and 'rec_texts' in result[0]:
            recognized_texts = result[0]['rec_texts']
            text_string = ''.join(recognized_texts)
            return jsonify({"text": text_string})
        else:
            return jsonify({"error": "No text found in image"}), 422


if __name__ == '__main__':
    app.run(port=5000, debug=True)

