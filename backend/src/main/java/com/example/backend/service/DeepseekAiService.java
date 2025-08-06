package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Profile("dev")
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
        System.out.println("Generating explanation for: " + word + " in " + language);
        String prompt = generatePrompt(word, language);

        System.out.println("Sending request to DeepSeek API for word: " + word);
        System.out.println("API URL: " + deepseekApiUrl);

        // Create request body in chat format for Ollama API
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt);

        // Use the model name from application.properties via deepseekApiKey
        // This allows us to easily change the model without changing the code
        String modelName = deepseekApiKey.equals("not-needed-for-ollama") ? "deepseek" : deepseekApiKey;
        System.out.println("Using model: " + modelName);

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", java.util.List.of(message),
                "stream", false,
                "temperature", 0.7,
                "max_tokens", 500);

        return webClient.post()
                .uri(deepseekApiUrl)
                // No authorization header needed for local Ollama
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> {
                    System.out.println("Received DeepSeek API response");
                })
                .map(response -> {
                    try {
                        // Parse the Ollama response and extract the generated text
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMessage = (Map<String, Object>) response.get("message");

                        if (responseMessage == null) {
                            throw new RuntimeException("No message found in API response");
                        }

                        String generatedText = (String) responseMessage.get("content");

                        if (generatedText == null || generatedText.isEmpty()) {
                            throw new RuntimeException("No content found in API response");
                        }

                        System.out.println("Generated text from DeepSeek: " + generatedText);

                        // Parse the generated text to extract explanation and examples
                        return parseGeneratedText(generatedText);
                    } catch (Exception e) {
                        System.err.println("Error parsing DeepSeek response: " + e.getMessage());
                        e.printStackTrace();

                        // Create a fallback response with error information
                        AiResponse fallback = new AiResponse();
                        fallback.setExplanation(
                                "Unable to generate explanation at this time. Error: " + e.getMessage());
                        fallback.setExamples("No examples available due to API error.");
                        fallback.setPronunciation(getPinyinFallback(word));
                        fallback.setAdjective(false);
                        return fallback;
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("DeepSeek API error: " + e.getMessage());
                    e.printStackTrace();

                    AiResponse fallback = new AiResponse();
                    fallback.setExplanation("Could not connect to DeepSeek AI service. Error: " + e.getMessage());
                    fallback.setExamples("No examples available due to connection error.");
                    fallback.setPronunciation(getPinyinFallback(word));
                    fallback.setAdjective(false);
                    return Mono.just(fallback);
                });
    }

    private String generatePrompt(String word, String language) {
        return String.format(
                "You are a language expert teaching Mandarin Chinese. " +
                        "Please provide a comprehensive explanation of the %s word '%s'. " +
                        "I need the following information in a clear, structured format:\n\n" +
                        "1. A detailed explanation of what this word means in English.\n" +
                        "2. The exact pinyin pronunciation with tone marks (e.g., 'hǎo' not 'hao3').\n" +
                        "3. Three example sentences using this word in context (provide both Chinese characters and English translation).\n"
                        +
                        "4. Determine if this word functions as an adjective in Chinese grammar (YES or NO).\n\n" +
                        "Format your response with these EXACT section headings:\n" +
                        "EXPLANATION: [your detailed explanation here]\n" +
                        "PRONUNCIATION: [pinyin with tone marks only, no Chinese characters]\n" +
                        "EXAMPLES: [three numbered examples with Chinese and English translations]\n" +
                        "IS_ADJECTIVE: [answer only YES or NO]",
                language, word);
    }

    private String getPinyinFallback(String word) {
        return "Pinyin unavailable for '" + word + "'";
    }

    private AiResponse parseGeneratedText(String text) {
        AiResponse response = new AiResponse();

        try {
            // Parse explanation
            int explanationStart = text.indexOf("EXPLANATION:");
            int examplesStart = text.indexOf("EXAMPLES:");
            int pronunciationStart = text.indexOf("PRONUNCIATION:");
            int isAdjectiveStart = text.indexOf("IS_ADJECTIVE:");

            if (explanationStart >= 0) {
                String explanationEndPoint = findEndPoint(text, explanationStart,
                        new String[] { "PRONUNCIATION:", "EXAMPLES:", "IS_ADJECTIVE:" });
                if (explanationEndPoint != null) {
                    String explanationText = text.substring(explanationStart + "EXPLANATION:".length(),
                            text.indexOf(explanationEndPoint)).trim();
                    response.setExplanation(explanationText);
                } else if (text.length() > explanationStart + "EXPLANATION:".length()) {
                    response.setExplanation(text.substring(explanationStart + "EXPLANATION:".length()).trim());
                } else {
                    response.setExplanation("No explanation available.");
                }
            } else {
                response.setExplanation("No explanation section found in API response.");
            }

            // Parse examples
            if (examplesStart >= 0) {
                String examplesEndPoint = findEndPoint(text, examplesStart,
                        new String[] { "EXPLANATION:", "PRONUNCIATION:", "IS_ADJECTIVE:" });
                if (examplesEndPoint != null) {
                    String examplesText = text.substring(examplesStart + "EXAMPLES:".length(),
                            text.indexOf(examplesEndPoint)).trim();
                    response.setExamples(examplesText);
                } else if (text.length() > examplesStart + "EXAMPLES:".length()) {
                    response.setExamples(text.substring(examplesStart + "EXAMPLES:".length()).trim());
                } else {
                    response.setExamples("No examples available.");
                }
            } else {
                response.setExamples("No examples section found in API response.");
            }

            // Parse pronunciation
            if (pronunciationStart >= 0) {
                String pronunciationEndPoint = findEndPoint(text, pronunciationStart,
                        new String[] { "EXPLANATION:", "EXAMPLES:", "IS_ADJECTIVE:" });
                if (pronunciationEndPoint != null) {
                    String pronunciationText = text.substring(pronunciationStart + "PRONUNCIATION:".length(),
                            text.indexOf(pronunciationEndPoint)).trim();
                    response.setPronunciation(pronunciationText);
                } else if (text.length() > pronunciationStart + "PRONUNCIATION:".length()) {
                    response.setPronunciation(text.substring(pronunciationStart + "PRONUNCIATION:".length()).trim());
                } else {
                    response.setPronunciation("No pronunciation available.");
                }
            } else {
                response.setPronunciation("No pronunciation section found in API response.");
            }

            // Parse adjective information
            if (isAdjectiveStart >= 0) {
                String adjectiveText = text.substring(isAdjectiveStart + "IS_ADJECTIVE:".length()).trim().toUpperCase();
                response.setAdjective(adjectiveText.contains("YES"));
            } else {
                response.setAdjective(false);
            }
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            e.printStackTrace();
            response.setExplanation("Error parsing explanation: " + e.getMessage());
            response.setExamples("Error parsing examples.");
            response.setPronunciation("Error parsing pronunciation.");
            response.setAdjective(false);
        }

        return response;
    }

    private String findEndPoint(String text, int startAfter, String[] possibleEndPoints) {
        int earliestEndPoint = Integer.MAX_VALUE;
        String result = null;

        for (String endPoint : possibleEndPoints) {
            int endPointIndex = text.indexOf(endPoint, startAfter + 1);
            if (endPointIndex != -1 && endPointIndex < earliestEndPoint) {
                earliestEndPoint = endPointIndex;
                result = endPoint;
            }
        }

        return result;
    }

    // These methods have been removed as they are no longer used
    // We're now using predefined responses and mock responses instead of API calls
}
