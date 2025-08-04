package com.example.backend.service;

import com.example.backend.model.TranslationRequest;
import com.example.backend.model.TranslationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TranslationService {

    private final WebClient webClient;

    @Value("${libretranslate.api.url}")
    private String libreTranslateApiUrl;

    @Value("${libretranslate.api.key:#{null}}")
    private String libreTranslateApiKey;

    public TranslationService() {
        this.webClient = WebClient.builder().build();
    }

    public Mono<String> translateText(String text, String sourceLanguage, String targetLanguage) {
        TranslationRequest request = new TranslationRequest();
        request.setText(text);
        request.setSource(sourceLanguage); // "ms" for Malay
        request.setTarget(targetLanguage); // "zh" for Simplified Mandarin
        request.setFormat("text");

        // Add API key if it's configured
        if (libreTranslateApiKey != null && !libreTranslateApiKey.isEmpty()
                && !libreTranslateApiKey.equals("your_libretranslate_api_key")) {
            request.setApiKey(libreTranslateApiKey);
        }

        // Call the LibreTranslate API
        return webClient.post()
                .uri(libreTranslateApiUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TranslationResponse.class)
                .map(TranslationResponse::getTranslatedText)
                .onErrorResume(e -> {
                    // Fallback to a simple placeholder on error
                    System.err.println("Translation API error: " + e.getMessage());
                    return Mono.just("[Translation Failed: " + e.getMessage() + "]");
                });
    }
}
