package com.example.backend.model;

public class DictionaryResponse {
    private String malayWord;
    private String mandarinWord;
    private String explanation;
    private String examples;
    private String pronunciationGuide;

    public DictionaryResponse() {
    }

    public DictionaryResponse(String malayWord, String mandarinWord, String explanation, String examples,
            String pronunciationGuide) {
        this.malayWord = malayWord;
        this.mandarinWord = mandarinWord;
        this.explanation = explanation;
        this.examples = examples;
        this.pronunciationGuide = pronunciationGuide;
    }

    public String getMalayWord() {
        return malayWord;
    }

    public void setMalayWord(String malayWord) {
        this.malayWord = malayWord;
    }

    public String getMandarinWord() {
        return mandarinWord;
    }

    public void setMandarinWord(String mandarinWord) {
        this.mandarinWord = mandarinWord;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getExamples() {
        return examples;
    }

    public void setExamples(String examples) {
        this.examples = examples;
    }

    public String getPronunciationGuide() {
        return pronunciationGuide;
    }

    public void setPronunciationGuide(String pronunciationGuide) {
        this.pronunciationGuide = pronunciationGuide;
    }
}
