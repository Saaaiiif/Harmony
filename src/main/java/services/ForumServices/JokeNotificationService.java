package services.ForumServices;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import org.json.JSONObject;

/**
 * Service de notifications fun : affiche une blague/fait amusant toutes les X minutes
 * via JokeAPI (https://v2.jokeapi.dev) dans un toast animé.
 */
public class JokeNotificationService {

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String JOKE_API_URL =
            "https://v2.jokeapi.dev/joke/Any?lang=fr&blacklistFlags=nsfw,racist,sexist,explicit";
    private static final int INTERVAL_MINUTES = 3; // toutes les 3 minutes
    private static final int TOAST_DURATION_SECONDS = 7; // durée d'affichage

    // ── State ─────────────────────────────────────────────────────────────────
    private ScheduledExecutorService scheduler;
    private StackPane overlayContainer; // injecté depuis le controller

    // ── Constructor ───────────────────────────────────────────────────────────
    public JokeNotificationService(StackPane overlayContainer) {
        this.overlayContainer = overlayContainer;
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JokeNotifThread");
            t.setDaemon(true); // thread daemon → s'arrête avec l'appli
            return t;
        });

        // Première blague après 5 secondes, puis toutes les INTERVAL_MINUTES
        scheduler.scheduleAtFixedRate(
                this::fetchAndShowJoke,
                5,
                INTERVAL_MINUTES * 60L,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // ── Fetch blague depuis l'API ─────────────────────────────────────────────

    private void fetchAndShowJoke() {
        try {
            URL url = new URL(JOKE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                showToast("😄", "Impossible de charger la blague du moment !");
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            String type = json.optString("type", "single");

            String jokeText;
            if ("twopart".equals(type)) {
                String setup = json.optString("setup", "");
                String delivery = json.optString("delivery", "");
                jokeText = setup + "\n👉 " + delivery;
            } else {
                jokeText = json.optString("joke", "Aucune blague trouvée 😅");
            }

            showToast("😂", jokeText);

        } catch (Exception e) {
            // En cas d'erreur réseau → blague de secours
            showToast("🤓", getFallbackJoke());
        }
    }

    // ── Afficher le toast sur le thread JavaFX ────────────────────────────────

    private void showToast(String emoji, String message) {
        Platform.runLater(() -> {
            VBox toast = buildToast(emoji, message);

            // Positionner en bas à droite
            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toast, new Insets(0, 24, 24, 0));

            overlayContainer.getChildren().add(toast);
            animateToast(toast);
        });
    }

    // ── Construction du composant toast ──────────────────────────────────────

    private VBox buildToast(String emoji, String message) {
        // Conteneur principal
        VBox toast = new VBox(6);
        toast.getStyleClass().add("joke-toast");
        toast.setMaxWidth(320);
        toast.setPickOnBounds(false);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(emoji);
        emojiLbl.getStyleClass().add("joke-toast-emoji");

        Label titleLbl = new Label("Blague du moment 😄");
        titleLbl.getStyleClass().add("joke-toast-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeLbl = new Label("✕");
        closeLbl.getStyleClass().add("joke-toast-close");
        closeLbl.setOnMouseClicked(e -> {
            // Retrait immédiat au clic
            FadeTransition ft = new FadeTransition(Duration.millis(200), toast);
            ft.setToValue(0);
            ft.setOnFinished(ev -> overlayContainer.getChildren().remove(toast));
            ft.play();
        });

        header.getChildren().addAll(emojiLbl, titleLbl, spacer, closeLbl);

        // Corps : texte de la blague
        Label messageLbl = new Label(message);
        messageLbl.getStyleClass().add("joke-toast-message");
        messageLbl.setWrapText(true);
        messageLbl.setMaxWidth(290);

        // Barre de progression (timer visuel)
        ProgressBar progressBar = new ProgressBar(1.0);
        progressBar.getStyleClass().add("joke-toast-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);

        toast.getChildren().addAll(header, messageLbl, progressBar);
        return toast;
    }

    // ── Animation : slide-in + auto-dismiss ───────────────────────────────────

    private void animateToast(VBox toast) {
        // Slide-in depuis le bas
        toast.setTranslateY(80);
        toast.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), toast);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), toast);
        fadeIn.setToValue(1.0);

        ParallelTransition intro = new ParallelTransition(slide, fadeIn);

        // Progress bar qui se vide sur la durée
        ProgressBar pb = (ProgressBar) toast.getChildren().get(2);
        Timeline countdown = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pb.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(TOAST_DURATION_SECONDS),
                        new KeyValue(pb.progressProperty(), 0.0, Interpolator.LINEAR))
        );

        // Fade-out + slide vers le bas avant suppression
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toast);
        slideOut.setToY(60);
        ParallelTransition outro = new ParallelTransition(fadeOut, slideOut);
        outro.setOnFinished(e -> overlayContainer.getChildren().remove(toast));

        // Enchaînement
        intro.setOnFinished(e -> {
            countdown.play();
            PauseTransition pause = new PauseTransition(Duration.seconds(TOAST_DURATION_SECONDS));
            pause.setOnFinished(ev -> outro.play());
            pause.play();
        });

        intro.play();
    }

    // ── Blagues de secours (hors-ligne) ──────────────────────────────────────

    private String getFallbackJoke() {
        String[] fallbacks = {
                "Pourquoi les plongeurs plongent-ils toujours en arrière ? Parce que sinon ils tomberaient dans le bateau ! 🚤",
                "Un homme entre dans une bibliothèque et demande : «Avez-vous des livres sur la paranoïa ?» La bibliothécaire chuchote : «Ils sont juste derrière vous...» 👀",
                "Qu'est-ce qu'un canif ? C'est le petit fils du canif ! 🔪... Non, attendez...",
                "C'est l'histoire d'un homme qui rentre dans un bar... Aïe ! 🍺",
                "Pourquoi les vampires sont-ils nuls en maths ? Parce qu'ils ne peuvent pas compter sur leurs doigts ! 🧛"
        };
        return fallbacks[(int) (Math.random() * fallbacks.length)];
    }
}