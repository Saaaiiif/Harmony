package com.example.harmony.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

/**
 * Service class for interacting with the Stability AI API to generate images from text prompts.
 */
public class StabilityAIService {
    private static final String API_KEY = "sk-P67AON0z7DHlFzyvgks4egpRqbVKJEqgUz4OYjM2f7dTtHh8";
    private static final String API_URL = "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image";

    /**
     * Generates an image based on the provided text prompt using Stability AI.
     *
     * @param prompt The text prompt to generate an image from
     * @return The generated image as a byte array, or null if generation failed
     */
    public byte[] generateImage(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setDoOutput(true);

            // Create the request body
            String requestBody = String.format(
                "{\"text_prompts\":[{\"text\":\"%s\"}],\"cfg_scale\":7,\"height\":1024,\"width\":1024,\"samples\":1,\"steps\":30}",
                prompt.replace("\"", "\\\"")
            );

            // Send the request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check if the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse the response
                String response = readResponse(connection.getInputStream());
                
                // Extract the base64-encoded image from the response
                String base64Image = extractBase64Image(response);
                if (base64Image != null) {
                    // Decode the base64 string to a byte array
                    return Base64.getDecoder().decode(base64Image);
                }
            } else {
                System.err.println("Error response code: " + responseCode);
                String errorResponse = readResponse(connection.getErrorStream());
                System.err.println("Error response: " + errorResponse);
            }
        } catch (IOException e) {
            System.err.println("Error generating image: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Reads the response from an input stream.
     *
     * @param inputStream The input stream to read from
     * @return The response as a string
     */
    private String readResponse(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Extracts the base64-encoded image from the API response.
     *
     * @param response The API response as a string
     * @return The base64-encoded image, or null if not found
     */
    private String extractBase64Image(String response) {
        // Simple parsing for the base64 image
        // In a real implementation, you would use a JSON parser
        int startIndex = response.indexOf("\"base64\":\"") + 10;
        if (startIndex > 10) {
            int endIndex = response.indexOf("\"", startIndex);
            if (endIndex > startIndex) {
                return response.substring(startIndex, endIndex);
            }
        }
        return null;
    }
}