package api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Intégration API Nager.Date (jours fériés France) pour le module Calendrier.
 * https://date.nager.at/
 */
public class HolidaysApiService {

    private static final String BASE = "https://date.nager.at/api/v3/PublicHolidays";
    private final HttpClient client = HttpClient.newBuilder().build();

    /** Récupère les jours fériés pour une année (France). */
    public CompletableFuture<Set<LocalDate>> getHolidaysForYear(int year) {
        String url = BASE + "/" + year + "/FR";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> parseHolidays(r.body()))
                .exceptionally(ex -> Collections.emptySet());
    }

    private Set<LocalDate> parseHolidays(String json) {
        Set<LocalDate> set = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                set.add(LocalDate.parse(o.getString("date")));
            }
        } catch (Exception ignored) { }
        return set;
    }
}
