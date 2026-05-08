package controllers.UserControlleers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;
import utils.FaceRecognitionService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller du popup de vérification biométrique lors de la connexion.
 *
 * Flux automatique :
 *   1. setup() est appelé par LoginController AVANT showAndWait()
 *      → storedFacePath et popupStage sont affectés
 *      → la caméra démarre à ce moment (plus de risque de null)
 *   2. Toutes les AUTO_CHECK_DELAY secondes, si un visage est détecté,
 *      on tente une comparaison automatique avec le visage stocké
 *   3. Si identique → isVerified = true, popup se ferme
 *   4. Après MAX_AUTO_ATTEMPTS tentatives échouées → popup se ferme (échec)
 *   5. L'utilisateur peut aussi cliquer "Vérifier maintenant" manuellement
 */
public class FaceVerificationController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ImageView  cameraView;
    @FXML private VBox       cameraPlaceholder;
    @FXML private Label      statusLabel;
    @FXML private Label      attemptsLabel;
    @FXML private Label      overlayResult;
    @FXML private ProgressBar progressBar;
    @FXML private Button     btnVerifier;

    // ── Services ─────────────────────────────────────────────────────────────
    private final FaceRecognitionService faceService = new FaceRecognitionService();

    // ── Configuration ────────────────────────────────────────────────────────
    private static final int MAX_AUTO_ATTEMPTS = 5;
    private static final int AUTO_CHECK_DELAY  = 3; // secondes entre tentatives auto

    // ── État ──────────────────────────────────────────────────────────────────
    private String                   storedFacePath;      // chemin du visage en BDD
    private Stage                    popupStage;          // stage de ce popup
    private boolean                  verified      = false;
    private boolean                  faceAvailable = false; // visage actuellement détecté
    private final AtomicInteger      autoAttempts  = new AtomicInteger(0);
    private ScheduledExecutorService autoCheckExecutor;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        // On initialise seulement les valeurs UI de base.
        // La caméra sera démarrée dans setup() APRÈS que storedFacePath
        // et popupStage aient été affectés par LoginController.
        progressBar.setProgress(0);
        overlayResult.setVisible(false);
        statusLabel.setText("🔍  Initialisation...");
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #8B5CF6;");
    }

    /**
     * Configure le popup avec le chemin du visage stocké et le Stage parent.
     * Doit être appelé AVANT showAndWait().
     *
     * ✅ CORRECTION : c'est ici (et non dans initialize()) que la caméra démarre,
     * garantissant que storedFacePath et popupStage sont non-null au démarrage.
     */
    public void setup(String storedFacePath, Stage stage) {
        this.storedFacePath = storedFacePath;
        this.popupStage = stage;

        // Fermeture propre si l'utilisateur ferme la fenêtre via la croix
        stage.setOnCloseRequest(e -> {
            verified = false;
            stopAllResources();
        });

        // ✅ Démarrage caméra ici, après affectation des champs critiques
        Platform.runLater(this::startCameraAndAutoCheck);
    }

    // =========================================================================
    //  CAMÉRA + VÉRIFICATION AUTOMATIQUE
    // =========================================================================

    private void startCameraAndAutoCheck() {
        try {
            faceService.startCamera((frame, faceFound) -> {
                if (frame != null) {
                    cameraView.setImage(frame);
                    cameraPlaceholder.setVisible(false);
                    cameraPlaceholder.setManaged(false);
                }
                faceAvailable = faceFound;

                if (faceFound) {
                    statusLabel.setText("✅  Visage détecté — vérification en cours...");
                    statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #059669;");
                } else {
                    statusLabel.setText("🔍  Aucun visage détecté — positionnez-vous face à la caméra");
                    statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #8B5CF6;");
                }
            });

            // Vérification automatique toutes les AUTO_CHECK_DELAY secondes
            autoCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FaceAutoCheck-Thread");
                t.setDaemon(true);
                return t;
            });

            autoCheckExecutor.scheduleAtFixedRate(() -> {
                if (verified || autoAttempts.get() >= MAX_AUTO_ATTEMPTS) return;
                if (faceAvailable) {
                    performComparison();
                }
            }, AUTO_CHECK_DELAY, AUTO_CHECK_DELAY, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("❌  Impossible d'accéder à la caméra.");
                statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
                btnVerifier.setDisable(true);
            });
        }
    }

    // =========================================================================
    //  ACTIONS UTILISATEUR
    // =========================================================================

    /**
     * Clic manuel sur "Vérifier maintenant".
     */
    @FXML
    void handleVerifierMaintenant() {
        if (!faceAvailable) {
            showStatus("⚠  Aucun visage détecté. Repositionnez-vous face à la caméra.", "#F97316");
            return;
        }
        performComparison();
    }

    /**
     * Clic sur "Annuler" : ferme le popup sans valider.
     */
    @FXML
    void handleAnnuler() {
        verified = false;
        stopAllResources();
        if (popupStage != null) popupStage.close();
    }

    // =========================================================================
    //  LOGIQUE DE COMPARAISON
    // =========================================================================

    private void performComparison() {
        // Capturer le visage actuel
        Mat currentFace = faceService.captureCurrentFace();
        if (currentFace == null || currentFace.empty()) return;

        int attempt = autoAttempts.incrementAndGet();
        double progress = (double) attempt / MAX_AUTO_ATTEMPTS;

        // Comparer avec le visage stocké
        boolean match = faceService.compareFaces(storedFacePath, currentFace);
        currentFace.release();

        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            attemptsLabel.setText("Tentative " + attempt + " / " + MAX_AUTO_ATTEMPTS);

            if (match) {
                // ✅ SUCCÈS
                verified = true;
                progressBar.setProgress(1.0);
                progressBar.setStyle("-fx-accent: #10B981;");

                showOverlay("✅  Identité confirmée !", "rgba(16,185,129,0.9)");
                showStatus("✅  Vérification réussie ! Connexion en cours...", "#059669");
                btnVerifier.setDisable(true);

                // Fermer après 1.5 secondes
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                pause.setOnFinished(e -> {
                    stopAllResources();
                    if (popupStage != null) popupStage.close();
                });
                pause.play();

            } else if (attempt >= MAX_AUTO_ATTEMPTS) {
                // ❌ ÉCHEC après toutes les tentatives
                showOverlay("❌  Non reconnu", "rgba(239,68,68,0.9)");
                showStatus("❌  Vérification échouée. Réessayez la connexion.", "#EF4444");
                btnVerifier.setDisable(true);

                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.0));
                pause.setOnFinished(e -> {
                    stopAllResources();
                    if (popupStage != null) popupStage.close();
                });
                pause.play();
            } else {
                // ⏳ Tentative échouée, on réessaie
                showStatus("⏳  Tentative " + attempt + " échouée. Repositionnez-vous...", "#F97316");
            }
        });
    }

    // =========================================================================
    //  NETTOYAGE + RÉSULTAT
    // =========================================================================

    /**
     * Retourne true si la vérification faciale a réussi.
     * À appeler dans LoginController APRÈS showAndWait().
     */
    public boolean isVerified() {
        return verified;
    }

    private void stopAllResources() {
        if (autoCheckExecutor != null) {
            autoCheckExecutor.shutdownNow();
            autoCheckExecutor = null;
        }
        faceService.stopCamera();
    }

    // =========================================================================
    //  UTILITAIRES UI
    // =========================================================================

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private void showOverlay(String message, String bgColor) {
        overlayResult.setText(message);
        overlayResult.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-padding: 8 18; -fx-background-radius: 20; " +
                "-fx-background-color: " + bgColor + ";");
        overlayResult.setVisible(true);
    }
}
