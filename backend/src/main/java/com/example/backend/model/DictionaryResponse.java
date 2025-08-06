package com.example.backend.model;

public class DictionaryResponse {
    private String malayWord;
    private String mandarinWord;
    private String explanation;
    private String examples;
    private String pinyin;
    private boolean adjective;

    public DictionaryResponse() {
    }

    public DictionaryResponse(String malayWord, String mandarinWord, String explanation, String examples,
            String pinyin, boolean adjective) {
        this.malayWord = malayWord;
        this.mandarinWord = mandarinWord;
        this.explanation = explanation;
        this.examples = examples;
        this.pinyin = pinyin;
        this.adjective = adjective;
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

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public boolean isAdjective() {
        return adjective;
    }

    public void setAdjective(boolean adjective) {
        this.adjective = adjective;
    }
}
