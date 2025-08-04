package com.example.backend.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("dev")
@Primary
public class MockTranslationService extends TranslationService {

    private final Map<String, String> mockTranslations = new HashMap<>();

    public MockTranslationService() {
        super();
        // Initialize with some common Malay to Mandarin translations
        mockTranslations.put("test", "测试");
        mockTranslations.put("baik", "好");
        mockTranslations.put("cantik", "漂亮");
        mockTranslations.put("cepat", "快");
        mockTranslations.put("besar", "大");
        mockTranslations.put("kecil", "小");
        mockTranslations.put("panas", "热");
        mockTranslations.put("sejuk", "冷");
        mockTranslations.put("tinggi", "高");
        mockTranslations.put("rendah", "低");
    }

    @Override
    public Mono<String> translateText(String text, String sourceLanguage, String targetLanguage) {
        // Check if we have a mock translation for this word
        String translation = mockTranslations.getOrDefault(text.toLowerCase(), "无翻译");
        return Mono.just(translation);
    }
}
