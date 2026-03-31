package com.chinesereads.backend.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // ——— Flujo completo: genera texto + títulos + traducciones + descripciones + palabras faltantes ———

    public Map<String, Object> generateFullText(String level) throws Exception {
        // 1. Generar texto chino
        Map<String, String> generateRequest = Map.of("level", level);
        Map response = restTemplate.postForObject(aiServiceUrl + "/generate", generateRequest, Map.class);
        String chineseText = (String) response.get("text");

        return buildAiResult(chineseText);
    }

    public Map<String, Object> processOcrAndGenerate(MultipartFile image) throws Exception {
        // 1. Llamar al OCR
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
        ResponseEntity<Map> ocrResponse = restTemplate.exchange(
                ocrServiceUrl + "/ocr", HttpMethod.POST, requestEntity, Map.class);

        String chineseText = (String) ocrResponse.getBody().get("text");
        if (chineseText == null || chineseText.isBlank()) {
            throw new RuntimeException("OCR did not extract any text from the image");
        }

        return buildAiResult(chineseText);
    }

    private Map<String, Object> buildAiResult(String chineseText) {
        Map<String, Object> result = new HashMap<>();
        result.put("chineseText", chineseText);

        // 2. Obtener títulos
        try {
            Map<String, String> req = Map.of("text", chineseText);
            List<String> titles = restTemplate.postForObject(
                    aiServiceUrl + "/getTitles", req,
                    new ArrayList<String>().getClass());
            result.put("titleEnglish", titles != null && titles.size() > 0 ? titles.get(0) : "");
            result.put("titleSpanish", titles != null && titles.size() > 1 ? titles.get(1) : "");
        } catch (Exception e) {
            result.put("titleEnglish", "");
            result.put("titleSpanish", "");
        }

        // 3. Obtener traducciones
        try {
            Map<String, String> req = Map.of("text", chineseText);
            List<String> translations = restTemplate.postForObject(
                    aiServiceUrl + "/getTranslations", req,
                    new ArrayList<String>().getClass());
            result.put("englishTranslation", translations != null && translations.size() > 0 ? translations.get(0) : "");
            result.put("spanishTranslation", translations != null && translations.size() > 1 ? translations.get(1) : "");
        } catch (Exception e) {
            result.put("englishTranslation", "");
            result.put("spanishTranslation", "");
        }

        // 4. Obtener descripciones
        try {
            Map<String, String> req = Map.of("text", chineseText);
            List<String> descriptions = restTemplate.postForObject(
                    aiServiceUrl + "/getDescriptions", req,
                    new ArrayList<String>().getClass());
            result.put("englishDescription", descriptions != null && descriptions.size() > 0 ? descriptions.get(0) : "");
            result.put("spanishDescription", descriptions != null && descriptions.size() > 1 ? descriptions.get(1) : "");
        } catch (Exception e) {
            result.put("englishDescription", "");
            result.put("spanishDescription", "");
        }

        // 5. Detectar palabras faltantes en el diccionario
        List<String> segments = jiebaService.segment(chineseText);
        List<String> missingWords = segments.stream()
                .distinct()
                .filter(w -> !w.isBlank())
                .filter(w -> wordRepository.findByChinese(w).isEmpty())
                .toList();

        result.put("missingWords", missingWords);

        // 6. Si hay palabras faltantes, pedir a la IA que las traduzca
        if (!missingWords.isEmpty()) {
            try {
                Map<String, Object> req = Map.of("words", missingWords);
                List<Map<String, String>> wordTranslations = restTemplate.postForObject(
                        aiServiceUrl + "/getMissingWords", req,
                        new ArrayList<Map>().getClass());
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