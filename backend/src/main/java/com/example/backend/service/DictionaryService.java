package com.example.backend.service;

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
        System.out.println("Using LibreTranslate API for: " + malayWord);
        return translationService.translateText(malayWord, "ms", "zh") // "ms" for Malay, "zh" for Simplified Mandarin
                .flatMap(mandarinWord -> {
                    System.out.println("Translation successful: '" + malayWord + "' → '" + mandarinWord + "'");
                    System.out.println("Calling DeepseekAiService for '" + mandarinWord + "'");

                    // Now use DeepseekAi to get detailed information about the word
                    return deepseekAiService.generateExplanation(mandarinWord, "Mandarin")
                            .doOnNext(aiResponse -> {
                                System.out.println("DeepseekAi response received:");
                                System.out.println("- Explanation: " + aiResponse.getExplanation());
                                System.out.println("- Examples: " + aiResponse.getExamples());
                                System.out.println("- Pronunciation: " + aiResponse.getPronunciation());
                                System.out.println("- Is Adjective: " + aiResponse.isAdjective());
                            })
                            .map(aiResponse -> {
                                DictionaryResponse response = new DictionaryResponse();
                                response.setMalayWord(malayWord);
                                response.setMandarinWord(mandarinWord);
                                response.setExplanation(aiResponse.getExplanation());
                                response.setExamples(aiResponse.getExamples());

                                // Add the pinyin pronunciation from the AI response
                                String pronunciation = aiResponse.getPronunciation();
                                if (pronunciation == null || pronunciation.isEmpty()) {
                                    response.setPinyin("No pronunciation available");
                                } else {
                                    response.setPinyin(pronunciation);
                                }

                                // Set if the word is an adjective
                                response.setAdjective(aiResponse.isAdjective());

                                return response;
                            });
                })
                .onErrorResume(e -> {
                    System.err.println("Error processing word: " + malayWord + ", error: " + e.getMessage());

                    // Create an error response instead of throwing an exception
                    DictionaryResponse errorResponse = new DictionaryResponse();
                    errorResponse.setMalayWord(malayWord);
                    errorResponse.setMandarinWord("Translation failed");
                    errorResponse.setExplanation(
                            "Unable to translate this word. LibreTranslate API error: " + e.getMessage());
                    errorResponse.setExamples("No examples available");
                    errorResponse.setPinyin("No pronunciation available");
                    errorResponse.setAdjective(false);

                    return Mono.just(errorResponse);
                });
    }
}
