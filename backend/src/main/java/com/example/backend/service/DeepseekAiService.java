package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class DeepseekAiService {

    private final WebClient webClient;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    public DeepseekAiService() {
        this.webClient = WebClient.builder().build();
    }

    public Mono<AiResponse> generateExplanation(String word, String language) {
        String prompt = generatePrompt(word, language);

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-r1-7b",
                "prompt", prompt,
                "temperature", 0.7,
                "max_tokens", 500);

        return webClient.post()
                .uri(deepseekApiUrl)
                .header("Authorization", "Bearer " + deepseekApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        // Parse the DeepSeek response and extract the generated text
                        Map<String, Object> choices = (Map<String, Object>) ((java.util.List<?>) response
                                .get("choices"))
                                .get(0);
                        String generatedText = (String) choices.get("text");

                        // Parse the generated text to extract explanation and examples
                        return parseGeneratedText(generatedText);
                    } catch (Exception e) {
                        System.err.println("Error parsing DeepSeek response: " + e.getMessage());
                        AiResponse fallback = new AiResponse();
                        fallback.setExplanation("Unable to generate explanation at this time.");
                        fallback.setExamples("No examples available.");
                        return fallback;
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("DeepSeek API error: " + e.getMessage());
                    AiResponse fallback = new AiResponse();
                    fallback.setExplanation("API call failed. Please try again later.");
                    fallback.setExamples("No examples available due to API error.");
                    return Mono.just(fallback);
                });
    }

    private String generatePrompt(String word, String language) {
        return String.format(
                "You are a language tutor for children learning %s. " +
                        "Please provide a simple explanation of the %s word '%s' and give 3 easy examples of how to use it in sentences. "
                        +
                        "Make sure your explanation is suitable for children and beginners. " +
                        "Include the pinyin pronunciation for the word. " +
                        "Format your response with three sections: EXPLANATION, EXAMPLES, and PRONUNCIATION.",
                language, language, word);
    }

    private AiResponse parseGeneratedText(String text) {
        AiResponse response = new AiResponse();

        try {
            // Parse explanation
            int explanationStart = text.indexOf("EXPLANATION:");
            int examplesStart = text.indexOf("EXAMPLES:");
            int pronunciationStart = text.indexOf("PRONUNCIATION:");

            if (explanationStart >= 0 && examplesStart > explanationStart) {
                String explanationText = text.substring(explanationStart + "EXPLANATION:".length(), examplesStart)
                        .trim();
                response.setExplanation(explanationText);
            } else {
                response.setExplanation("No explanation available.");
            }

            // Parse examples
            if (examplesStart >= 0) {
                String examplesText;
                if (pronunciationStart > examplesStart) {
                    examplesText = text.substring(examplesStart + "EXAMPLES:".length(), pronunciationStart).trim();
                } else {
                    examplesText = text.substring(examplesStart + "EXAMPLES:".length()).trim();
                }
                response.setExamples(examplesText);
            } else {
                response.setExamples("No examples available.");
            }

            // Parse pronunciation (will be handled in DictionaryService)
            if (pronunciationStart >= 0) {
                String pronunciationText = text.substring(pronunciationStart + "PRONUNCIATION:".length()).trim();
                // Store pronunciation in examples for now, will be extracted in
                // DictionaryService
                response.setPronunciation(pronunciationText);
            }
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            response.setExplanation("Error parsing explanation.");
            response.setExamples("Error parsing examples.");
        }

        return response;
    }
}
