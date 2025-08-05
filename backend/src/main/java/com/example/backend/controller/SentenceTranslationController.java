package com.example.backend.controller;

import com.example.backend.model.SentenceTranslationRequest;
import com.example.backend.model.SentenceTranslationResponse;
import com.example.backend.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SentenceTranslationController {

    private final TranslationService translationService;

    public SentenceTranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/translate-sentence")
    public Mono<ResponseEntity<SentenceTranslationResponse>> translateSentence(
            @RequestBody SentenceTranslationRequest request) {
        if (request.getSentence() == null || request.getSentence().trim().isEmpty()) {
            SentenceTranslationResponse errorResponse = new SentenceTranslationResponse();
            errorResponse.setOriginalSentence("");
            errorResponse.setTranslatedSentence("Error: No sentence provided");
            errorResponse.setSourceLanguage(request.getSourceLanguage());
            errorResponse.setTargetLanguage(request.getTargetLanguage());
            errorResponse.setSuccess(false);
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        // Default to Malay -> Chinese if not specified
        String sourceLanguage = request.getSourceLanguage() != null ? request.getSourceLanguage() : "ms";
        String targetLanguage = request.getTargetLanguage() != null ? request.getTargetLanguage() : "zh";

        return translationService.translateText(request.getSentence(), sourceLanguage, targetLanguage)
                .map(translatedText -> {
                    SentenceTranslationResponse response = new SentenceTranslationResponse();
                    response.setOriginalSentence(request.getSentence());
                    response.setTranslatedSentence(translatedText);
                    response.setSourceLanguage(sourceLanguage);
                    response.setTargetLanguage(targetLanguage);
                    response.setSuccess(true);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    SentenceTranslationResponse errorResponse = new SentenceTranslationResponse();
                    errorResponse.setOriginalSentence(request.getSentence());
                    errorResponse.setTranslatedSentence("Translation error: " + e.getMessage());
                    errorResponse.setSourceLanguage(sourceLanguage);
                    errorResponse.setTargetLanguage(targetLanguage);
                    errorResponse.setSuccess(false);
                    return Mono.just(ResponseEntity.ok(errorResponse));
                });
    }

    @GetMapping("/translate-sentence")
    public Mono<ResponseEntity<SentenceTranslationResponse>> translateSentenceGet(
            @RequestParam String sentence,
            @RequestParam(required = false, defaultValue = "ms") String from,
            @RequestParam(required = false, defaultValue = "zh") String to) {

        SentenceTranslationRequest request = new SentenceTranslationRequest();
        request.setSentence(sentence);
        request.setSourceLanguage(from);
        request.setTargetLanguage(to);

        return translateSentence(request);
    }
}
