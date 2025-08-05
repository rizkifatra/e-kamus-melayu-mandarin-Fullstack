package com.example.backend.service;

import com.example.backend.model.AiResponse;
import com.example.backend.model.DictionaryResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DictionaryService {

    private final TranslationService translationService;
    private final DeepseekAiService deepseekAiService;

    public DictionaryService(TranslationService translationService,
            DeepseekAiService deepseekAiService) {
        this.translationService = translationService;
        this.deepseekAiService = deepseekAiService;
    }

    public Mono<DictionaryResponse> processWord(String malayWord) {
        System.out.println("Processing Malay word: " + malayWord);

        // Use LibreTranslate API for translation
        System.out.println("Translating word using LibreTranslate API: " + malayWord);
        return translationService.translateText(malayWord, "ms", "zh") // "ms" for Malay, "zh" for Simplified Mandarin
                .flatMap(mandarinWord -> deepseekAiService.generateExplanation(mandarinWord, "Mandarin")
                        .map(aiResponse -> {
                            DictionaryResponse response = new DictionaryResponse();
                            response.setMalayWord(malayWord);
                            response.setMandarinWord(mandarinWord);
                            response.setExplanation(aiResponse.getExplanation());
                            response.setExamples(aiResponse.getExamples());

                            // Add the pronunciation guide from the AI response
                            String pronunciation = aiResponse.getPronunciation();
                            if (pronunciation == null || pronunciation.isEmpty()) {
                                response.setPronunciationGuide("No pronunciation available");
                            } else {
                                response.setPronunciationGuide(pronunciation);
                            }
                            return response;
                        }))
                .onErrorResume(e -> {
                    System.err.println("Error processing word: " + malayWord + ", error: " + e.getMessage());

                    // Create an error response instead of throwing an exception
                    DictionaryResponse errorResponse = new DictionaryResponse();
                    errorResponse.setMalayWord(malayWord);
                    errorResponse.setMandarinWord("Translation failed");
                    errorResponse.setExplanation(
                            "Unable to translate this word. LibreTranslate API error: " + e.getMessage());
                    errorResponse.setExamples("No examples available");
                    errorResponse.setPronunciationGuide("No pronunciation available");

                    return Mono.just(errorResponse);
                });
    }
}
