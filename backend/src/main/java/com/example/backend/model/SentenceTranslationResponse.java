package com.example.backend.model;

public class SentenceTranslationResponse {
    private String originalSentence;
    private String translatedSentence;
    private String sourceLanguage;
    private String targetLanguage;
    private boolean success;

    public SentenceTranslationResponse() {
    }

    public SentenceTranslationResponse(String originalSentence, String translatedSentence,
            String sourceLanguage, String targetLanguage, boolean success) {
        this.originalSentence = originalSentence;
        this.translatedSentence = translatedSentence;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.success = success;
    }

    public String getOriginalSentence() {
        return originalSentence;
    }

    public void setOriginalSentence(String originalSentence) {
        this.originalSentence = originalSentence;
    }

    public String getTranslatedSentence() {
        return translatedSentence;
    }

    public void setTranslatedSentence(String translatedSentence) {
        this.translatedSentence = translatedSentence;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
