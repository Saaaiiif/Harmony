package services.RessourceServices;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AdviceApiService {

    private static final String URL = "https://api.adviceslip.com/advice";
    private final HttpClient client = HttpClient.newBuilder().build();

    public CompletableFuture<String> getRandomAdvice() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> parseAdvice(r.body()))
                .exceptionally(ex -> "Concentrez-vous sur une tâche à la fois.");
    }

    private String parseAdvice(String json) {
        try {
            JSONObject o = new JSONObject(json);
            return o.getJSONObject("slip").getString("advice");
        } catch (Exception e) {
            return "Conseil du jour : avancez pas à pas.";
        }
    }
}
