package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.*;
import org.bytedeco.opencv.opencv_core.Mat;
import services.serviceUser;
import utils.FaceRecognitionService;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller de l'étape 3 d'inscription : capture biométrique du visage.
 *
 * Flux :
 *  1. La caméra démarre automatiquement à l'ouverture de la page
 *  2. Quand un visage est détecté, le bouton "Capturer" s'active
 *  3. L'utilisateur clique "Capturer" → aperçu affiché
 *  4. L'utilisateur clique "Confirmer" → compte créé en BDD avec face_image_path
 *  5. Redirection vers Login
 */
public class InscriptionStep3Controller {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ImageView  cameraView;
    @FXML private ImageView  capturedFaceView;
    @FXML private VBox       cameraPlaceholder;
    @FXML private Label      statusLabel;
    @FXML private Label      faceDetectedOverlay;
    @FXML private VBox       previewContainer;
    @FXML private VBox       captureButtons;
    @FXML private HBox       confirmButtons;
    @FXML private Button     btnCapturer;
    @FXML private Button     btnConfirmer;

    // ── Services ─────────────────────────────────────────────────────────────
    private final FaceRecognitionService faceService = new FaceRecognitionService();
    private final serviceUser            userService = new serviceUser();

    // ── État ──────────────────────────────────────────────────────────────────
    private UserRegistrationData userData;
    private Mat                  capturedFaceMat = null;  // visage capturé (Mat)
    private boolean              faceCurrentlyDetected = false;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        // Démarrer la caméra dès l'affichage
        Platform.runLater(this::startCamera);
    }

    /**
     * Reçoit les données des étapes 1 et 2.
     */
    public void setUserData(UserRegistrationData data) {
        this.userData = data;
    }

    // =========================================================================
    //  CAMÉRA
    // =========================================================================

    private void startCamera() {
        try {
            faceService.startCamera((frame, faceFound) -> {
                // Afficher le stream
                if (frame != null) {
                    cameraView.setImage(frame);
                    cameraPlaceholder.setVisible(false);
                    cameraPlaceholder.setManaged(false);
                }

                // Mettre à jour le statut de détection
                faceCurrentlyDetected = faceFound;

                if (faceFound) {
                    statusLabel.setText("✅  Visage détecté — cliquez sur \"Capturer\"");
                    statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #059669;");
                    faceDetectedOverlay.setVisible(true);
                    btnCapturer.setDisable(false);
                } else {
                    statusLabel.setText("🔍  Aucun visage détecté — positionnez-vous face à la caméra");
                    statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #8B5CF6;");
                    faceDetectedOverlay.setVisible(false);
                    btnCapturer.setDisable(true);
                }
            });

            statusLabel.setText("🔍  Recherche de visage en cours...");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌  Impossible d'accéder à la caméra. Vérifiez qu'elle est branchée.");
            statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            // Permettre de créer le compte sans visage si pas de caméra
            btnCapturer.setDisable(false);
            btnCapturer.setText("⚠  Passer (sans capture)");
        }
    }

    // =========================================================================
    //  ACTIONS UTILISATEUR
    // =========================================================================

    /**
     * L'utilisateur clique "Capturer mon visage" :
     * on fige le dernier visage détecté et on affiche l'aperçu.
     */
    @FXML
    void handleCapturer() {
        if (faceCurrentlyDetected) {
            capturedFaceMat = faceService.captureCurrentFace();
        }

        if (capturedFaceMat != null && !capturedFaceMat.empty()) {
            // Afficher l'aperçu du visage capturé
            Image facePreview = matToPreviewImage(capturedFaceMat);
            if (facePreview != null) {
                capturedFaceView.setImage(facePreview);
            }

            showConfirmationState();
        } else {
            // Mode "passer sans capture" (pas de caméra disponible)
            capturedFaceMat = null;
            showConfirmationState();
            statusLabel.setText("⚠  Compte créé sans biométrie faciale.");
            statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #F97316;");
        }
    }

    /**
     * L'utilisateur clique "Reprendre" : on retourne à la vue caméra.
     */
    @FXML
    void handleReprendre() {
        capturedFaceMat = null;
        showCaptureState();
        statusLabel.setText("🔍  Positionnez-vous à nouveau face à la caméra...");
        statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #8B5CF6;");
    }

    /**
     * L'utilisateur clique "Créer mon compte" :
     * - Arrête la caméra
     * - Sauvegarde le visage
     * - Crée l'utilisateur en BDD
     * - Redirige vers Login
     */
    @FXML
    void handleConfirmer() {
        btnConfirmer.setDisable(true);
        btnConfirmer.setText("Création en cours...");

        // Arrêter la caméra proprement
        faceService.stopCamera();

        // Sauvegarder le visage si disponible
        String facePath = null;
        if (capturedFaceMat != null && !capturedFaceMat.empty()) {
            facePath = faceService.saveFace(capturedFaceMat, userData.getEmail());
        }

        // Construire l'objet user
        user newUser = new user(
                userData.getNom(),
                userData.getPrenom(),
                userData.getEmail(),
                userData.getPassword(),
                userData.getDateNaissance(),
                LocalDate.now().toString(),
                Role.ETUDIANT,
                userData.getSexe(),
                userData.getPoids(),
                userData.getTaille(),
                userData.getNiveauActivite(),
                userData.getNiveauScolaire(),
                userData.getEtablissement()
        );

        try {
            // ✅ Enregistrement avec photo de profil ET visage biométrique
            userService.add(newUser, userData.getImagePath(), facePath);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Compte créé !");
            alert.setHeaderText(null);
            if (facePath != null) {
                alert.setContentText("✅ Votre compte a été créé avec succès et votre visage a été enregistré.\n" +
                        "Vous devrez scanner votre visage à chaque connexion.");
            } else {
                alert.setContentText("✅ Votre compte a été créé sans biométrie faciale.\n" +
                        "Vous pouvez vous connecter normalement.");
            }
            alert.showAndWait();
            goToLogin();

        } catch (Exception e) {
            e.printStackTrace();
            btnConfirmer.setDisable(false);
            btnConfirmer.setText("✓  Créer mon compte");
            showAlert("Erreur", "Une erreur est survenue lors de la création du compte.\nVeuillez réessayer.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Retour vers l'étape 2.
     */
    @FXML
    void handleRetour() {
        faceService.stopCamera();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InscriptionStep2.fxml"));
            Parent root = loader.load();
            InscriptionStep2Controller step2Controller = loader.getController();
            step2Controller.setUserData(userData);
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 2/3)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  GESTION DES ÉTATS UI
    // =========================================================================

    private void showConfirmationState() {
        captureButtons.setVisible(false);
        captureButtons.setManaged(false);
        confirmButtons.setVisible(true);
        confirmButtons.setManaged(true);
        previewContainer.setVisible(capturedFaceMat != null);
        previewContainer.setManaged(capturedFaceMat != null);
        if (capturedFaceMat != null) {
            statusLabel.setText("✅  Visage capturé — confirmez pour créer votre compte.");
            statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #059669;");
        }
    }

    private void showCaptureState() {
        confirmButtons.setVisible(false);
        confirmButtons.setManaged(false);
        captureButtons.setVisible(true);
        captureButtons.setManaged(true);
        previewContainer.setVisible(false);
        previewContainer.setManaged(false);
    }

    // =========================================================================
    //  UTILITAIRES
    // =========================================================================

    /**
     * Convertit un Mat (gris) en Image JavaFX pour l'aperçu.
     * ✅ Méthode fiable : passage par fichier temporaire pour éviter
     *    les incompatibilités d'API imencode entre versions JavaCV.
     */
    private Image matToPreviewImage(Mat faceMat) {
        try {
            // Convertir en BGR pour l'écriture PNG (imwrite attend du BGR)
            org.bytedeco.opencv.opencv_core.Mat bgrMat = new org.bytedeco.opencv.opencv_core.Mat();
            org.bytedeco.opencv.global.opencv_imgproc.cvtColor(
                    faceMat, bgrMat,
                    org.bytedeco.opencv.global.opencv_imgproc.COLOR_GRAY2BGR);

            // Écrire dans un fichier PNG temporaire
            java.io.File tempFile = java.io.File.createTempFile("face_preview_", ".png");
            tempFile.deleteOnExit();
            org.bytedeco.opencv.global.opencv_imgcodecs.imwrite(
                    tempFile.getAbsolutePath(), bgrMat);
            bgrMat.release();

            // Charger comme Image JavaFX depuis l'URI du fichier
            return new Image(tempFile.toURI().toString());

        } catch (Exception e) {
            System.err.println("Erreur conversion aperçu : " + e.getMessage());
            return null;
        }
    }

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
