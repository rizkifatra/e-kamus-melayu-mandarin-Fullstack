package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile("dev")
public class DeepseekAiService {

    private static final Logger logger = LoggerFactory.getLogger(DeepseekAiService.class);
    private final WebClient webClient;
    private final Map<String, AiResponse> cache = new ConcurrentHashMap<>();

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${ollama.temperature:0.3}")
    private double temperature;

    @Value("${ollama.max_tokens:300}")
    private int maxTokens;

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    public DeepseekAiService(WebClient webClient) {
        this.webClient = webClient;
        logger.info("DeepseekAiService initialized with WebClient: {}", webClient);
    }

    public Mono<AiResponse> generateExplanation(String word, String language) {
        String cacheKey = language + ":" + word;
        logger.debug("Generating explanation for: {} in {}", word, language);

        // Check if cache is enabled and if we have this word in our cache
        if (cacheEnabled && cache.containsKey(cacheKey)) {
            logger.info("Cache hit for word '{}' in {}", word, language);
            return Mono.just(cache.get(cacheKey));
        }

        logger.info("Cache miss for word '{}' in {} - calling DeepSeek API", word, language);
        String prompt = generatePrompt(word, language);

        logger.debug("Sending request to DeepSeek API for word: {}", word);
        logger.debug("API URL: {}", deepseekApiUrl);

        // Use the model name from application.properties via deepseekApiKey
        // This allows us to easily change the model without changing the code
        String modelName = deepseekApiKey.equals("not-needed-for-ollama") ? "gpt-oss:20b" : deepseekApiKey;
        logger.debug("Using model: {}", modelName);

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

        logger.debug("Final API URL: {}", apiUrl);
        logger.debug("Sending request to: {}", apiUrl);
        logger.debug("Request body: {}", requestBody);

        return webClient.post()
                .uri(apiUrl)
                // No authorization header needed for local Ollama
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                // Add a timeout specifically for this request
                .timeout(java.time.Duration.ofMinutes(5), Mono.fromCallable(() -> {
                    logger.error("Request to Ollama API timed out after 5 minutes");
                    throw new RuntimeException("Request to Ollama API timed out. LLM inference may require more time.");
                }))
                .doOnNext(response -> {
                    logger.debug("Received DeepSeek API response: {}", response);
                })
                .doOnError(error -> {
                    logger.error("Error during API call to {}: {}", apiUrl, error.getMessage());
                    if (error.getCause() != null) {
                        logger.error("Caused by: {}", error.getCause().getMessage());
                    }
                    logger.error("Stack trace:", error);
                })
                .map(response -> {
                    try {
                        // Log the full response structure for debugging
                        logger.debug("Full API response structure: {}", response.keySet());

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

                        logger.debug("Generated text from DeepSeek: {}", generatedText);

                        // Parse the generated text to extract explanation and examples
                        AiResponse aiResponse = parseGeneratedText(generatedText);

                        // Store in cache for future requests if caching is enabled
                        if (cacheEnabled) {
                            cache.put(language + ":" + word, aiResponse);
                            logger.info("Cached response for '{}' in {}", word, language);
                        }

                        return aiResponse;
                    } catch (Exception e) {
                        logger.error("Error parsing DeepSeek response: {}", e.getMessage(), e);

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
                    logger.error("DeepSeek API error: {} ({})", e.getMessage(), e.getClass().getName(), e);

                    // Check for connection issues
                    if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Failed to connect")) {
                        logger.error("Connection refused: Make sure Ollama container is running at {}", apiUrl);
                    }

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
                "You are a language expert teaching Simplified Mandarin Chinese who teaches Chinese and needs accurate linguistic details for practical use.\n\n"
                        +
                        "DO NOT USE <think> TAGS OR INTERNAL DELIBERATION. RESPOND IMMEDIATELY WITH THE FINAL ANSWER.\n\n"
                        +
                        "Use simple styling of the text response (dont bold,italic,etc) " +
                        "Please provide a comprehensive explanation of the %s word '%s'.\n" +
                        "The response must be clear, structured, and follow the exact format below:\n\n" +
                        "1. A simple explanation of the word's meaning in Malay (Bahasa Malaysia).\n" +
                        "2. The accurate pinyin pronunciation only from word that i gave with tone marks (e.g., 'hǎo', not 'hao3').\n"
                        +
                        "3. Three example sentences using this word in real context. Each should include:\n" +
                        "   - The original sentence in Chinese\n" +
                        "   - Its translation in Malay\n" +
                        "4. State whether this word is an adjective in Chinese grammar (answer with YES or NO).\n\n" +
                        "Use the following EXACT section headers in your response:\n\n" +
                        "EXPLANATION:\n[your simple explanation in Malay Language]\n\n" +
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
        logger.debug("Parsing generated text of length: {}", text.length());

        try {
            // Remove any <think> tags that might be in the response
            text = text.replaceAll("(?s)<think>.*?</think>", "").trim();

            // Define the sections we want to extract
            Map<String, String> sections = extractSections(text);

            // Set the extracted values in the response object
            response.setExplanation(sections.getOrDefault("EXPLANATION", "No explanation available."));
            response.setPronunciation(sections.getOrDefault("PRONUNCIATION", "No pronunciation available."));
            response.setExamples(sections.getOrDefault("EXAMPLES", "No examples available."));

            // Handle adjective field
            String isAdjectiveText = sections.getOrDefault("IS_ADJECTIVE", "NO").trim().toUpperCase();
            response.setAdjective(isAdjectiveText.contains("YES"));

            logger.debug("Parsed sections - Explanation: {}, Pronunciation: {}, Examples: {}, Is Adjective: {}",
                    response.getExplanation().substring(0, Math.min(20, response.getExplanation().length())) + "...",
                    response.getPronunciation(),
                    response.getExamples().substring(0, Math.min(20, response.getExamples().length())) + "...",
                    response.isAdjective());
        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            response.setExplanation("Error parsing explanation: " + e.getMessage());
            response.setExamples("Error parsing examples.");
            response.setPronunciation("Error parsing pronunciation.");
            response.setAdjective(false);
        }

        return response;
    }

    private Map<String, String> extractSections(String text) {
        Map<String, String> sections = new HashMap<>();
        String[] sectionHeaders = { "EXPLANATION:", "PRONUNCIATION:", "EXAMPLES:", "IS_ADJECTIVE:" };

        // First, normalize line endings and remove any <think> blocks
        text = text.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Normalize line endings
        text = text.replaceAll("\r\n", "\n");

        // Find the start positions of each section
        Map<String, Integer> sectionPositions = new HashMap<>();
        for (String header : sectionHeaders) {
            int pos = text.indexOf(header);
            if (pos >= 0) {
                sectionPositions.put(header, pos);
                logger.debug("Found section '{}' at position {}", header, pos);
            } else {
                logger.warn("Section '{}' not found in response", header);
            }
        }

        // Sort the sections by their position in the text
        List<String> orderedHeaders = sectionPositions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        logger.debug("Ordered section headers: {}", orderedHeaders);

        // Extract each section's content
        for (int i = 0; i < orderedHeaders.size(); i++) {
            String currentHeader = orderedHeaders.get(i);
            int currentStart = sectionPositions.get(currentHeader) + currentHeader.length();

            // If this is the last section, extract until the end of text
            if (i == orderedHeaders.size() - 1) {
                String content = text.substring(currentStart).trim();
                sections.put(currentHeader.replace(":", ""), content);
                logger.debug("Extracted '{}' section (last): {} characters",
                        currentHeader.replace(":", ""), content.length());
            } else {
                // Otherwise extract until the start of the next section
                String nextHeader = orderedHeaders.get(i + 1);
                int nextStart = sectionPositions.get(nextHeader);
                String content = text.substring(currentStart, nextStart).trim();
                sections.put(currentHeader.replace(":", ""), content);
                logger.debug("Extracted '{}' section: {} characters",
                        currentHeader.replace(":", ""), content.length());
            }
        }

        return sections;
    } // Cache management methods

    /**
     * Clears the translation cache
     */
    public void clearCache() {
        if (!cacheEnabled) {
            logger.info("Cache is disabled, nothing to clear");
            return;
        }
        logger.info("Clearing translation cache. Removed {} entries.", cache.size());
        cache.clear();
    }

    /**
     * Returns the current size of the translation cache
     * 
     * @return The number of entries in the cache
     */
    public int getCacheSize() {
        if (!cacheEnabled) {
            return 0;
        }
        return cache.size();
    }

    /**
     * Checks if a word is in the cache
     * 
     * @param word     The word to check
     * @param language The language of the word
     * @return true if the word is in the cache, false otherwise
     */
    public boolean isInCache(String word, String language) {
        if (!cacheEnabled) {
            return false;
        }
        return cache.containsKey(language + ":" + word);
    }

    /**
     * Checks if the cache is enabled
     * 
     * @return true if the cache is enabled, false otherwise
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
}
