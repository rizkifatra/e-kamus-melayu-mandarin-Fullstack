package com.example.backend.controller;

import com.example.backend.service.DeepseekAiService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*") // For development; restrict in production
public class CacheController {
    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);
    private final DeepseekAiService deepseekAiService;

    public CacheController(DeepseekAiService deepseekAiService) {
        this.deepseekAiService = deepseekAiService;
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", deepseekAiService.getCacheSize());
        stats.put("enabled", deepseekAiService.isCacheEnabled());
        stats.put("timestamp", System.currentTimeMillis());
        logger.debug("Cache stats requested: {}", stats);
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear the translation cache
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        int sizeBefore = deepseekAiService.getCacheSize();
        deepseekAiService.clearCache();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("clearedEntries", sizeBefore);
        response.put("timestamp", System.currentTimeMillis());

        logger.info("Cache cleared, removed {} entries", sizeBefore);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a specific word is in the cache
     */
    @GetMapping("/contains")
    public ResponseEntity<Map<String, Object>> checkCache(
            @RequestParam String word,
            @RequestParam(defaultValue = "Mandarin") String language) {

        boolean inCache = deepseekAiService.isInCache(word, language);

        Map<String, Object> response = new HashMap<>();
        response.put("word", word);
        response.put("language", language);
        response.put("inCache", inCache);

        logger.debug("Cache check for '{}' in {}: {}", word, language, inCache);
        return ResponseEntity.ok(response);
    }
}
