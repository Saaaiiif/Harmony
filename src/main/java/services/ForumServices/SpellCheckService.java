package services.ForumServices;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

public class SpellCheckService {

    private static final String API_URL =
            "https://api.languagetool.org/v2/check";

    public String correctText(String text) {

        try {

            String params = "text=" + text +
                    "&language=fr";

            URL url = new URL(API_URL);
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input =
                        params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            return applyCorrections(text, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    private String applyCorrections(String originalText,
                                    String jsonResponse) {

        JSONObject json = new JSONObject(jsonResponse);
        JSONArray matches = json.getJSONArray("matches");

        String correctedText = originalText;

        for (int i = matches.length() - 1; i >= 0; i--) {

            JSONObject match = matches.getJSONObject(i);

            int offset = match.getInt("offset");
            int length = match.getInt("length");

            JSONArray replacements =
                    match.getJSONArray("replacements");

            if (replacements.length() > 0) {

                String replacement =
                        replacements
                                .getJSONObject(0)
                                .getString("value");

                correctedText =
                        correctedText.substring(0, offset)
                                + replacement
                                + correctedText.substring(offset + length);
            }
        }

        return correctedText;
    }
}