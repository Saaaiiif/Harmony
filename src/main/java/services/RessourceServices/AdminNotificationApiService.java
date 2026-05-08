package services.RessourceServices;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;


public class AdminNotificationApiService {

    private static final String WEBHOOK_URL =
            "https://webhook.site/f40e82c6-6254-40c9-917e-f4df6e0cc29b";

    private final HttpClient client = HttpClient.newHttpClient();

    
    public CompletableFuture<Boolean> notifyDemandProcessed(
            String eventTitle,
            String salleName,
            boolean accepted
    ) {
        String body = String.format(
                "{ \"content\": \"📢 **Demande de salle %s**\\n📌 Événement: %s\\n🏢 Salle: %s\" }",
                accepted ? "ACCEPTÉE" : "REFUSÉE",
                eventTitle,
                salleName
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Status Code: " + response.statusCode());
                    System.out.println("Response Body: " + response.body());
                    return response.statusCode() >= 200 && response.statusCode() < 300;
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return false;
                });
    }
}
