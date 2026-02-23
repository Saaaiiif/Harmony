package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GroqAIService {
// url api grok
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // api key
    private static final String API_KEY = "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";
    // ai model
    private static final String MODEL = "llama-3.3-70b-versatile";

    public String generateMeditationFromTheme(String theme) throws Exception {
        String systemPrompt = "Tu es un assistant spécialisé dans la création de sessions de méditation pour une application universitaire de bien-être étudiant appelée Harmonie. "
                + "L'application aide les étudiants à gérer leur stress et leur bien-être mental. "
                + "Quand on te donne un thème de méditation, tu dois générer les informations suivantes en format EXACT (chaque ligne commence par le préfixe indiqué, sans markdown, sans guillemets) :\n"
                + "AUTEUR: (un nom d'auteur/expert en méditation connu et réaliste)\n"
                + "DUREE: (un nombre entier en minutes, entre 5 et 60, adapté au thème)\n"
                + "AUDIO: (une URL YouTube réaliste vers une vidéo de méditation guidée en rapport avec le thème, format https://www.youtube.com/watch?v=...)\n"
                + "CONSEIL1: (un conseil pratique de méditation lié au thème, minimum 10 caractères)\n"
                + "CONSEIL2: (un deuxième conseil pratique, minimum 10 caractères)\n"
                + "CONSEIL3: (un troisième conseil pratique, minimum 10 caractères)\n"
                + "Réponds UNIQUEMENT avec ces 6 lignes, rien d'autre. Pas de texte avant ni après.";

        String userMessage = "Génère une session de méditation sur le thème: " + theme;
        return callGroqAPI(systemPrompt, userMessage);
    }

    public String generateStudentRapport(String studentName, String journalSummary) throws Exception {
        String systemPrompt = "Tu es un psychologue scolaire bienveillant qui analyse le bien-être émotionnel des étudiants pour l'application Harmonie. "
                + "Tu reçois un résumé anonymisé des entrées du journal d'humeur d'un étudiant (dates, scores d'humeur de 1 à 5, et humeurs). "
                + "Tu ne dois JAMAIS citer le contenu personnel du journal - c'est privé. "
                + "Tu dois fournir un rapport professionnel et bienveillant qui inclut :\n"
                + "1. Une évaluation générale du bien-être émotionnel de l'étudiant\n"
                + "2. Les tendances observées (amélioration, détérioration, stabilité)\n"
                + "3. Des recommandations pour l'administration/encadrants\n"
                + "4. Des suggestions d'actions concrètes\n"
                + "Le rapport doit être en français, professionnel mais empathique. "
                + "Ne mentionne JAMAIS ce que l'étudiant a écrit dans son journal. "
                + "Base-toi uniquement sur les scores et les humeurs pour ton analyse. "
                + "Formate le rapport de manière claire avec des sections.";

        String userMessage = "Génère un rapport de bien-être pour l'étudiant " + studentName + ".\n"
                + "Voici le résumé de ses entrées de journal :\n" + journalSummary;
        return callGroqAPI(systemPrompt, userMessage);
    }

    private String callGroqAPI(String systemPrompt, String userMessage) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        String jsonBody = "{"
                + "\"model\": \"" + MODEL + "\","
                + "\"messages\": ["
                + "{\"role\": \"system\", \"content\": " + escapeJson(systemPrompt) + "},"
                + "{\"role\": \"user\", \"content\": " + escapeJson(userMessage) + "}"
                + "],"
                + "\"temperature\": 0.7,"
                + "\"max_tokens\": 1024"
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            reader.close();
            throw new Exception("Erreur API Groq (code " + responseCode + "): " + errorResponse);
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return extractContent(response.toString());
    }

    private String extractContent(String jsonResponse) {
        String marker = "\"content\":";
        int idx = jsonResponse.lastIndexOf(marker);
        if (idx == -1) {
            return jsonResponse;
        }

        idx += marker.length();
        while (idx < jsonResponse.length() && (jsonResponse.charAt(idx) == ' ' || jsonResponse.charAt(idx) == '\t')) {
            idx++;
        }

        if (idx < jsonResponse.length() && jsonResponse.charAt(idx) == '"') {
            idx++;
            StringBuilder content = new StringBuilder();
            while (idx < jsonResponse.length()) {
                char c = jsonResponse.charAt(idx);
                if (c == '\\' && idx + 1 < jsonResponse.length()) {
                    char next = jsonResponse.charAt(idx + 1);
                    if (next == '"') {
                        content.append('"');
                        idx += 2;
                    } else if (next == 'n') {
                        content.append('\n');
                        idx += 2;
                    } else if (next == 'r') {
                        content.append('\r');
                        idx += 2;
                    } else if (next == 't') {
                        content.append('\t');
                        idx += 2;
                    } else if (next == '\\') {
                        content.append('\\');
                        idx += 2;
                    } else {
                        content.append(c);
                        idx++;
                    }
                } else if (c == '"') {
                    break;
                } else {
                    content.append(c);
                    idx++;
                }
            }
            return content.toString();
        }

        return jsonResponse.substring(idx);
    }

    private String escapeJson(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
