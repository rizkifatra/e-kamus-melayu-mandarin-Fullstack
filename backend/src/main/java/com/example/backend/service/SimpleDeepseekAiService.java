package com.example.backend.service;

import com.example.backend.model.AiResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * SimpleDeepseekAiService is a simplified implementation that doesn't require
 * an external AI service.
 * This service provides predefined responses for common words and is used
 * in production environments where AI APIs might not be available or reliable.
 */
@Service
@Profile("prod")
@Primary
public class SimpleDeepseekAiService extends DeepseekAiService {

    private final Map<String, AiResponse> preloadedResponses = new HashMap<>();

    public SimpleDeepseekAiService(WebClient webClient) {
        super(webClient);
        System.out.println("SimpleDeepseekAiService initialized - using predefined responses");
        initializePreloadedResponses();
    }

    @Override
    public Mono<AiResponse> generateExplanation(String word, String language) {
        System.out.println("SimpleDeepseekAiService: Generating explanation for: " + word);

        // Check if we have a preloaded response for this word
        if (preloadedResponses.containsKey(word)) {
            System.out.println("Using preloaded response for: " + word);
            return Mono.just(preloadedResponses.get(word));
        }

        // If no preloaded response, create a generic response
        System.out.println("No preloaded response for: " + word + ", creating generic response");
        AiResponse response = new AiResponse();
        response.setExplanation("Ini adalah terjemahan generik untuk perkataan '" + word
                + "'. Untuk mendapatkan penjelasan terperinci, gunakan profil 'dev'.");
        response.setExamples(
                "1. 这是一个例子。\n   Ini adalah contoh.\n2. 请使用这个词。\n   Sila gunakan perkataan ini.\n3. 我们学习中文。\n   Kami belajar bahasa Mandarin.");
        response.setPronunciation(getPinyinFallback(word));
        response.setAdjective(false);

        return Mono.just(response);
    }

    private String getPinyinFallback(String word) {
        // This is a simplified version that doesn't try to guess the pronunciation
        return "Pinyin tidak tersedia untuk '" + word + "'";
    }

    private void initializePreloadedResponses() {
        // Common Mandarin words with preloaded responses
        addPreloadedWord("你好",
                "Salam atau ucapan sapaan yang biasa digunakan untuk menyapa seseorang. Ia bermakna 'Hello' atau 'Hi' dalam Bahasa Inggeris.",
                "nǐ hǎo",
                "1. 你好，我的名字是李明。\n   Hello, nama saya adalah Li Ming.\n2. 早上好，你好吗？\n   Selamat pagi, apa khabar?\n3. 请你好好学习。\n   Sila belajar dengan baik.",
                false);

        addPreloadedWord("谢谢",
                "Ungkapan terima kasih dalam Bahasa Mandarin. Digunakan untuk menunjukkan penghargaan atau rasa terima kasih kepada seseorang.",
                "xiè xiè",
                "1. 谢谢你的帮助。\n   Terima kasih atas bantuan anda.\n2. 非常谢谢你！\n   Terima kasih banyak!\n3. 我要谢谢我的父母。\n   Saya ingin berterima kasih kepada ibu bapa saya.",
                false);

        addPreloadedWord("朋友",
                "Perkataan untuk 'kawan' atau 'sahabat'. Merujuk kepada seseorang yang mempunyai hubungan rapat dan baik dengan anda.",
                "péng yǒu",
                "1. 他是我的好朋友。\n   Dia adalah kawan baik saya.\n2. 我们是朋友。\n   Kami adalah kawan.\n3. 朋友们都来参加派对。\n   Semua kawan datang ke majlis.",
                false);

        addPreloadedWord("学习",
                "Bermaksud 'belajar' atau 'mengkaji'. Merujuk kepada proses mendapatkan pengetahuan atau kemahiran baru.",
                "xué xí",
                "1. 我在学习中文。\n   Saya sedang belajar Bahasa Mandarin.\n2. 学习需要时间。\n   Pembelajaran memerlukan masa.\n3. 他喜欢学习新技能。\n   Dia suka belajar kemahiran baru.",
                false);

        addPreloadedWord("工作", "Bermaksud 'kerja' atau 'pekerjaan'. Boleh digunakan sebagai kata nama atau kata kerja.",
                "gōng zuò",
                "1. 我的工作很有趣。\n   Kerja saya sangat menarik.\n2. 他在银行工作。\n   Dia bekerja di bank.\n3. 这份工作需要经验。\n   Pekerjaan ini memerlukan pengalaman.",
                false);

        addPreloadedWord("美丽",
                "Bermaksud 'cantik' atau 'indah'. Digunakan untuk menerangkan sesuatu yang mempunyai penampilan yang menyenangkan.",
                "měi lì",
                "1. 这是一个美丽的花园。\n   Ini adalah taman yang cantik.\n2. 她有一双美丽的眼睛。\n   Dia mempunyai sepasang mata yang cantik.\n3. 中国有很多美丽的风景。\n   China mempunyai banyak pemandangan yang indah.",
                true);

        System.out.println("Preloaded " + preloadedResponses.size() + " word responses");
    }

    private void addPreloadedWord(String word, String explanation, String pronunciation, String examples,
            boolean isAdjective) {
        AiResponse response = new AiResponse();
        response.setExplanation(explanation);
        response.setPronunciation(pronunciation);
        response.setExamples(examples);
        response.setAdjective(isAdjective);

        preloadedResponses.put(word, response);
    }
}
