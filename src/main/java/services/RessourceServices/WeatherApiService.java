package services.RessourceServices;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class WeatherApiService {

    private static final String BASE = "https://api.open-meteo.com/v1/forecast";
    private final HttpClient client = HttpClient.newBuilder().build();

    public CompletableFuture<WeatherInfo> getWeatherForDate(LocalDate date) {
        String url = BASE + "?latitude=36.8065&longitude=10.1815&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=Africa/Tunis&start_date="
                + date + "&end_date=" + date;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> parseWeather(r.body(), date))
                .exceptionally(ex -> new WeatherInfo(date, null, null, "Données météo indisponibles", ex.getMessage()));
    }

    private WeatherInfo parseWeather(String json, LocalDate date) {
        try {
            JSONObject o = new JSONObject(json);
            if (!o.has("daily")) return new WeatherInfo(date, null, null, "—", null);
            JSONObject daily = o.getJSONObject("daily");
            double max = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double min = daily.getJSONArray("temperature_2m_min").getDouble(0);
            int code = daily.getJSONArray("weathercode").getInt(0);
            String desc = weatherCodeToDesc(code);
            return new WeatherInfo(date, max, min, desc, null);
        } catch (Exception e) {
            return new WeatherInfo(date, null, null, "—", e.getMessage());
        }
    }

    private String weatherCodeToDesc(int code) {
        if (code == 0) return "Ensoleillé";
        if (code <= 3) return "Nuageux";
        if (code <= 67) return "Pluie";
        if (code <= 77) return "Neige";
        if (code <= 82) return "Averses";
        if (code <= 86) return "Neige";
        return "Orage";
    }

    public static class WeatherInfo {
        public final LocalDate date;
        public final Double tempMax;
        public final Double tempMin;
        public final String description;
        public final String error;

        public WeatherInfo(LocalDate date, Double tempMax, Double tempMin, String description, String error) {
            this.date = date;
            this.tempMax = tempMax;
            this.tempMin = tempMin;
            this.description = description;
            this.error = error;
        }
    }
}
