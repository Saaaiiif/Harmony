package services.ForumServices;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service de détection de gros mots
 * ✅ Double protection : liste locale FR + API purgomalum.com (gratuite)
 */
public class BadWordsService {

    private static final String API_URL = "https://www.purgomalum.com/service/containsprofanity?text=";

    // ── Liste locale française ────────────────────────────────────────────────
    private static final List<String> BAD_WORDS_FR = Arrays.asList(
            "merde", "putain", "connard", "connasse", "salope", "enculé",
            "enculer", "fdp", "fils de pute", "nique", "niquer", "ta gueule",
            "va te faire", "batard", "bâtard", "cul", "bite", "couille",
            "chier", "chierrer", "bordel", "pute", "con", "conne", "idiot",
            "crétin", "abruti", "imbécile", "débile", "mongol", "attardé",
            "raciste", "nazi", "Hitler", "nigger", "negro"
    );

    /**
     * Vérifie si le texte contient des gros mots.
     * D'abord vérifie localement (instantané), puis via API si propre.
     *
     * @return résultat avec détails
     */
    public DetectionResult checkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new DetectionResult(false, null, text);
        }

        // ── 1. Vérification locale (instantanée) ─────────────────────────────
        String lowerText = text.toLowerCase();
        for (String word : BAD_WORDS_FR) {
            if (lowerText.contains(word.toLowerCase())) {
                return new DetectionResult(true, word, censorText(text, word));
            }
        }

        // ── 2. Vérification via API (anglais + international) ─────────────────
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + encoded))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            boolean hasProfanity = "true".equalsIgnoreCase(response.body().trim());
            if (hasProfanity) {
                return new DetectionResult(true, "mot inapproprié détecté", censorText(text));
            }

        } catch (Exception e) {
            // API indisponible — on continue avec le résultat local seulement
            System.out.println("⚠️ BadWords API indisponible, vérification locale seulement");
        }

        return new DetectionResult(false, null, text);
    }

    /**
     * Censure un mot spécifique dans le texte (remplace par ***)
     */
    private String censorText(String text, String word) {
        String stars = "*".repeat(word.length());
        return text.replaceAll("(?i)" + Pattern.quote(word), stars);
    }

    /**
     * Censure générique (quand le mot exact n'est pas connu)
     */
    private String censorText(String text) {
        return text.replaceAll("\\b\\w{4,}\\b", "****");
    }

    /**
     * Version rapide : retourne juste true/false sans API (pour validation temps réel)
     */
    public boolean containsBadWordsLocal(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase();
        return BAD_WORDS_FR.stream().anyMatch(w -> lower.contains(w.toLowerCase()));
    }

    // ── Résultat de détection ─────────────────────────────────────────────────
    public static class DetectionResult {
        private final boolean hasBadWords;
        private final String detectedWord;
        private final String censoredText;

        public DetectionResult(boolean hasBadWords, String detectedWord, String censoredText) {
            this.hasBadWords = hasBadWords;
            this.detectedWord = detectedWord;
            this.censoredText = censoredText;
        }

        public boolean hasBadWords() { return hasBadWords; }
        public String getDetectedWord() { return detectedWord; }
        public String getCensoredText() { return censoredText; }
    }
}
