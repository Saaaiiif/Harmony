package api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Appel API publique pour afficher des stats / données externes dans l'admin.
 * Exemple : API REST Countries
 */
public class PublicStatsApiService {

    private static final String URL =
            "https://restcountries.com/v3.1/name/France?fields=name,population,capital";

    private final HttpClient client = HttpClient.newBuilder().build();

    /** Récupère une info externe (ex. pays) pour le tableau de bord admin. */
    public CompletableFuture<String> getExternalInfo() {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)   // récupère le body (String)
                .thenApply(this::parse)          // parse le JSON
                .exceptionally(ex -> "Données indisponibles");
    }

    private String parse(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.isEmpty()) return "—";

            JSONObject o = arr.getJSONObject(0);

            String name = o.getJSONObject("name")
                    .optString("common", "?");

            String capital = o.optJSONArray("capital") != null
                    ? o.getJSONArray("capital").optString(0, "?")
                    : "?";

            String population = String.valueOf(o.optLong("population", 0));

            return name + " • Capitale : " + capital + " • Population : " + population;

        } catch (Exception e) {
            return "—";
        }
    }
}