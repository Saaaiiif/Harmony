package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Service d'envoi de notification (webhook) pour le module Admin.
 * Peut être branché sur un webhook réel (Discord, Slack, Zapier, etc.).
 */
public class AdminNotificationApiService {

    private static final String WEBHOOK_PLACEHOLDER = "https://webhook.site/unique-id"; // Remplacer par une URL réelle si besoin
    private final HttpClient client = HttpClient.newBuilder().build();

    /** Envoie une notification (POST JSON) lorsqu'une demande de salle est traitée. */
    public CompletableFuture<Boolean> notifyDemandProcessed(String eventTitle, String salleName, boolean accepted) {
        String body = String.format("{\"event\":\"%s\",\"salle\":\"%s\",\"action\":\"%s\"}",
                eventTitle != null ? eventTitle.replace("\"", "'") : "",
                salleName != null ? salleName.replace("\"", "'") : "",
                accepted ? "accepted" : "rejected");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_PLACEHOLDER))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> r.statusCode() >= 200 && r.statusCode() < 300)
                .exceptionally(ex -> false);
    }
}
