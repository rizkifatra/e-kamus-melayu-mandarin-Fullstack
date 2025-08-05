package com.example.backend.model;

public class SentenceTranslationRequest {
    private String sentence;
    private String sourceLanguage;
    private String targetLanguage;

    public SentenceTranslationRequest() {
    }

    public SentenceTranslationRequest(String sentence, String sourceLanguage, String targetLanguage) {
        this.sentence = sentence;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
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
}
