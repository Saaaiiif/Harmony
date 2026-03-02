package services.LibraryServices;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches suggestions from:
 *  - Open Library Subject API + Search API (free, no key)
 *  - YouTube Data API v3 (requires key)
 */
public class SuggestionsService {

    private static final String YOUTUBE_API_KEY = "AIzaSyBx0_999HltFSdko33yurEBI18w61LpPAE";
    private static final int    TIMEOUT_MS       = 6000;

    // ── Models ────────────────────────────────────────────────────────────────

    public record BookResult(
            String title,
            String author,
            String coverUrl,
            String openLibUrl
    ) {}

    public record VideoResult(
            String title,
            String channelName,
            String thumbnailUrl,
            String videoUrl
    ) {}

    // ── Open Library ──────────────────────────────────────────────────────────

    /**
     * Smart two-strategy book search:
     * 1. Open Library Subject API  — returns books specifically tagged with this subject
     * 2. Fallback: search for "<subject> textbook" to fill remaining slots
     */
    public List<BookResult> fetchBooks(String subject, int limit) {
        List<BookResult> results = new ArrayList<>();
        if (subject == null || subject.isBlank()) return results;

        subject = subject.trim();

        // ── Strategy 1: Subject API ───────────────────────────────────────────
        // e.g. /subjects/linear_algebra.json — books officially tagged with this subject
        try {
            String slug = subject.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");

            String json = get("https://openlibrary.org/subjects/" + slug + ".json?limit=" + limit);

            if (json != null && json.contains("\"works\"")) {
                int worksStart = json.indexOf("\"works\":[");
                if (worksStart >= 0) {
                    String section = json.substring(worksStart + 9);
                    int pos = 0;
                    while (results.size() < limit) {
                        int start = section.indexOf("{", pos);
                        if (start < 0) break;
                        int end = findMatchingBrace(section, start);
                        if (end < 0) break;
                        String entry = section.substring(start, end + 1);
                        pos = end + 1;

                        String title  = extractString(entry, "title");
                        if (title == null) continue;

                        // Subject API nests author inside authors array of objects
                        String author = extractFirstObjectField(entry, "authors", "name");
                        String coverId = extractValue(entry, "cover_id");
                        String key    = extractString(entry, "key");

                        String coverUrl = toHighResCover(coverId);
                        String libUrl   = key != null ? "https://openlibrary.org" + key : "https://openlibrary.org";

                        results.add(new BookResult(title, author, coverUrl, libUrl));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ── Strategy 2: Textbook search to fill remaining ─────────────────────
        if (results.size() < limit) {
            try {
                int needed  = limit - results.size();
                String query = subject + " textbook";
                String enc  = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String json = get("https://openlibrary.org/search.json?q=" + enc
                        + "&limit=" + (needed + 5)
                        + "&fields=key,title,author_name,cover_i");

                if (json != null) {
                    int docsStart = json.indexOf("\"docs\":[");
                    if (docsStart >= 0) {
                        String section = json.substring(docsStart + 8);
                        int pos = 0;
                        while (results.size() < limit) {
                            int start = section.indexOf("{", pos);
                            if (start < 0) break;
                            int end = findMatchingBrace(section, start);
                            if (end < 0) break;
                            String entry = section.substring(start, end + 1);
                            pos = end + 1;

                            String title = extractString(entry, "title");
                            if (title == null) continue;

                            // Skip duplicates
                            String finalTitle = title;
                            if (results.stream().anyMatch(b -> b.title().equalsIgnoreCase(finalTitle))) continue;

                            String author  = extractArrayFirst(entry, "author_name");
                            String coverId = extractValue(entry, "cover_i");
                            String key     = extractString(entry, "key");

                            String coverUrl = toHighResCover(coverId);
                            String libUrl   = key != null ? "https://openlibrary.org" + key : "https://openlibrary.org/search?q=" + enc;

                            results.add(new BookResult(title, author, coverUrl, libUrl));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    private String toHighResCover(String coverId) {
        if (coverId == null || coverId.isBlank() || coverId.equals("null")) return null;
        return "https://covers.openlibrary.org/b/id/" + coverId.trim() + "-L.jpg";
    }

    // ── YouTube ───────────────────────────────────────────────────────────────

    public List<VideoResult> fetchVideos(String query, int limit) {
        List<VideoResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;
        if (YOUTUBE_API_KEY.equals("YOUR_YOUTUBE_API_KEY")) return results;

        try {
            String encoded = URLEncoder.encode(query.trim() + " tutorial", StandardCharsets.UTF_8);
            String urlStr  = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet&type=video&maxResults=" + limit
                    + "&q=" + encoded
                    + "&key=" + YOUTUBE_API_KEY;

            String json = get(urlStr);
            if (json == null) return results;

            String[] items = json.split("\"kind\":\\s*\"youtube#searchResult\"");

            for (String item : items) {
                if (results.size() >= limit) break;
                if (!item.contains("\"videoId\"")) continue;

                String videoId   = extractString(item, "videoId");
                String title     = extractString(item, "title");
                String channel   = extractString(item, "channelTitle");

                // Best thumbnail: maxres → high → medium
                String thumbnail = extractNestedString(item, "maxres", "url");
                if (thumbnail == null) thumbnail = extractNestedString(item, "high", "url");
                if (thumbnail == null) thumbnail = extractNestedString(item, "medium", "url");

                if (videoId == null || title == null) continue;

                results.add(new VideoResult(
                        htmlDecode(title),
                        channel,
                        thumbnail,
                        "https://www.youtube.com/watch?v=" + videoId
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private String get(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HarmonyApp/1.0");

        if (conn.getResponseCode() != 200) return null;

        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private int findMatchingBrace(String s, int from) {
        int depth = 0;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { if (--depth == 0) return i; }
        }
        return -1;
    }

    /** "key":"value" */
    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        int colon = json.indexOf(":", i + search.length());
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < json.length() && json.charAt(p) == ' ') p++;
        if (p >= json.length() || json.charAt(p) != '"') return null;
        int q2 = p + 1;
        while (q2 < json.length()) {
            if (json.charAt(q2) == '"' && json.charAt(q2 - 1) != '\\') break;
            q2++;
        }
        return json.substring(p + 1, q2)
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\u0027", "'");
    }

    /** "key": 12345 or "key": null */
    private String extractValue(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        int colon = json.indexOf(":", i + search.length());
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < json.length() && json.charAt(p) == ' ') p++;
        if (p >= json.length()) return null;
        if (json.charAt(p) == '"') return extractString(json, key);
        int end = p;
        while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(p, end).trim();
    }

    /** First string in array: "key":["value",...] */
    private String extractArrayFirst(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        int arr = json.indexOf("[", i);
        if (arr < 0) return null;
        int q1 = json.indexOf("\"", arr + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    /** First object in array, extract a field: "key":[{"field":"value"},...] */
    private String extractFirstObjectField(String json, String arrayKey, String field) {
        String search = "\"" + arrayKey + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        int arr = json.indexOf("[", i);
        if (arr < 0) return null;
        int obj = json.indexOf("{", arr);
        if (obj < 0) return null;
        int end = findMatchingBrace(json, obj);
        if (end < 0) return null;
        return extractString(json.substring(obj, end + 1), field);
    }

    /** "parent":{..."child":"value"...} */
    private String extractNestedString(String json, String parent, String child) {
        String search = "\"" + parent + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        int obj = json.indexOf("{", i);
        if (obj < 0) return null;
        int end = findMatchingBrace(json, obj);
        if (end < 0) return null;
        return extractString(json.substring(obj, end + 1), child);
    }

    private String htmlDecode(String s) {
        if (s == null) return null;
        return s.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
    }
}