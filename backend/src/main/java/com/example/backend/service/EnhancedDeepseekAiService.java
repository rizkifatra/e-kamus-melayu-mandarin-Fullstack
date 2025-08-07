package com.example.backend.service;

import com.example.backend.model.AiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Enhanced DeepSeekAiService that overrides specific word translations
 * for better handling of common Malay words.
 */
@Service
@Profile("dev")
@Primary
public class EnhancedDeepseekAiService extends DeepseekAiService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDeepseekAiService.class);

    public EnhancedDeepseekAiService(WebClient webClient) {
        super(webClient);
        logger.info("EnhancedDeepseekAiService initialized - providing enhanced translations for common Malay words");
    }

    @Override
    public Mono<AiResponse> generateExplanation(String word, String language) {
        String lowercaseWord = word.toLowerCase();

        // Enhanced prompt for specific Malay words
        Mono<AiResponse> response = super.generateExplanation(word, language);

        // Post-process the response based on specific words
        switch (lowercaseWord) {
            case "layu":
                response = response.map(this::enhanceLayu);
                break;
            case "gerun":
                response = response.map(this::enhanceGerun);
                break;
            case "cantik":
                response = response.map(this::enhanceCantik);
                break;
            case "pintar":
                response = response.map(this::enhancePintar);
                break;
            case "cepat":
                response = response.map(this::enhanceCepat);
                break;
            case "lambat":
                response = response.map(this::enhanceLambat);
                break;
            case "tinggi":
                response = response.map(this::enhanceTinggi);
                break;
            case "pendek":
                response = response.map(this::enhancePendek);
                break;
            case "baik":
                response = response.map(this::enhanceBaik);
                break;
            case "marah":
                response = response.map(this::enhanceMarah);
                break;
            case "gembira":
                response = response.map(this::enhanceGembira);
                break;
            case "sedih":
                response = response.map(this::enhanceSedih);
                break;
        }

        return response;
    }

    /**
     * Enhance the response for "layu" (withered/wilted)
     */
    private AiResponse enhanceLayu(AiResponse response) {
        if (response.getExplanation().contains("No explanation available") ||
                response.getPronunciation().contains("No pronunciation available")) {

            logger.info("Enhancing translation for word 'layu'");

            // Provide a better pronunciation
            response.setPronunciation("kū wěi");

            // Only override if missing
            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'枯萎' dalam bahasa Mandarin menggambarkan keadaan tumbuhan yang kehilangan kesegaran dan kecergasan, menjadi kering dan layu. Ia biasanya digunakan untuk menggambarkan bunga, daun, atau sayuran yang mulai kering dan tidak segar lagi.");
            }

            // Only override if missing
            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 花朵因缺水而枯萎了。\n" +
                                "   Bunga itu layu kerana kekurangan air.\n" +
                                "2. 不要让植物在阳光下枯萎。\n" +
                                "   Jangan biarkan tumbuhan layu di bawah cahaya matahari.\n" +
                                "3. 这些蔬菜已经开始枯萎了。\n" +
                                "   Sayur-sayuran ini sudah mula layu.");
            }
        }
        return response;
    }

    /**
     * Enhance the response for "gerun" (afraid/fearful)
     */
    private AiResponse enhanceGerun(AiResponse response) {
        if (response.getExplanation().contains("No explanation available") ||
                response.getPronunciation().contains("No pronunciation available")) {

            logger.info("Enhancing translation for word 'gerun'");

            // Provide a better pronunciation
            response.setPronunciation("hài pà");

            // Only override if missing
            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'害怕' dalam bahasa Mandarin bermaksud perasaan takut atau cemas terhadap sesuatu. Ia adalah satu perasaan ketakutan atau kekhuatiran yang dialami apabila seseorang menghadapi sesuatu yang dianggap sebagai ancaman atau bahaya.");
            }

            // Only override if missing
            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 孩子害怕黑暗。\n" +
                                "   Kanak-kanak gerun akan kegelapan.\n" +
                                "2. 他对高处感到害怕。\n" +
                                "   Dia berasa gerun terhadap tempat tinggi.\n" +
                                "3. 不要害怕尝试新事物。\n" +
                                "   Jangan gerun untuk mencuba perkara baru.");
            }
        }
        return response;
    }

    /**
     * Enhance the response for "cantik" (beautiful)
     */
    private AiResponse enhanceCantik(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'cantik'");

            response.setPronunciation("měi lì");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'美丽' dalam bahasa Mandarin merujuk kepada sesuatu yang indah atau menarik dari segi penampilan. Ia digunakan untuk menggambarkan keindahan fizikal seseorang, pemandangan, atau objek yang menimbulkan rasa kagum dan kegembiraan bila dipandang.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 她是个美丽的女孩。\n" +
                                "   Dia seorang gadis yang cantik.\n" +
                                "2. 这里的风景非常美丽。\n" +
                                "   Pemandangan di sini sangat cantik.\n" +
                                "3. 那朵花开得很美丽。\n" +
                                "   Bunga itu mekar dengan cantiknya.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "pintar" (smart)
     */
    private AiResponse enhancePintar(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'pintar'");

            response.setPronunciation("cōng míng");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'聪明' dalam bahasa Mandarin bermaksud mempunyai kemampuan mental yang baik, cerdas, atau bijak. Ia menggambarkan seseorang yang dapat memahami dan mempelajari sesuatu dengan cepat dan menyelesaikan masalah dengan efektif.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 她是班上最聪明的学生。\n" +
                                "   Dia pelajar paling pintar dalam kelasnya.\n" +
                                "2. 这个孩子非常聪明，学东西很快。\n" +
                                "   Anak ini sangat pintar, dia cepat belajar.\n" +
                                "3. 你必须聪明地解决这个问题。\n" +
                                "   Anda mesti menyelesaikan masalah ini dengan cara yang pintar.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "cepat" (fast)
     */
    private AiResponse enhanceCepat(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'cepat'");

            response.setPronunciation("kuài");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'快' dalam bahasa Mandarin bermaksud bergerak atau berlaku dengan kelajuan yang tinggi, atau dalam masa yang singkat. Ia juga boleh merujuk kepada sesuatu yang cekap atau efisien dalam penggunaan masa.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他跑得很快。\n" +
                                "   Dia berlari dengan cepat.\n" +
                                "2. 请快点，我们要迟到了。\n" +
                                "   Tolong cepat sikit, kita akan terlambat.\n" +
                                "3. 这种方法比较快。\n" +
                                "   Cara ini lebih cepat.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "lambat" (slow)
     */
    private AiResponse enhanceLambat(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'lambat'");

            response.setPronunciation("màn");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'慢' dalam bahasa Mandarin bermaksud bergerak atau berlaku dengan kelajuan yang rendah, atau mengambil masa yang lebih panjang daripada biasa. Ia juga boleh menggambarkan seseorang yang tidak cepat dalam tindakan atau pemikiran.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他走路很慢。\n" +
                                "   Dia berjalan dengan lambat.\n" +
                                "2. 这个电脑运行得很慢。\n" +
                                "   Komputer ini beroperasi dengan lambat.\n" +
                                "3. 请慢慢说，我听不懂。\n" +
                                "   Tolong cakap dengan lebih lambat, saya tidak faham.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "tinggi" (tall/high)
     */
    private AiResponse enhanceTinggi(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'tinggi'");

            response.setPronunciation("gāo");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'高' dalam bahasa Mandarin merujuk kepada sesuatu yang mempunyai jarak yang jauh dari bawah ke atas, atau berada pada kedudukan yang lebih atas berbanding dengan tahap biasa. Ia boleh digunakan untuk menggambarkan ketinggian fizikal, tahap, atau darjah sesuatu.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他个子很高。\n" +
                                "   Dia sangat tinggi.\n" +
                                "2. 这座山非常高。\n" +
                                "   Gunung ini sangat tinggi.\n" +
                                "3. 这个城市的生活成本很高。\n" +
                                "   Kos kehidupan di bandar ini sangat tinggi.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "pendek" (short)
     */
    private AiResponse enhancePendek(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'pendek'");

            response.setPronunciation("ǎi");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'矮' dalam bahasa Mandarin merujuk kepada sesuatu yang mempunyai ketinggian yang rendah atau kurang daripada purata. Ia biasanya digunakan untuk menggambarkan ketinggian fizikal seseorang atau objek.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他比我矮一点。\n" +
                                "   Dia sedikit lebih pendek daripada saya.\n" +
                                "2. 那棵树很矮。\n" +
                                "   Pokok itu sangat pendek.\n" +
                                "3. 矮个子的人也可以打篮球。\n" +
                                "   Orang yang pendek juga boleh bermain bola keranjang.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "baik" (good/kind)
     */
    private AiResponse enhanceBaik(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'baik'");

            response.setPronunciation("hǎo");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'好' dalam bahasa Mandarin bermaksud bagus, memuaskan, atau memiliki kualiti yang tinggi. Ia juga boleh bermakna bersikap baik atau bersopan santun. Dalam konteks manusia, ia boleh merujuk kepada seseorang yang berbudi pekerti tinggi atau mempunyai moral yang baik.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他是个好人。\n" +
                                "   Dia seorang yang baik.\n" +
                                "2. 这个电影很好看。\n" +
                                "   Filem ini sangat baik untuk ditonton.\n" +
                                "3. 祝你有个好心情。\n" +
                                "   Semoga anda mempunyai perasaan yang baik.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "marah" (angry)
     */
    private AiResponse enhanceMarah(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'marah'");

            response.setPronunciation("shēng qì");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'生气' dalam bahasa Mandarin bermaksud perasaan tidak senang atau tersinggung yang kuat, yang biasanya diikuti oleh kemarahan atau ketidakpuasan. Ia menggambarkan emosi di mana seseorang itu rasa tidak puas hati atau kesal terhadap sesuatu.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他对我生气了。\n" +
                                "   Dia marah kepada saya.\n" +
                                "2. 别生气，这不是你的错。\n" +
                                "   Jangan marah, ini bukan salah kamu.\n" +
                                "3. 她很容易生气。\n" +
                                "   Dia mudah marah.");
            }

            response.setAdjective(false);
        }
        return response;
    }

    /**
     * Enhance the response for "gembira" (happy)
     */
    private AiResponse enhanceGembira(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'gembira'");

            response.setPronunciation("kuài lè");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'快乐' dalam bahasa Mandarin merujuk kepada perasaan kegembiraan, kesenangan, atau kepuasan. Ia menggambarkan emosi positif yang dirasai apabila seseorang itu berpuas hati atau gembira dengan keadaan semasa.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 他总是很快乐。\n" +
                                "   Dia sentiasa sangat gembira.\n" +
                                "2. 祝你生日快乐。\n" +
                                "   Selamat hari jadi, semoga gembira.\n" +
                                "3. 我们快乐地度过了假期。\n" +
                                "   Kami telah menghabiskan cuti dengan gembira.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Enhance the response for "sedih" (sad)
     */
    private AiResponse enhanceSedih(AiResponse response) {
        // Only enhance if there are issues with the response
        if (isResponseIncomplete(response)) {
            logger.info("Enhancing translation for word 'sedih'");

            response.setPronunciation("bēi shāng");

            if (response.getExplanation().contains("No explanation available")) {
                response.setExplanation(
                        "'悲伤' dalam bahasa Mandarin bermaksud perasaan sedih, dukacita, atau kesedihan. Ia menggambarkan emosi negatif yang dirasai apabila seseorang mengalami kehilangan, kekecewaan, atau situasi yang menyedihkan.");
            }

            if (response.getExamples().contains("No examples available")) {
                response.setExamples(
                        "1. 听到这个消息，他感到非常悲伤。\n" +
                                "   Setelah mendengar berita itu, dia berasa sangat sedih.\n" +
                                "2. 电影的结局很悲伤。\n" +
                                "   Pengakhiran filem itu sangat sedih.\n" +
                                "3. 她的眼睛里充满了悲伤。\n" +
                                "   Matanya dipenuhi dengan kesedihan.");
            }

            response.setAdjective(true);
        }
        return response;
    }

    /**
     * Helper method to check if a response needs enhancement
     */
    private boolean isResponseIncomplete(AiResponse response) {
        return response.getExplanation().contains("No explanation available") ||
                response.getPronunciation().contains("No pronunciation available") ||
                response.getExamples().contains("No examples available");
    }
}
