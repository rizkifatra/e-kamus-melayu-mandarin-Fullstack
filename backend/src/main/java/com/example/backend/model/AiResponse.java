package com.example.backend.model;

public class AiResponse {
    private String explanation;
    private String examples;
    private String pronunciation;

    public AiResponse() {
    }

    public AiResponse(String explanation, String examples, String pronunciation) {
        this.explanation = explanation;
        this.examples = examples;
        this.pronunciation = pronunciation;
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

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }
}
