package org.example.telegramfoodbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.telegramfoodbot.dto.FoodAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private static final String OPENAI_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private static final String SYSTEM_PROMPT = """
            Ти — експерт з харчування. Отримуєш фото страви.
            Твоє завдання:
            1. Визначити страву.
            2. Оцінити розмір посуду (стандартна тарілка ~26 см / десертна ~20 см / миска / стакан / інше).
            3. На основі розміру посуду та ступеня заповненості — оцінити масу порції в грамах.
            4. Розрахувати калорії, білки, жири, вуглеводи.

            Відповідай ТІЛЬКИ валідним JSON без markdown-огорток, без пояснень:
            {
              "dish": "назва страви українською",
              "assumedGrams": 350,
              "calories": 520,
              "proteinG": 30.0,
              "fatG": 22.0,
              "carbsG": 45.0,
              "confidence": "high",
              "portionNote": "стандартна тарілка ~26 см, заповнена на 2/3"
            }
            Якщо на фото немає їжі — поверни тільки: {"error":"not_food"}
            """;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.openai.key}")
    private String openAiKey;

    public FoodAnalysisResult analyze(byte[] imageBytes) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "openai/gpt-4o-mini");
        body.put("max_tokens", 500);

        ArrayNode messages = body.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");

        ObjectNode imageContent = content.addObject();
        imageContent.put("type", "image_url");
        imageContent.putObject("image_url")
                .put("url", "data:image/jpeg;base64," + base64Image);

        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + openAiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "FoodCalorieBot")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API error: HTTP " + response.code() + " — " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String jsonContent = root.at("/choices/0/message/content").asText().trim();
            log.debug("OpenAI response content: {}", jsonContent);
            return objectMapper.readValue(jsonContent, FoodAnalysisResult.class);
        }
    }

    public FoodAnalysisResult refine(FoodAnalysisResult previous, String userComment) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "openai/gpt-4o-mini");
        body.put("max_tokens", 500);

        ArrayNode messages = body.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content",
                "Ось попередній аналіз: " + objectMapper.writeValueAsString(previous) +
                ". Користувач уточнює: " + userComment +
                ". Перерахуй КБЖВ з урахуванням уточнення. Відповідай тільки валідним JSON.");

        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + openAiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "FoodCalorieBot")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenRouter refine error: HTTP " + response.code() + " — " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String jsonContent = root.at("/choices/0/message/content").asText().trim();
            log.debug("OpenRouter refine response: {}", jsonContent);
            return objectMapper.readValue(jsonContent, FoodAnalysisResult.class);
        }
    }
}
