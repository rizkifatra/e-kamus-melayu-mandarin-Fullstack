package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * A simplified implementation of DeepseekAiService that doesn't rely on the
 * actual API.
 * This is used for production when we want to focus only on the translation
 * feature.
 */
@Service
@Profile("prod")
@Primary
public class SimpleDeepseekAiService extends DeepseekAiService {

    @Override
    public Mono<AiResponse> generateExplanation(String word, String language) {
        System.out.println("Using simplified AI explanations for: " + word);

        AiResponse response = new AiResponse();

        // Create a simple explanation
        response.setExplanation(
                "This is the " + language + " word \"" + word + "\". " +
                        "This translation was provided by the LibreTranslate API.");

        // Create example sentences
        response.setExamples(
                "Example 1: A simple sentence using \"" + word + "\".\n" +
                        "Example 2: Another context with \"" + word + "\".");

        // Set a pronunciation guide
        response.setPronunciation(getPronunciation(word));

        return Mono.just(response);
    }

    /**
     * Generates a simple pronunciation guide
     */
    private String getPronunciation(String word) {
        if (word == null || word.isEmpty()) {
            return "No pronunciation available";
        }

        // Just return the word with tone marks for illustration
        return word + " (tones vary)";
    }
}
