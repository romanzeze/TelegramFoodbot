package org.example.telegramfoodbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.telegramfoodbot.config.BotConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramFileService {

    private static final String TG_API = "https://api.telegram.org";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BotConfig botConfig;

    public byte[] downloadPhotoBytes(String fileId) throws IOException {
        String filePath = resolveFilePath(fileId);
        String downloadUrl = TG_API + "/file/bot" + botConfig.token() + "/" + filePath;
        log.debug("Downloading photo from {}", downloadUrl);

        Request request = new Request.Builder().url(downloadUrl).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Photo download failed: HTTP " + response.code());
            }
            return response.body().bytes();
        }
    }

    private String resolveFilePath(String fileId) throws IOException {
        String url = TG_API + "/bot" + botConfig.token() + "/getFile?file_id=" + fileId;
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("getFile failed: HTTP " + response.code());
            }
            JsonNode root = objectMapper.readTree(response.body().string());
            return root.at("/result/file_path").asText();
        }
    }
}
