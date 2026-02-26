package services.ForumServices;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslationService {

    public String translate(String text, String sourceLang, String targetLang)
            throws IOException, InterruptedException {

        if (text == null || text.isEmpty()) {
            return "Texte vide !";
        }

        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

        String langPair = sourceLang + "%7C" + targetLang;

        String url = "https://api.mymemory.translated.net/get?q="
                + encodedText +
                "&langpair=" + langPair;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();

        System.out.println("API RESPONSE: " + body);

        int start = body.indexOf("\"translatedText\":");
        if (start == -1) return "Erreur extraction";

        start = body.indexOf("\"", start + 17) + 1;
        int end = body.indexOf("\"", start);

        return body.substring(start, end);
    }
}