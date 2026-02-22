package api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Intégration API WorldTimeAPI (heure mondiale)
 * https://worldtimeapi.org/
 */
public class WorldTimeApiService {

    private static final String URL =
            "https://worldtimeapi.org/api/timezone/Europe/Paris";

    private final HttpClient client = HttpClient.newBuilder().build();

    /** Heure actuelle (Paris) au format "HH:mm" et datetime complète. */
    public CompletableFuture<WorldTimeInfo> getCurrentTime() {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)   // 🔹 récupère le String
                .thenApply(this::parse)          // 🔹 envoie au parse(String)
                .exceptionally(ex ->
                        new WorldTimeInfo(null, null, "Erreur API"));
    }

    private WorldTimeInfo parse(String json) {
        try {
            JSONObject o = new JSONObject(json);

            String datetime = o.getString("datetime");

            Instant instant = Instant.parse(datetime.substring(0, 19) + "Z");

            String time = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.of("Europe/Paris"))
                    .format(instant);

            String dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.of("Europe/Paris"))
                    .format(instant);

            return new WorldTimeInfo(time, dateTime, null);

        } catch (Exception e) {
            return new WorldTimeInfo(null, null, "Erreur parsing");
        }
    }

    public static class WorldTimeInfo {
        public final String timeHHmm;
        public final String dateTimeFull;
        public final String error;

        public WorldTimeInfo(String timeHHmm, String dateTimeFull, String error) {
            this.timeHHmm = timeHHmm;
            this.dateTimeFull = dateTimeFull;
            this.error = error;
        }
    }
}