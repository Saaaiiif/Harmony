package services.LibraryServices;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ImageGenerationService {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell";


    public byte[] generateImage(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "image/png");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);

            String requestBody = String.format(
                    "{\"inputs\":\"%s\"}",
                    prompt.replace("\"", "\\\"")
            );

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream()) {
                    return is.readAllBytes();
                }
            } else {
                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) System.err.println("Error: " + new String(es.readAllBytes()));
                }
                System.err.println("Error response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] generateCourseImage(String courseTitle, String subject) {
        String prompt = String.format(
                "A sleek minimalist course cover icon for an online education platform. " +
                        "The course is titled \"%s\" and teaches \"%s\" as an academic subject. " +
                        "Interpret \"%s\" strictly as an educational or technical discipline, not literally. " +
                        "For example if the subject is Python it means the programming language, " +
                        "if it is Java it means software development, if it is Biology it means life sciences. " +
                        "Flat vector illustration, modern app icon style, subtle gradient, " +
                        "centered composition, soft geometric shapes, professional edu-tech aesthetic, " +
                        "vibrant but clean color palette, no text, no letters, no words, no animals unless abstractly symbolic",
                courseTitle, subject, subject
        );
        return generateImage(prompt);
    }
}
