package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("dev")
public class DeepseekAiService {

    private final WebClient webClient;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${ollama.temperature:0.3}")
    private double temperature;

    @Value("${ollama.max_tokens:300}")
    private int maxTokens;

    public DeepseekAiService(WebClient webClient) {
        this.webClient = webClient;
        System.out.println("DeepseekAiService initialized with WebClient: " + webClient);
    }

    public Mono<AiResponse> generateExplanation(String word, String language) {
        System.out.println("Generating explanation for: " + word + " in " + language);
        String prompt = generatePrompt(word, language);

        System.out.println("Sending request to DeepSeek API for word: " + word);
        System.out.println("API URL: " + deepseekApiUrl);

        // Use the model name from application.properties via deepseekApiKey
        // This allows us to easily change the model without changing the code
        String modelName = deepseekApiKey.equals("not-needed-for-ollama") ? "gpt-oss:20b" : deepseekApiKey;
        System.out.println("Using model: " + modelName);

        // For Ollama completions API - using the direct completion endpoint format with
        // optimized parameters
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("system",
                "You are a language expert. Always respond directly with the final answer in the exact format requested. Never use <think> tags or show your internal reasoning process.");

        // Using the API URL as specified in properties
        String apiUrl = deepseekApiUrl;

        System.out.println("Final API URL: " + apiUrl);

        System.out.println("Sending request to: " + apiUrl);
        System.out.println("Request body: " + requestBody);

        return webClient.post()
                .uri(apiUrl)
                // No authorization header needed for local Ollama
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                // Add a timeout specifically for this request
                .timeout(java.time.Duration.ofMinutes(5), Mono.fromCallable(() -> {
                    System.out.println("Request to Ollama API timed out after 5 minutes");
                    throw new RuntimeException("Request to Ollama API timed out. LLM inference may require more time.");
                }))
                .doOnNext(response -> {
                    System.out.println("Received DeepSeek API response: " + response);
                })
                .doOnError(error -> {
                    System.err.println("Error during API call to " + apiUrl + ": " + error.getMessage());
                    if (error.getCause() != null) {
                        System.err.println("Caused by: " + error.getCause().getMessage());
                    }
                    error.printStackTrace();
                })
                .map(response -> {
                    try {
                        // Log the full response structure for debugging
                        System.out.println("Full API response structure: " + response.keySet());

                        String generatedText = null;

                        // Check for response format: Ollama v0.1.x format
                        if (response.containsKey("response")) {
                            generatedText = (String) response.get("response");
                        }
                        // Check for response format: Ollama chat completion format
                        else if (response.containsKey("message")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMessage = (Map<String, Object>) response.get("message");
                            if (responseMessage != null) {
                                generatedText = (String) responseMessage.get("content");
                            }
                        }
                        // Check for response format: Older Ollama completion format
                        else if (response.containsKey("content")) {
                            generatedText = (String) response.get("content");
                        }

                        // Check if response is empty
                        if ((generatedText == null || generatedText.isEmpty()) && response.containsKey("done_reason")) {
                            String doneReason = (String) response.get("done_reason");
                            if ("load".equals(doneReason)) {
                                // The model is still loading
                                throw new RuntimeException("The model '" + modelName
                                        + "' is still loading. Please try again in a few moments.");
                            } else {
                                throw new RuntimeException("No content returned from API. Reason: " + doneReason
                                        + ". Response: " + response);
                            }
                        }

                        if (generatedText == null || generatedText.isEmpty()) {
                            throw new RuntimeException("No content found in API response: " + response);
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
                    System.err.println("Error class: " + e.getClass().getName());

                    // Check for connection issues
                    if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Failed to connect")) {
                        System.err.println("Connection refused: Make sure Ollama container is running at " + apiUrl);
                    }

                    // Print full stack trace for debugging
                    e.printStackTrace();

                    AiResponse fallback = new AiResponse();
                    fallback.setExplanation(
                            "Could not connect to DeepSeek AI service at " + apiUrl + ". Error: " + e.getMessage());
                    fallback.setExamples(
                            "No examples available due to connection error. Check if Ollama is running with the model loaded.");
                    fallback.setPronunciation(getPinyinFallback(word));
                    fallback.setAdjective(false);
                    return Mono.just(fallback);
                });
    }

    private String generatePrompt(String word, String language) {
        return String.format(
                "You are a language expert teaching Simplified Mandarin Chinese.\n\n" +
                        "DO NOT USE <think> TAGS OR INTERNAL DELIBERATION. RESPOND IMMEDIATELY WITH THE FINAL ANSWER.\n\n"+
                        "Use simple styling of the text response (dont bold,italic,etc) " +
                        "Please provide a comprehensive explanation of the %s word '%s'.\n" +
                        "The response must be clear, structured, and follow the exact format below:\n\n" +
                        "1. A simple explanation of the word's meaning in Malay (Bahasa Malaysia).\n" +
                        "2. The accurate pinyin pronunciation only from word that i gave with tone marks (e.g., 'hǎo', not 'hao3').\n" +
                        "3. Three example sentences using this word in real context. Each should include:\n" +
                        "   - The original sentence in Chinese\n" +
                        "   - Its translation in Malay\n" +
                        "4. State whether this word is an adjective in Chinese grammar (answer with YES or NO).\n\n" +
                        "Use the following EXACT section headers in your response:\n\n" +
                        "EXPLANATION:\n[your simple explanation in Malay]\n\n" +
                        "PRONUNCIATION:\n[pinyin with tone marks only]\n\n" +
                        "EXAMPLES:\n" +
                        "1. [Chinese sentence]\n   [Malay translation]\n" +
                        "2. [Chinese sentence]\n   [Malay translation]\n" +
                        "3. [Chinese sentence]\n   [Malay translation]\n\n" +
                        "IS_ADJECTIVE:\n[YES or NO]",
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
