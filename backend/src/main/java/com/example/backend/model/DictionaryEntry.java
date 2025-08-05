package com.example.backend.model;

public class DictionaryEntry {
    private String malayWord;
    private String mandarinWord;
    private String pronunciationGuide;
    private String explanation;
    private String[] examples;

    public DictionaryEntry() {
    }

    public DictionaryEntry(String malayWord, String mandarinWord, String pronunciationGuide, String explanation,
            String[] examples) {
        this.malayWord = malayWord;
        this.mandarinWord = mandarinWord;
        this.pronunciationGuide = pronunciationGuide;
        this.explanation = explanation;
        this.examples = examples;
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

    public String getPronunciationGuide() {
        return pronunciationGuide;
    }

    public void setPronunciationGuide(String pronunciationGuide) {
        this.pronunciationGuide = pronunciationGuide;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String[] getExamples() {
        return examples;
    }

    public void setExamples(String[] examples) {
        this.examples = examples;
    }
}
