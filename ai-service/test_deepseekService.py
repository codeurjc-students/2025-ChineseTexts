"""Unit tests for the hardened AI pipeline (strict JSON + schema validation).

DeepSeek calls are mocked, so the suite runs offline and for free:

    venv/bin/python -m unittest -v
"""
import unittest
from unittest.mock import patch

import deepseekService


class AiServiceTestCase(unittest.TestCase):

    def setUp(self):
        deepseekService.app.config["TESTING"] = True
        self.client = deepseekService.app.test_client()

    # ————————————————————— /getTitles (bilingual pair) —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_get_titles_returns_backend_array_shape(self, mock_call):
        mock_call.return_value = '{"english": "A Day at School", "spanish": "Un día en la escuela"}'
        response = self.client.post("/getTitles", json={"text": "我今天去学校。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["A Day at School", "Un día en la escuela"])
        mock_call.assert_called_once()
        self.assertTrue(mock_call.call_args.kwargs.get("json_mode"))

    @patch("deepseekService.call_deepseek")
    def test_get_titles_accepts_markdown_fenced_json(self, mock_call):
        mock_call.return_value = '```json\n{"english": "Title", "spanish": "Título"}\n```'
        response = self.client.post("/getTitles", json={"text": "你好。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["Title", "Título"])

    @patch("deepseekService.call_deepseek")
    def test_get_titles_retries_once_then_succeeds(self, mock_call):
        mock_call.side_effect = [
            "Sure! Here are the titles you asked for.",
            '{"english": "Title", "spanish": "Título"}',
        ]
        response = self.client.post("/getTitles", json={"text": "你好。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["Title", "Título"])
        self.assertEqual(mock_call.call_count, 2)

    @patch("deepseekService.call_deepseek")
    def test_get_titles_fails_after_two_invalid_attempts(self, mock_call):
        mock_call.side_effect = ["not json", "still not json"]
        response = self.client.post("/getTitles", json={"text": "你好。"})
        self.assertEqual(response.status_code, 500)
        body = response.get_json()
        self.assertIn("error", body)
        self.assertEqual(body["raw"], "still not json")
        self.assertEqual(mock_call.call_count, 2)

    @patch("deepseekService.call_deepseek")
    def test_get_titles_rejects_empty_or_missing_fields(self, mock_call):
        mock_call.side_effect = [
            '{"english": "", "spanish": "Título"}',
            '{"english": "Title"}',
        ]
        response = self.client.post("/getTitles", json={"text": "你好。"})
        self.assertEqual(response.status_code, 500)
        self.assertEqual(mock_call.call_count, 2)

    def test_get_titles_requires_text(self):
        response = self.client.post("/getTitles", json={})
        self.assertEqual(response.status_code, 400)

    # ————————————————————— /getDescriptions —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_get_descriptions_returns_backend_array_shape(self, mock_call):
        mock_call.return_value = '{"english": "A short story.", "spanish": "Una historia corta."}'
        response = self.client.post("/getDescriptions", json={"text": "我今天去学校。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["A short story.", "Una historia corta."])

    # ————————————————————— /getTranslations (full text) —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_get_translations_joins_sentences_with_periods(self, mock_call):
        mock_call.side_effect = [
            '{"english": "Hello", "spanish": "Hola"}',
            '{"english": "Thank you", "spanish": "Gracias"}',
        ]
        response = self.client.post("/getTranslations", json={"text": "你好。谢谢。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["Hello. Thank you.", "Hola. Gracias."])

    @patch("deepseekService.call_deepseek")
    def test_get_translations_failed_sentence_becomes_empty_part(self, mock_call):
        mock_call.side_effect = [
            '{"english": "Hello", "spanish": "Hola"}',
            "garbage", "garbage again",
        ]
        response = self.client.post("/getTranslations", json={"text": "你好。谢谢。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), ["Hello.", "Hola."])

    @patch("deepseekService.call_deepseek")
    def test_get_translations_splits_on_all_terminators_and_keeps_them(self, mock_call):
        mock_call.side_effect = [
            '{"english": "Hello", "spanish": "Hola"}',
            '{"english": "Thanks", "spanish": "Gracias"}',
        ]
        response = self.client.post("/getTranslations", json={"text": "你好！谢谢。"})
        self.assertEqual(response.status_code, 200)
        # ！ also closes a sentence, and each part keeps ITS terminator (no "!.").
        self.assertEqual(response.get_json(), ["Hello! Thanks.", "Hola! Gracias."])
        self.assertEqual(mock_call.call_count, 2)

    # ————————————————————— /translateSentences (1:1 alignment) —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_translate_sentences_keeps_alignment_on_failure(self, mock_call):
        mock_call.side_effect = [
            '{"english": "Hello", "spanish": "Hola"}',
            "garbage", "garbage again",
            '{"english": "Goodbye", "spanish": "Adiós"}',
        ]
        response = self.client.post("/translateSentences",
                                    json={"sentences": ["你好", "谢谢", "再见"]})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(),
                         [["Hello", "Hola"], ["", ""], ["Goodbye", "Adiós"]])
        self.assertEqual(mock_call.call_count, 4)

    @patch("deepseekService.call_deepseek")
    def test_translate_sentences_blank_input_skips_api_call(self, mock_call):
        mock_call.return_value = '{"english": "Hello", "spanish": "Hola"}'
        response = self.client.post("/translateSentences",
                                    json={"sentences": ["", "你好"]})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), [["", ""], ["Hello", "Hola"]])
        mock_call.assert_called_once()

    def test_translate_sentences_requires_list(self):
        response = self.client.post("/translateSentences", json={"sentences": "你好"})
        self.assertEqual(response.status_code, 400)

    # ————————————————————— /getMissingWords —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_missing_words_happy_path_preserves_input_order(self, mock_call):
        mock_call.return_value = (
            '{"words": ['
            '{"chinese": "谢谢", "pinyin": "xièxie", "english": "thanks", "spanish": "gracias"},'
            '{"chinese": "你好", "pinyin": "nǐ hǎo", "english": "hello", "spanish": "hola"}'
            ']}'
        )
        response = self.client.post("/getMissingWords", json={"words": ["你好", "谢谢"]})
        self.assertEqual(response.status_code, 200)
        result = response.get_json()
        self.assertEqual([w["chinese"] for w in result], ["你好", "谢谢"])
        self.assertEqual(result[0],
                         {"chinese": "你好", "pinyin": "nǐ hǎo", "english": "hello", "spanish": "hola"})

    @patch("deepseekService.call_deepseek")
    def test_missing_words_drops_hallucinated_and_invalid_entries(self, mock_call):
        mock_call.return_value = (
            '{"words": ['
            '{"chinese": "你好", "pinyin": "nǐ hǎo", "english": "hello", "spanish": "hola"},'
            '{"chinese": "谢谢", "pinyin": "", "english": "thanks", "spanish": "gracias"},'
            '{"chinese": "猫", "pinyin": "māo", "english": "cat", "spanish": "gato"}'
            ']}'
        )
        response = self.client.post("/getMissingWords", json={"words": ["你好", "谢谢"]})
        self.assertEqual(response.status_code, 200)
        result = response.get_json()
        # 谢谢 has an empty pinyin and 猫 was never requested: both dropped.
        self.assertEqual([w["chinese"] for w in result], ["你好"])

    @patch("deepseekService.call_deepseek")
    def test_missing_words_all_invalid_retries_then_500(self, mock_call):
        mock_call.side_effect = [
            '{"words": [{"chinese": "猫", "pinyin": "māo", "english": "cat", "spanish": "gato"}]}',
            '{"words": "not a list"}',
        ]
        response = self.client.post("/getMissingWords", json={"words": ["你好"]})
        self.assertEqual(response.status_code, 500)
        self.assertEqual(mock_call.call_count, 2)

    @patch("deepseekService.call_deepseek")
    def test_missing_words_empty_list_returns_empty_without_api_call(self, mock_call):
        response = self.client.post("/getMissingWords", json={"words": []})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), [])
        mock_call.assert_not_called()

    def test_missing_words_requires_list(self):
        response = self.client.post("/getMissingWords", json={"words": "你好"})
        self.assertEqual(response.status_code, 400)

    # ————————————————————— /generate —————————————————————

    @patch("deepseekService.call_deepseek")
    def test_generate_returns_clean_text(self, mock_call):
        mock_call.return_value = "我今天去学校。天气很好。"
        response = self.client.post("/generate", json={"level": "HSK1"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), {"text": "我今天去学校。天气很好。"})
        self.assertFalse(mock_call.call_args.kwargs.get("json_mode", False))

    @patch("deepseekService.call_deepseek")
    def test_generate_strips_fences_and_wrapping_quotes(self, mock_call):
        mock_call.return_value = '```\n"我今天去学校。"\n```'
        response = self.client.post("/generate", json={"level": "HSK1"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), {"text": "我今天去学校。"})

    @patch("deepseekService.call_deepseek")
    def test_generate_retries_when_no_chinese_then_500(self, mock_call):
        mock_call.side_effect = ["Sorry, I cannot do that.", "Here is some English text."]
        response = self.client.post("/generate", json={"level": "HSK1"})
        self.assertEqual(response.status_code, 500)
        self.assertIn("error", response.get_json())
        self.assertEqual(mock_call.call_count, 2)

    def test_generate_requires_level(self):
        response = self.client.post("/generate", json={"topic": "animals"})
        self.assertEqual(response.status_code, 400)

    # ————————————————————— /chatWord —————————————————————

    @patch("deepseekService.call_deepseek_chat")
    def test_chat_word_returns_reply(self, mock_chat):
        mock_chat.return_value = "你好 means hello."
        response = self.client.post("/chatWord", json={
            "word": "你好",
            "history": [{"role": "user", "content": "What does it mean?"}],
        })
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), {"reply": "你好 means hello."})

    @patch("deepseekService.call_deepseek_chat")
    def test_chat_word_empty_reply_is_500(self, mock_chat):
        mock_chat.return_value = ""
        response = self.client.post("/chatWord", json={
            "word": "你好",
            "history": [{"role": "user", "content": "What does it mean?"}],
        })
        self.assertEqual(response.status_code, 500)


if __name__ == "__main__":
    unittest.main()
