package utils;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

/**
 * Service de génération de CAPTCHA visuel natif JavaFX.
 *
 * Génère un Canvas avec :
 *  - Des caractères alphanumériques légèrement rotatifs et déformés
 *  - Du bruit de fond (lignes et points aléatoires)
 *  - Une palette de couleurs cohérente avec le thème Harmony (violet/bleu)
 *
 * Aucune dépendance externe requise — 100 % JavaFX natif.
 */
public class CaptchaService {

    // Largeur et hauteur du canvas CAPTCHA
    public static final double CAPTCHA_WIDTH  = 230;
    public static final double CAPTCHA_HEIGHT = 65;

    // Caractères autorisés : on exclut les ambigus (0, O, l, 1, I, Q)
    private static final String CHARSET = "ABCDEFGHJKLMNPRSTUVWXYZ23456789";

    // Longueur du code CAPTCHA
    private static final int CODE_LENGTH = 5;

    private static final Random RANDOM = new Random();

    // Code actuellement généré (à comparer avec la saisie utilisateur)
    private String currentCode = "";

    /**
     * Génère un nouveau code CAPTCHA aléatoire et retourne le Canvas prêt à afficher.
     */
    public Canvas generateCaptchaCanvas() {
        currentCode = generateCode();
        Canvas canvas = new Canvas(CAPTCHA_WIDTH, CAPTCHA_HEIGHT);
        drawCaptcha(canvas, currentCode);
        return canvas;
    }

    /**
     * Retourne le code CAPTCHA actuellement actif (insensible à la casse).
     */
    public String getCurrentCode() {
        return currentCode;
    }

    /**
     * Vérifie si la saisie de l'utilisateur correspond au CAPTCHA (insensible à la casse).
     */
    public boolean validate(String userInput) {
        if (userInput == null || currentCode == null) return false;
        return userInput.trim().equalsIgnoreCase(currentCode.trim());
    }

    // ==================== Génération du code ====================

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    // ==================== Rendu graphique ====================

    private void drawCaptcha(Canvas canvas, String code) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // --- Fond avec léger dégradé (couleur claire, cohérente avec les champs input) ---
        gc.setFill(Color.rgb(243, 244, 246)); // #F3F4F6
        gc.fillRoundRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT, 12, 12);

        // --- Bordure subtile violette ---
        gc.setStroke(Color.rgb(139, 92, 246, 0.4)); // #8B5CF6 semi-transparent
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(0.75, 0.75, CAPTCHA_WIDTH - 1.5, CAPTCHA_HEIGHT - 1.5, 12, 12);

        // --- Bruit de fond : points aléatoires ---
        drawNoisePoints(gc);

        // --- Bruit de fond : lignes parasites ---
        drawNoiseLines(gc);

        // --- Caractères distordus ---
        drawCharacters(gc, code);
    }

    private void drawNoisePoints(GraphicsContext gc) {
        for (int i = 0; i < 120; i++) {
            double x = RANDOM.nextDouble() * CAPTCHA_WIDTH;
            double y = RANDOM.nextDouble() * CAPTCHA_HEIGHT;
            double size = 1 + RANDOM.nextDouble() * 2;

            // Couleurs dans la palette violette/grise du thème
            Color[] noiseColors = {
                Color.rgb(139, 92,  246, 0.15),  // violet clair
                Color.rgb(107, 114, 128, 0.15),  // gris
                Color.rgb(124, 58,  237, 0.12),  // violet foncé
                Color.rgb(167, 139, 250, 0.20),  // lavande
            };
            gc.setFill(noiseColors[RANDOM.nextInt(noiseColors.length)]);
            gc.fillOval(x, y, size, size);
        }
    }

    private void drawNoiseLines(GraphicsContext gc) {
        Color[] lineColors = {
            Color.rgb(139, 92,  246, 0.18),
            Color.rgb(124, 58,  237, 0.15),
            Color.rgb(107, 114, 128, 0.12),
        };

        for (int i = 0; i < 5; i++) {
            gc.setStroke(lineColors[RANDOM.nextInt(lineColors.length)]);
            gc.setLineWidth(0.8 + RANDOM.nextDouble() * 0.8);

            double x1 = RANDOM.nextDouble() * CAPTCHA_WIDTH * 0.3;
            double y1 = RANDOM.nextDouble() * CAPTCHA_HEIGHT;
            double x2 = CAPTCHA_WIDTH * 0.7 + RANDOM.nextDouble() * CAPTCHA_WIDTH * 0.3;
            double y2 = RANDOM.nextDouble() * CAPTCHA_HEIGHT;

            // Légère courbure via une ligne brisée en 3 segments
            double mx = (x1 + x2) / 2 + (RANDOM.nextDouble() - 0.5) * 30;
            double my = (y1 + y2) / 2 + (RANDOM.nextDouble() - 0.5) * 20;
            gc.strokeLine(x1, y1, mx, my);
            gc.strokeLine(mx, my, x2, y2);
        }
    }

    private void drawCharacters(GraphicsContext gc, String code) {
        // Espacement horizontal entre les caractères
        double slotWidth = (CAPTCHA_WIDTH - 20) / CODE_LENGTH;

        // Palettes de couleurs pour les caractères (tons violets/bleus du thème)
        Color[] charColors = {
            Color.rgb(109, 40,  217),   // violet profond
            Color.rgb(124, 58,  237),   // violet principal
            Color.rgb(67,  56,  202),   // indigo
            Color.rgb(79,  70,  229),   // violet-bleu
            Color.rgb(91,  33,  182),   // violet sombre
        };

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Taille de police aléatoire entre 22 et 30
            double fontSize = 22 + RANDOM.nextInt(9);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));

            // Couleur alternée
            gc.setFill(charColors[i % charColors.length]);

            // Position X centrée dans son slot
            double x = 10 + i * slotWidth + slotWidth / 2;

            // Position Y légèrement variable (oscillation verticale)
            double baseY = CAPTCHA_HEIGHT / 2 + 8;
            double y = baseY + (RANDOM.nextDouble() - 0.5) * 12;

            // Rotation aléatoire [-20°, +20°]
            double angle = (RANDOM.nextDouble() - 0.5) * 40;
            double angleRad = Math.toRadians(angle);

            // Sauvegarde du contexte graphique, application de la rotation
            gc.save();
            gc.translate(x, y);
            gc.rotate(angle);

            // Ombre légère pour la lisibilité
            gc.setFill(Color.rgb(255, 255, 255, 0.6));
            gc.fillText(String.valueOf(c), 1.5, 1.5);

            // Caractère principal
            gc.setFill(charColors[i % charColors.length]);
            gc.fillText(String.valueOf(c), 0, 0);

            gc.restore();
        }
    }
}
