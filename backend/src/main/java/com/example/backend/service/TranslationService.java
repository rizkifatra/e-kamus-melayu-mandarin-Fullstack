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

        // Add API key if it's configured and not empty
        if (libreTranslateApiKey != null && !libreTranslateApiKey.isEmpty()
                && !libreTranslateApiKey.equals("your_libretranslate_api_key")) {
            request.setApiKey(libreTranslateApiKey);
        }

        System.out.println("Calling LibreTranslate API at: " + libreTranslateApiUrl);
        System.out.println("Translating: '" + text + "' from " + sourceLanguage + " to " + targetLanguage);

        // Call the LibreTranslate API
        return webClient.post()
                .uri(libreTranslateApiUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TranslationResponse.class)
                .map(response -> {
                    String translated = response.getTranslatedText();
                    System.out.println("Translation successful: '" + text + "' → '" + translated + "'");
                    return translated;
                })
                .onErrorResume(e -> {
                    // Provide more detailed error information
                    System.err.println("Translation API error: " + e.getMessage());
                    return Mono.error(new RuntimeException("LibreTranslate API error: " + e.getMessage()));
                });
    }
}
