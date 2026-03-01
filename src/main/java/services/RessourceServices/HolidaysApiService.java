package services.RessourceServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HolidaysApiService {

    private static final String BASE = "https://date.nager.at/api/v3/PublicHolidays";
    private final HttpClient client = HttpClient.newBuilder().build();

    public CompletableFuture<Map<LocalDate, String>> getHolidaysForYear(int year) {
        String url = BASE + "/" + year + "/TN";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> parseHolidays(r.body()))
                .exceptionally(ex -> Collections.emptyMap());
    }

    private Map<LocalDate, String> parseHolidays(String json) {
        Map<LocalDate, String> map = new HashMap<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                LocalDate date = LocalDate.parse(o.getString("date"));
                String name = o.optString("localName", o.optString("name", "Jour férié"));
                map.put(date, name);
            }
        } catch (Exception ignored) { }
        return map;
    }
}
