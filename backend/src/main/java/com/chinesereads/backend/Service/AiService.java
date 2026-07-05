package com.chinesereads.backend.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Repository.WordRepository;

@Service
public class AiService {

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    @Value("${ocr.service.url:http://localhost:5000}")
    private String ocrServiceUrl;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private JiebaService jiebaService;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> generateFullText(String level, String topic) throws Exception {
        Map<String, String> generateRequest = new HashMap<>();
        generateRequest.put("level", level);
        generateRequest.put("topic", topic);

        Map<String, Object> response = restTemplate.exchange(
                aiServiceUrl + "/generate",
                HttpMethod.POST,
                new HttpEntity<>(generateRequest),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        String chineseText = (String) response.get("text");
        return buildAiResult(chineseText);
    }

    public Map<String, Object> processOcrAndGenerate(MultipartFile image) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> ocrResponse = restTemplate.exchange(
                ocrServiceUrl + "/ocr",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        String chineseText = (String) ocrResponse.getBody().get("text");
        if (chineseText == null || chineseText.isBlank()) {
            throw new RuntimeException("OCR did not extract any text from the image");
        }

        return buildAiResult(chineseText);
    }

    // ——————————— Reusable building blocks (also used for private user texts) ———————————

    /** Runs OCR on an image and returns the extracted Chinese text (no AI generation). */
    public String ocrImageToText(MultipartFile image) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename();
            }
        });

        ResponseEntity<Map<String, Object>> ocrResponse = restTemplate.exchange(
                ocrServiceUrl + "/ocr",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return (String) ocrResponse.getBody().get("text");
    }

    /** [englishTitle, spanishTitle] for the given Chinese text ("" on failure). */
    public List<String> getTitles(String chineseText) {
        try {
            return restTemplate.exchange(
                    aiServiceUrl + "/getTitles",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", chineseText)),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** [englishTranslation, spanishTranslation] for the given Chinese text ("" on failure). */
    public List<String> getTranslations(String chineseText) {
        try {
            return restTemplate.exchange(
                    aiServiceUrl + "/getTranslations",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", chineseText)),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** [{chinese,pinyin,english,spanish}, ...] definitions for a list of words ([] on failure). */
    public List<Map<String, String>> getWordDefinitions(List<String> words) {
        if (words == null || words.isEmpty()) {
            return List.of();
        }
        try {
            return restTemplate.exchange(
                    aiServiceUrl + "/getMissingWords",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("words", words)),
                    new ParameterizedTypeReference<List<Map<String, String>>>() {}
            ).getBody();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> buildAiResult(String chineseText) {
        Map<String, Object> result = new HashMap<>();
        result.put("chineseText", chineseText);

        // Títulos
        try {
            List<String> titles = restTemplate.exchange(
                    aiServiceUrl + "/getTitles",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", chineseText)),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
            result.put("titleEnglish", titles != null && titles.size() > 0 ? titles.get(0) : "");
            result.put("titleSpanish", titles != null && titles.size() > 1 ? titles.get(1) : "");
        } catch (Exception e) {
            result.put("titleEnglish", "");
            result.put("titleSpanish", "");
        }

        // Traducciones
        try {
            List<String> translations = restTemplate.exchange(
                    aiServiceUrl + "/getTranslations",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", chineseText)),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
            result.put("englishTranslation", translations != null && translations.size() > 0 ? translations.get(0) : "");
            result.put("spanishTranslation", translations != null && translations.size() > 1 ? translations.get(1) : "");
        } catch (Exception e) {
            result.put("englishTranslation", "");
            result.put("spanishTranslation", "");
        }

        // Descripciones
        try {
            List<String> descriptions = restTemplate.exchange(
                    aiServiceUrl + "/getDescriptions",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", chineseText)),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
            result.put("englishDescription", descriptions != null && descriptions.size() > 0 ? descriptions.get(0) : "");
            result.put("spanishDescription", descriptions != null && descriptions.size() > 1 ? descriptions.get(1) : "");
        } catch (Exception e) {
            result.put("englishDescription", "");
            result.put("spanishDescription", "");
        }

        // Palabras faltantes
        List<String> segments = jiebaService.segment(chineseText);
        List<String> missingWords = segments.stream()
                .distinct()
                .filter(w -> !w.isBlank())
                .filter(w -> wordRepository.findByChinese(w).isEmpty())
                .toList();

        result.put("missingWords", missingWords);

        if (!missingWords.isEmpty()) {
            try {
                List<Map<String, String>> wordTranslations = restTemplate.exchange(
                        aiServiceUrl + "/getMissingWords",
                        HttpMethod.POST,
                        new HttpEntity<>(Map.of("words", missingWords)),
                        new ParameterizedTypeReference<List<Map<String, String>>>() {}
                ).getBody();
                result.put("missingWordSuggestions", wordTranslations);
            } catch (Exception e) {
                result.put("missingWordSuggestions", List.of());
            }
        } else {
            result.put("missingWordSuggestions", List.of());
        }

        return result;
    }
}