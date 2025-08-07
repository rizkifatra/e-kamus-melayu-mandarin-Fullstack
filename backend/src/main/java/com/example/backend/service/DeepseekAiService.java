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
                "You are a language expert with superior fluency in both Malay (Bahasa Malaysia/Melayu) and Mandarin Chinese. Always respond directly with the final answer in the exact format requested. The explanations must always be written in proper Malay language. IMPORTANT: DO NOT USE ANY MARKDOWN FORMATTING IN YOUR RESPONSE. Avoid using any asterisks (*), underscores (_), backticks (`), tildes (~), or any other formatting characters in your text. Provide plain, unformatted text only. Never use <think> tags or show your internal reasoning process. You have extensive knowledge of everyday Malay words like 'makan', 'tidur', 'cantik', 'muram', 'layu', and 'gerun' and their Mandarin Chinese equivalents.");

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
        // For common Malaysian words, we can provide additional context to help the
        // model
        String additionalContext = "";
        String lowercaseWord = word.toLowerCase();

        // Special handling for common Malay words that might need better translation
        if (lowercaseWord.equals("makan")) {
            additionalContext = "Note that 'makan' is a very common Malay verb meaning 'to eat' (吃 in Chinese, pronounced 'chī').";
        } else if (lowercaseWord.equals("tidur")) {
            additionalContext = "Note that 'tidur' is a common Malay verb meaning 'to sleep' (睡觉 in Chinese, pronounced 'shuì jiào').";
        } else if (lowercaseWord.equals("cantik")) {
            additionalContext = "Note that 'cantik' is a common Malay adjective meaning 'beautiful/pretty' (美丽 in Chinese, pronounced 'měi lì').";
        } else if (lowercaseWord.equals("muram")) {
            additionalContext = "Note that 'muram' is a Malay adjective meaning 'gloomy/depressed' (忧郁 in Chinese, pronounced 'yōu yù').";
        } else if (lowercaseWord.equals("layu")) {
            additionalContext = "Note that 'layu' is a Malay word meaning 'withered/wilted' (枯萎 in Chinese, pronounced 'kū wěi'). It specifically describes plants that have lost freshness and are drooping.";
        } else if (lowercaseWord.equals("gerun")) {
            additionalContext = "Note that 'gerun' is a Malay word meaning 'afraid/fearful' (害怕 in Chinese, pronounced 'hài pà').";
        }

        return String.format(
                "You are a language expert teaching Simplified Mandarin Chinese who teaches Chinese and needs accurate linguistic details for practical use.\n\n"
                        +
                        "DO NOT USE <think> TAGS OR INTERNAL DELIBERATION. RESPOND IMMEDIATELY WITH THE FINAL ANSWER.\n\n"
                        +
                        "IMPORTANT: DO NOT USE ANY FORMATTING such as **, _, ~~, or any other markdown. Plain text only with no asterisks or formatting symbols.\n\n"
                        +
                        "Please provide a comprehensive explanation of the %s word '%s'. %s\n" +
                        "The response must be clear, structured, and follow the exact format below:\n\n" +
                        "1. A simple explanation of the word's meaning written ONLY in Malay language (Bahasa Malaysia/Melayu). THE EXPLANATION MUST BE IN MALAY LANGUAGE, NOT IN CHINESE.\n"
                        +
                        "2. The accurate pinyin pronunciation for the Chinese equivalent with tone marks (e.g., 'hǎo', not 'hao3').\n"
                        +
                        "3. Three example sentences using this word in real context. Each should include:\n" +
                        "   - The original sentence in Chinese\n" +
                        "   - Its translation in Malay\n" +
                        "4. State whether this word is an adjective in Chinese grammar (answer with YES or NO).\n\n" +
                        "Use the following EXACT section headers in your response:\n\n" +
                        "EXPLANATION:\n[your simple explanation written ONLY in Bahasa Malaysia/Melayu, not in Chinese or any other language]\n\n"
                        +
                        "PRONUNCIATION:\n[pinyin with tone marks only]\n\n" +
                        "EXAMPLES:\n" +
                        "1. [Chinese sentence]\n   [Malay translation]\n" +
                        "2. [Chinese sentence]\n   [Malay translation]\n" +
                        "3. [Chinese sentence]\n   [Malay translation]\n\n" +
                        "IS_ADJECTIVE:\n[YES or NO]",
                language, word, additionalContext);
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

            // Log the raw text for debugging
            logger.debug("Raw text after removing <think> tags (first 100 chars): {}",
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);

            // First try using our section extractor
            Map<String, String> sections = extractSections(text);

            // If any critical sections are missing, try direct regex extraction as a
            // fallback
            if (!sections.containsKey("EXPLANATION") || !sections.containsKey("PRONUNCIATION")
                    || !sections.containsKey("EXAMPLES")) {
                logger.info("Some sections missing from primary extraction method, trying direct regex extraction...");

                // Try direct extraction with a new approach
                if (!sections.containsKey("EXPLANATION")) {
                    String explanation = extractWithRegex(text, "EXPLANATION:", "PRONUNCIATION:");
                    if (explanation != null) {
                        sections.put("EXPLANATION", explanation);
                        logger.debug("Extracted EXPLANATION with regex: {} chars", explanation.length());
                    }
                }

                if (!sections.containsKey("PRONUNCIATION")) {
                    String pronunciation = extractWithRegex(text, "PRONUNCIATION:", "EXAMPLES:");
                    if (pronunciation != null) {
                        sections.put("PRONUNCIATION", pronunciation);
                        logger.debug("Extracted PRONUNCIATION with regex: {} chars", pronunciation.length());
                    }
                }

                if (!sections.containsKey("EXAMPLES")) {
                    String examples = extractWithRegex(text, "EXAMPLES:", "IS_ADJECTIVE:");
                    if (examples != null) {
                        sections.put("EXAMPLES", examples);
                        logger.debug("Extracted EXAMPLES with regex: {} chars", examples.length());
                    }
                }

                if (!sections.containsKey("IS_ADJECTIVE")) {
                    String isAdj = extractWithRegex(text, "IS_ADJECTIVE:", null);
                    if (isAdj != null) {
                        sections.put("IS_ADJECTIVE", isAdj);
                        logger.debug("Extracted IS_ADJECTIVE with regex: {}", isAdj);
                    }
                }
            }

            // Clean up and set the extracted values in the response object
            response.setExplanation(
                    cleanUpFormatting(sections.getOrDefault("EXPLANATION", "No explanation available.")));
            response.setPronunciation(
                    cleanUpFormatting(sections.getOrDefault("PRONUNCIATION", "No pronunciation available.")));
            response.setExamples(cleanUpFormatting(sections.getOrDefault("EXAMPLES", "No examples available.")));

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

    /**
     * Extract text between two section headers using regex
     * 
     * @param text        Full text
     * @param startHeader Starting header
     * @param endHeader   Ending header or null if extracting to the end
     * @return Extracted text or null if not found
     */
    private String extractWithRegex(String text, String startHeader, String endHeader) {
        try {
            int startIdx = text.indexOf(startHeader);
            if (startIdx == -1) {
                logger.warn("Could not find start header: {}", startHeader);
                return null;
            }

            startIdx += startHeader.length();
            int endIdx;

            if (endHeader != null) {
                endIdx = text.indexOf(endHeader, startIdx);
                if (endIdx == -1) {
                    // If the end header is not found, extract until the end
                    endIdx = text.length();
                }
            } else {
                endIdx = text.length();
            }

            String extracted = text.substring(startIdx, endIdx).trim();
            logger.debug("Extracted {} to {} ({}): {}",
                    startHeader,
                    endHeader != null ? endHeader : "END",
                    extracted.length(),
                    extracted.length() > 30 ? extracted.substring(0, 30) + "..." : extracted);

            return extracted;
        } catch (Exception e) {
            logger.error("Error extracting section between {} and {}: {}",
                    startHeader, endHeader, e.getMessage());
            return null;
        }
    }

    /**
     * Cleans up formatting artifacts from the text
     * Removes asterisks, extra whitespace, and other unwanted formatting
     * 
     * @param text The text to clean
     * @return Cleaned text
     */
    private String cleanUpFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // More aggressive cleaning of formatting

        // Remove single and double asterisks (bold and italic in markdown)
        text = text.replaceAll("\\*\\*", "");
        text = text.replaceAll("\\*", "");

        // Remove underscores (italic and bold in markdown)
        text = text.replaceAll("__", "");
        text = text.replaceAll("_", "");

        // Remove tildes (strikethrough in markdown)
        text = text.replaceAll("~~", "");

        // Remove backticks (code in markdown)
        text = text.replaceAll("`", "");

        // Remove angle brackets that might be used for HTML-like formatting
        text = text.replaceAll("<[^>]*>", "");

        // Normalize multiple newlines to a single newline
        text = text.replaceAll("\n{3,}", "\n\n");

        // Remove extra whitespace at the start and end
        text = text.trim();

        // Log the cleaned text for debugging
        logger.debug("Cleaned text from {} chars to {} chars", text.length() + 1, text.length());

        return text;
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
            String content = "";

            // If this is the last section, extract until the end of text
            if (i == orderedHeaders.size() - 1) {
                content = text.substring(currentStart).trim();
                logger.debug("Extracted '{}' section (last): {} characters",
                        currentHeader.replace(":", ""), content.length());
            } else {
                // Otherwise extract until the start of the next section
                String nextHeader = orderedHeaders.get(i + 1);
                int nextStart = sectionPositions.get(nextHeader);
                content = text.substring(currentStart, nextStart).trim();
                logger.debug("Extracted '{}' section: {} characters",
                        currentHeader.replace(":", ""), content.length());
            }

            // Pre-clean the content before storing it
            content = cleanUpFormatting(content);
            sections.put(currentHeader.replace(":", ""), content);

            // Log the cleaned content
            logger.debug("Cleaned '{}' section content: {}",
                    currentHeader.replace(":", ""),
                    content.substring(0, Math.min(30, content.length())) + (content.length() > 30 ? "..." : ""));
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
