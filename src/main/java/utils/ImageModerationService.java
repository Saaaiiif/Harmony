package utils;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.regex.*;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  ImageModerationService — Vérification de contenu inapproprié via Sightengine
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  API : Sightengine  (https://sightengine.com)
 *  Plan GRATUIT : 500 opérations/mois — aucune carte de crédit requise.
 *
 *  ── INSCRIPTION (2 minutes) ──────────────────────────────────────────────
 *   1. https://dashboard.sightengine.com/signup
 *   2. Dashboard → API Credentials
 *   3. Copier « API User »  et « API Secret »
 *   4. Remplacer les deux constantes ci-dessous :
 *
 *        API_USER   = "xxxxxx"
 *        API_SECRET = "xxxxxxxxxxxxxxxxxxxxxx"
 *
 *  ── MODÈLES ACTIVÉS ──────────────────────────────────────────────────────
 *   nudity    : nudité partielle / totale / sexuelle
 *   gore      : sang, violence graphique, blessures
 *   offensive : gestes offensants, symboles haineux
 *   wad       : weapons, alcohol, drugs
 *
 *  ── SEUILS DE REJET ──────────────────────────────────────────────────────
 *   Sexuel explicite  > 0.40
 *   Érotique          > 0.55
 *   Suggestif excessif> 0.70
 *   Gore / violence   > 0.45
 *   Offensant         > 0.60
 *   Arme à feu        > 0.65
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class ImageModerationService {

    /* ── ✅ Remplacez par vos credentials Sightengine ─────────────────────── */
    private static final String API_USER   = "523880973";
    private static final String API_SECRET = "MaWbGEam6EvJ6CS75LoHaeyanr2ezCuL";
    /* ──────────────────────────────────────────────────────────────────────── */

    private static final String ENDPOINT = "https://api.sightengine.com/1.0/check.json";
    private static final String MODELS   = "nudity,gore,offensive,wad";

    // Seuils
    private static final double SEUIL_SEXUEL     = 0.40;
    private static final double SEUIL_EROTIQUE   = 0.55;
    private static final double SEUIL_SUGGESTIF  = 0.70;
    private static final double SEUIL_GORE       = 0.45;
    private static final double SEUIL_OFFENSANT  = 0.60;
    private static final double SEUIL_ARME       = 0.65;

    // ── Résultat ────────────────────────────────────────────────────────────

    public static final class ModerationResult {
        private final boolean approved;
        private final String  reason;

        ModerationResult(boolean approved, String reason) {
            this.approved = approved;
            this.reason   = reason;
        }

        public boolean isApproved() { return approved; }
        public String  getReason()  { return reason;   }
    }

    // ── Point d'entrée ──────────────────────────────────────────────────────

    /**
     * Analyse une image et retourne un résultat de modération.
     * ⚠ À appeler depuis un thread de fond (pas le thread JavaFX).
     *
     * @param imageFile fichier image à analyser
     * @return ModerationResult
     * @throws IOException          si lecture fichier ou réseau impossible
     * @throws InterruptedException si le thread est interrompu
     */
    public ModerationResult checkImage(File imageFile)
            throws IOException, InterruptedException {

        if (imageFile == null || !imageFile.exists())
            return new ModerationResult(false, "Fichier introuvable");

        if (imageFile.length() > 30L * 1024 * 1024)
            return new ModerationResult(false, "Image trop volumineuse (max 30 Mo)");

        String boundary = "HBoundary" + System.currentTimeMillis();
        byte[] body     = buildMultipart(imageFile, boundary);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        String json = response.body();
        System.out.println("[Moderation] status=" + response.statusCode()
                + "  body=" + json);

        return parseResponse(json);
    }

    // ── Construction multipart (sans dépendance externe) ───────────────────

    private byte[] buildMultipart(File file, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        String sep  = "--" + boundary + CRLF;
        String end  = "--" + boundary + "--" + CRLF;

        // fichier image
        out.write(sep.getBytes());
        out.write(("Content-Disposition: form-data; name=\"media\"; filename=\""
                + file.getName() + "\"" + CRLF).getBytes());
        out.write(("Content-Type: " + mime(file) + CRLF + CRLF).getBytes());
        out.write(Files.readAllBytes(file.toPath()));
        out.write(CRLF.getBytes());

        // models
        appendField(out, boundary, "models", MODELS, CRLF);
        // api_user
        appendField(out, boundary, "api_user", API_USER, CRLF);
        // api_secret
        appendField(out, boundary, "api_secret", API_SECRET, CRLF);

        out.write(end.getBytes());
        return out.toByteArray();
    }

    private void appendField(ByteArrayOutputStream out, String boundary,
                             String name, String value, String CRLF) throws IOException {
        out.write(("--" + boundary + CRLF).getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF + CRLF).getBytes());
        out.write(value.getBytes());
        out.write(CRLF.getBytes());
    }

    private String mime(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".gif"))  return "image/gif";
        if (n.endsWith(".bmp"))  return "image/bmp";
        if (n.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    // ── Analyse JSON (regex — aucune lib externe) ──────────────────────────

    private ModerationResult parseResponse(String json) {

        if (json == null || json.isBlank())
            return failSafe("Réponse vide de l'API");

        // Erreur d'authentification ou quota dépassé → ne pas bloquer l'utilisateur
        if (!json.contains("\"success\"")) {
            String msg = str(json, "message");
            System.err.println("[Moderation] Erreur API: " + msg);
            // On laisse passer : l'API étant indisponible on ne punit pas l'utilisateur
            return new ModerationResult(true, "Vérification indisponible — image acceptée par défaut");
        }

        // ── Nudité sexuelle explicite ────────────────────────────────────────
        // Sightengine retourne "nudity": { "sexual_activity": x, "sexual_display": x,
        //   "erotica": x, "suggestive": x, "none": x, ... }
        double sexAct  = dbl(json, "sexual_activity");
        double sexDisp = dbl(json, "sexual_display");
        double erotica = dbl(json, "erotica");
        double suggestive = dbl(json, "suggestive");

        if (sexAct  > SEUIL_SEXUEL)
            return new ModerationResult(false,
                    "Contenu sexuel explicite détecté (" + pct(sexAct) + ")");
        if (sexDisp > SEUIL_SEXUEL)
            return new ModerationResult(false,
                    "Affichage sexuel détecté (" + pct(sexDisp) + ")");
        if (erotica > SEUIL_EROTIQUE)
            return new ModerationResult(false,
                    "Contenu érotique détecté (" + pct(erotica) + ")");
        if (suggestive > SEUIL_SUGGESTIF)
            return new ModerationResult(false,
                    "Contenu suggestif inapproprié (" + pct(suggestive) + ")");

        // ── Gore / violence ─────────────────────────────────────────────────
        // "gore": { "prob": x }
        double goreProb = nested(json, "gore", "prob");
        if (goreProb > SEUIL_GORE)
            return new ModerationResult(false,
                    "Violence / gore détecté (" + pct(goreProb) + ")");

        // ── Contenu offensant ───────────────────────────────────────────────
        // "offensive": { "prob": x }
        double offProb = nested(json, "offensive", "prob");
        if (offProb > SEUIL_OFFENSANT)
            return new ModerationResult(false,
                    "Contenu offensant détecté (" + pct(offProb) + ")");

        // ── Armes à feu ─────────────────────────────────────────────────────
        // "weapon": { "classes": { "firearm": x, ... } }
        double firearm = dbl(json, "firearm");
        if (firearm > SEUIL_ARME)
            return new ModerationResult(false,
                    "Arme à feu détectée (" + pct(firearm) + ")");

        return new ModerationResult(true, "Image approuvée");
    }

    // En cas d'erreur inattendue, on refuse l'image par précaution
    private ModerationResult failSafe(String reason) {
        return new ModerationResult(false, reason);
    }

    // ── Utilitaires de parsing ─────────────────────────────────────────────

    /** Extrait la première valeur numérique associée à la clé. */
    private double dbl(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key)
                + "\"\\s*:\\s*([0-9]+\\.?[0-9]*)").matcher(json);
        return m.find() ? safe(m.group(1)) : 0.0;
    }

    /** Extrait la valeur d'un champ dans un objet imbriqué {parent: {child: v}}. */
    private double nested(String json, String parent, String child) {
        int pi = json.indexOf("\"" + parent + "\"");
        if (pi < 0) return 0.0;
        int bi = json.indexOf('{', pi);
        if (bi < 0) return 0.0;
        // Trouver l'accolade fermante correspondante
        int depth = 0, end = bi;
        for (int i = bi; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') { if (--depth == 0) { end = i; break; } }
        }
        return dbl(json.substring(bi, end + 1), child);
    }

    private String str(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key)
                + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private double safe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private String pct(double v) {
        return String.format("%.0f%%", v * 100);
    }
}
