package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of DeepseekAiService for development and testing
 * purposes.
 * This service returns pre-defined responses instead of calling the actual
 * DeepSeek API.
 * 
 * To use this mock service, set the Spring profile to "dev" or "test".
 */
@Service
@Profile({ "dev", "test" })
@Primary
public class MockDeepseekAiService extends DeepseekAiService {

    @Override
    public Mono<AiResponse> generateExplanation(String word, String language) {
        // Create a mock response based on the input word
        AiResponse mockResponse = new AiResponse();

        // Generate some mock content based on the word
        String explanation = generateMockExplanation(word, language);
        String examples = generateMockExamples(word, language);

        mockResponse.setExplanation(explanation);
        mockResponse.setExamples(examples);

        return Mono.just(mockResponse);
    }

    private String generateMockExplanation(String word, String language) {
        return String.format(
                "This is a mock explanation for the %s word '%s'. In %s, this word is commonly used as an adjective to describe things or people. It is a simple word that children often learn early.",
                language, word, language);
    }

    private String generateMockExamples(String word, String language) {
        return String.format(
                "1. This is the first example sentence using the word '%s'.\n" +
                        "2. Here is a second example of how to use '%s' in a sentence.\n" +
                        "3. Finally, a third example showing '%s' in context.",
                word, word, word);
    }
}
