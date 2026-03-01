package services.ForumServices;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


public class ImageGenerationService {

    private static final String API_KEY = "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"; // ← coller votre token ici

    // Modèle gratuit stable-diffusion
    private static final String MODEL_URL =
            "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0";

    private static final int MAX_RETRIES = 3;

    /**
     * Génère une image et retourne les bytes PNG directement.
     */
    public byte[] generateImageBytes(String prompt, String style) throws Exception {
        String fullPrompt = (style != null && !style.isEmpty())
                ? prompt + ", " + style + ", high quality, 4k, detailed"
                : prompt + ", high quality, 4k, detailed";

        String jsonBody = "{\"inputs\": \"" + fullPrompt.replace("\"", "\\\"") + "\"}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            System.out.println("🚀 Tentative " + attempt + "/" + MAX_RETRIES);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            System.out.println("📡 Status HTTP: " + response.statusCode());
            System.out.println("📦 Bytes reçus: " + response.body().length);

            if (response.statusCode() == 200 && response.body().length > 1000) {
                return response.body(); // ✅ succès
            }

            if (response.statusCode() == 503) {
                // Modèle en cours de chargement sur HF, attendre
                System.out.println("⏳ Modèle en chargement, attente 25s...");
                Thread.sleep(25000);
                continue;
            }

            // Autre erreur
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new RuntimeException("Erreur API (" + response.statusCode() + "): " + body);
        }

        throw new RuntimeException("Échec après " + MAX_RETRIES + " tentatives.");
    }
}
