package services.MeditationServices;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class GroqAIService {

    private static final String API_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String WHISPER_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String API_KEY     = "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";
    private static final String MODEL       = "llama-3.3-70b-versatile";
    private static final String WHISPER_MODEL = "whisper-large-v3-turbo";


    public String generateMeditationFromTheme(String theme) throws Exception {
        String systemPrompt = "Tu es un assistant spécialisé dans la création de sessions de méditation pour une application universitaire de bien-être étudiant appelée Harmonie. "
                + "L'application aide les étudiants à gérer leur stress et leur bien-être mental. "
                + "Quand on te donne un thème de méditation, tu dois générer les informations suivantes en format EXACT (sans markdown, sans guillemets) :\n"
                + "AUTEUR: (invente un nom fictif original)\n"
                + "DUREE: (nombre entier en minutes, entre 5 et 60)\n"
                + "AUDIO: (URL YouTube réaliste vers une vidéo de méditation guidée, format https://www.youtube.com/watch?v=...)\n"
                + "CONSEIL1: (conseil pratique lié au thème, minimum 10 caractères)\n"
                + "CONSEIL2: (deuxième conseil pratique)\n"
                + "CONSEIL3: (troisième conseil pratique)\n"
                + "Réponds UNIQUEMENT avec ces 6 lignes, rien d'autre.";
        return callGroqAPI(systemPrompt, "Génère une session de méditation sur le thème: " + theme);
    }


    public String generateStudentRapport(String studentName, String journalSummary) throws Exception {
        String systemPrompt = "Tu es un psychologue scolaire bienveillant qui analyse le bien-être émotionnel des étudiants pour l'application Harmonie. "
                + "Tu reçois un résumé anonymisé des entrées du journal d'humeur d'un étudiant (dates, scores 1-5, humeurs). "
                + "Tu ne dois JAMAIS citer le contenu personnel du journal. "
                + "Fournis un rapport professionnel incluant :\n"
                + "1. Une évaluation générale du bien-être\n"
                + "2. Les tendances observées\n"
                + "3. Des recommandations pour l'administration\n"
                + "4. Des actions concrètes\n"
                + "Le rapport doit être en français, professionnel et empathique.";
        return callGroqAPI(systemPrompt,
                "Génère un rapport de bien-être pour l'étudiant " + studentName + ".\nVoici le résumé:\n" + journalSummary);
    }

    public String parseJournalFromSpeech(String transcription, String currentDate) throws Exception {
        String systemPrompt = "Tu es un assistant qui analyse la parole d'un étudiant pour remplir automatiquement son journal d'humeur. "
                + "La date d'aujourd'hui est: " + currentDate + ". "
                + "Extrais les informations suivantes en format EXACT :\n"
                + "DATE: (format YYYY-MM-DD)\n"
                + "HUMEUR: (TRES_BIEN, BIEN, NEUTRE, MAL, ou TRES_MAL)\n"
                + "CONTENU: (résumé clair de ce que l'étudiant a exprimé)\n"
                + "Réponds UNIQUEMENT avec ces 3 lignes.";
        return callGroqAPI(systemPrompt, "Transcription: \"" + transcription + "\"");
    }

 
    private Process currentSpeechProcess = null;

    public boolean isSpeaking() {
        return currentSpeechProcess != null && currentSpeechProcess.isAlive();
    }

    public void stopSpeaking() {
        if (currentSpeechProcess != null && currentSpeechProcess.isAlive()) {
            currentSpeechProcess.destroyForcibly();
            currentSpeechProcess = null;
        }
    }

    public void speakText(String text, Runnable onDone) {
        if (isSpeaking()) {
            stopSpeaking();
            if (onDone != null) onDone.run();
            return;
        }
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", "-v", "Thomas", text);
                } else if (os.contains("win")) {
                    String psCommand = "Add-Type -AssemblyName System.Speech; "
                            + "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                            + "$synth.Speak('" + text.replace("'", "''") + "');";
                    pb = new ProcessBuilder("powershell", "-Command", psCommand);
                } else {
                    pb = new ProcessBuilder("espeak", "-v", "fr", text);
                }
                pb.redirectErrorStream(true);
                currentSpeechProcess = pb.start();
                currentSpeechProcess.waitFor();
                currentSpeechProcess = null;
                if (onDone != null) javafx.application.Platform.runLater(onDone);
            } catch (Exception e) {
                currentSpeechProcess = null;
                if (onDone != null) javafx.application.Platform.runLater(onDone);
                e.printStackTrace();
            }
        }).start();
    }

    // =========================================================================
    //  Audio transcription (Whisper)
    // =========================================================================

    public String transcribeAudio(File audioFile) throws Exception {
        String boundary = "----FormBoundary" + System.currentTimeMillis();
        URL url = new URL(WHISPER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            writeField(os, boundary, "model", WHISPER_MODEL);
            writeField(os, boundary, "language", "fr");
            writeField(os, boundary, "response_format", "json");
            writeFile(os, boundary, "file", audioFile);
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        conn.disconnect();

        if (responseCode < 200 || responseCode >= 300)
            throw new Exception("Erreur Whisper (code " + responseCode + "): " + response);

        // Extract text field from JSON
        String json = response.toString();
        int idx = json.indexOf("\"text\"");
        if (idx == -1) return json;
        idx = json.indexOf(":", idx) + 1;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx < json.length() && json.charAt(idx) == '"') {
            idx++;
            StringBuilder content = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '\\' && idx + 1 < json.length()) {
                    char next = json.charAt(idx + 1);
                    if (next == '"') { content.append('"'); idx += 2; }
                    else if (next == 'n') { content.append('\n'); idx += 2; }
                    else if (next == '\\') { content.append('\\'); idx += 2; }
                    else { content.append(c); idx++; }
                } else if (c == '"') { break; }
                else { content.append(c); idx++; }
            }
            return content.toString();
        }
        return json;
    }

    // =========================================================================
    //  Internal helpers
    // =========================================================================

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
                + "{\"role\": \"user\",   \"content\": " + escapeJson(userMessage) + "}"
                + "],"
                + "\"temperature\": 0.9,"
                + "\"max_tokens\": 1024"
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        conn.disconnect();

        if (responseCode < 200 || responseCode >= 300)
            throw new Exception("Erreur API Groq (code " + responseCode + "): " + response);

        return extractContent(response.toString());
    }

    private String extractContent(String jsonResponse) {
        String marker = "\"content\":";
        int idx = jsonResponse.lastIndexOf(marker);
        if (idx == -1) return jsonResponse;
        idx += marker.length();
        while (idx < jsonResponse.length() && (jsonResponse.charAt(idx) == ' ' || jsonResponse.charAt(idx) == '\t')) idx++;
        if (idx < jsonResponse.length() && jsonResponse.charAt(idx) == '"') {
            idx++;
            StringBuilder content = new StringBuilder();
            while (idx < jsonResponse.length()) {
                char c = jsonResponse.charAt(idx);
                if (c == '\\' && idx + 1 < jsonResponse.length()) {
                    char next = jsonResponse.charAt(idx + 1);
                    if (next == '"') { content.append('"'); idx += 2; }
                    else if (next == 'n') { content.append('\n'); idx += 2; }
                    else if (next == 'r') { content.append('\r'); idx += 2; }
                    else if (next == 't') { content.append('\t'); idx += 2; }
                    else if (next == '\\') { content.append('\\'); idx += 2; }
                    else { content.append(c); idx++; }
                } else if (c == '"') { break; }
                else { content.append(c); idx++; }
            }
            return content.toString();
        }
        return jsonResponse.substring(idx);
    }

    private void writeField(OutputStream os, String boundary, String name, String value) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFile(OutputStream os, String boundary, String name, File file) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        os.write("Content-Type: audio/wav\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        Files.copy(file.toPath(), os);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
