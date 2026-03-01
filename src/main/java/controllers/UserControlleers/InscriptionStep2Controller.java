package controllers.UserControlleers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.UserModels.NiveauActivitePhysique;
import models.UserModels.NiveauScolaire;
import models.UserModels.Sexe;
import models.UserModels.UserRegistrationData;
import services.UserServices.serviceUser;
import utils.ImageModerationService;
import utils.ValidationUtils;

import java.io.File;
import java.io.IOException;

/**
 * Controller — Étape 2/3 d'inscription.
 *
 * ── Flux corrigé ─────────────────────────────────────────────────────────
 *
 *   Step1 (Inscription.fxml)
 *      └──► Step2 (InscriptionStep2.fxml)   ← ici
 *              │  Validation formulaire + vérification image (Sightengine API)
 *              │  ✅ "Continuer →" sauvegarde les données dans UserRegistrationData
 *              │       et navigue vers Step3 — NE crée PAS le compte
 *              └──► Step3 (InscriptionStep3.fxml)
 *                       └── Capture biométrique visage
 *                               └── Crée le compte en BDD → Login
 *
 * ── Vérification image (Sightengine) ────────────────────────────────────
 *  1. Utilisateur choisit une image → aperçu immédiat
 *  2. Spinner "Vérification…" + bouton "Continuer" désactivé
 *  3. Appel API en thread de fond (Task JavaFX — interface non gelée)
 *  4a. ✅ Approuvée → selectedImagePath enregistré, bouton réactivé
 *  4b. ❌ Refusée   → selectedImagePath = null, aperçu retiré, message rouge
 *  4c. ⚠ Réseau    → selectedImagePath = null, message orange
 *
 *  Si aucune image ou image refusée : selectedImagePath reste null ;
 *  le compte sera créé sans photo (comportement identique à l'original).
 */
public class InscriptionStep2Controller {

    /* ── Champs SANTÉ ──────────────────────────────────────────────────── */
    @FXML private ComboBox<Sexe>                   sexeCombo;
    @FXML private TextField                        poidsField;
    @FXML private TextField                        tailleField;
    @FXML private ComboBox<NiveauActivitePhysique> niveauActiviteCombo;

    /* ── Champs SCOLAIRE ───────────────────────────────────────────────── */
    @FXML private ComboBox<NiveauScolaire>         niveauScolaireCombo;
    @FXML private TextField                        etablissementField;

    /* ── Image de profil ───────────────────────────────────────────────── */
    @FXML private ImageView profileImageView;
    @FXML private StackPane avatarStackPane;
    @FXML private Label     avatarInitialLabel;
    @FXML private Label     imageStatusLabel;

    /* ── Zone de vérification d'image (FXML) ───────────────────────────── */
    @FXML private HBox              verificationRow;
    @FXML private ProgressIndicator verificationSpinner;
    @FXML private Label             verificationLabel;

    /* ── Labels d'erreur formulaire ────────────────────────────────────── */
    @FXML private Label errorPoids;
    @FXML private Label errorTaille;
    @FXML private Label errorEtablissement;

    /* ── Bouton principal ───────────────────────────────────────────────── */
    @FXML private Button btnCreateAccount;   // libellé "Continuer →" dans le FXML

    /* ── État interne ───────────────────────────────────────────────────── */
    private UserRegistrationData userData;
    private String                selectedImagePath      = null;
    private boolean               imageApproved          = false;
    private boolean               verificationInProgress = false;

    private final serviceUser            service   = new serviceUser();
    private final ImageModerationService moderator = new ImageModerationService();

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        populateComboBoxes();
        hideAllErrors();
        setupRealtimeValidation();
        hideVerificationRow();
    }

    private void populateComboBoxes() {
        sexeCombo.setItems(FXCollections.observableArrayList(Sexe.values()));
        sexeCombo.setPromptText("Sélectionnez votre sexe");
        niveauActiviteCombo.setItems(FXCollections.observableArrayList(NiveauActivitePhysique.values()));
        niveauActiviteCombo.setPromptText("Sélectionnez votre niveau");
        niveauScolaireCombo.setItems(FXCollections.observableArrayList(NiveauScolaire.values()));
        niveauScolaireCombo.setPromptText("Sélectionnez votre niveau");
    }

    private void hideAllErrors() {
        errorPoids.setVisible(false);
        errorTaille.setVisible(false);
        errorEtablissement.setVisible(false);
    }

    private void setupRealtimeValidation() {
        poidsField.focusedProperty().addListener((o, ov, nv)       -> { if (!nv) validatePoids(); });
        tailleField.focusedProperty().addListener((o, ov, nv)      -> { if (!nv) validateTaille(); });
        etablissementField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validateEtablissement(); });
    }

    // =========================================================================
    //  DONNÉES ÉTAPE 1 → ÉTAPE 2  (appelé par InscriptionController)
    // =========================================================================

    public void setUserData(UserRegistrationData data) {
        this.userData = data;
        if (data.getSexe()            != null) sexeCombo.setValue(data.getSexe());
        if (data.getPoids()           != null) poidsField.setText(data.getPoids().toString());
        if (data.getTaille()          != null) tailleField.setText(data.getTaille().toString());
        if (data.getNiveauActivite()  != null) niveauActiviteCombo.setValue(data.getNiveauActivite());
        if (data.getNiveauScolaire()  != null) niveauScolaireCombo.setValue(data.getNiveauScolaire());
        if (data.getEtablissement()   != null) etablissementField.setText(data.getEtablissement());

        // Restaurer l'image si déjà approuvée lors d'un retour depuis Step3
        if (data.getImagePath() != null) {
            selectedImagePath = data.getImagePath();
            imageApproved     = true;
            File f = new File(selectedImagePath);
            loadImagePreview(f);
            imageStatusLabel.setText("✅ " + f.getName());
            imageStatusLabel.setStyle(
                    "-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
            showVerificationRow();
            setStateApproved();
        }

        updateAvatarInitial();
    }

    private void updateAvatarInitial() {
        if (userData != null && userData.getPrenom() != null && !userData.getPrenom().isEmpty()) {
            avatarInitialLabel.setText(userData.getPrenom().substring(0, 1).toUpperCase());
        } else {
            avatarInitialLabel.setText("U");
        }
    }

    // =========================================================================
    //  ZONE DE VÉRIFICATION — états visuels
    // =========================================================================

    private void hideVerificationRow() {
        verificationRow.setVisible(false);
        verificationRow.setManaged(false);
    }

    private void showVerificationRow() {
        verificationRow.setVisible(true);
        verificationRow.setManaged(true);
    }

    /** 🔄 En cours : spinner + bouton désactivé. */
    private void setStateVerifying() {
        showVerificationRow();
        verificationSpinner.setVisible(true);
        verificationSpinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        verificationLabel.setText("Vérification de l'image en cours…");
        verificationLabel.setStyle(
                "-fx-text-fill: #3B82F6; -fx-font-size: 11; -fx-font-weight: bold;");
        disableCreateButton();
    }

    /** ✅ Image approuvée. */
    private void setStateApproved() {
        verificationSpinner.setVisible(false);
        verificationLabel.setText("✅ Image approuvée — contenu approprié");
        verificationLabel.setStyle(
                "-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
        enableCreateButton();
    }

    /** ❌ Image refusée avec raison. */
    private void setStateRejected(String reason) {
        verificationSpinner.setVisible(false);
        verificationLabel.setText("❌ Image refusée : " + reason);
        verificationLabel.setStyle(
                "-fx-text-fill: #EF4444; -fx-font-size: 11; -fx-font-weight: bold;");
        verificationLabel.setWrapText(true);
        enableCreateButton();
    }

    /** ⚠ Erreur réseau. */
    private void setStateNetworkError(String msg) {
        verificationSpinner.setVisible(false);
        verificationLabel.setText("⚠ " + msg);
        verificationLabel.setStyle(
                "-fx-text-fill: #F59E0B; -fx-font-size: 11; -fx-font-weight: bold;");
        enableCreateButton();
    }

    private void disableCreateButton() {
        if (btnCreateAccount == null) return;
        btnCreateAccount.setDisable(true);
        btnCreateAccount.setStyle(
                "-fx-background-color: #9CA3AF; -fx-text-fill: white;" +
                " -fx-font-size: 15; -fx-font-weight: bold;" +
                " -fx-background-radius: 12; -fx-cursor: default;");
    }

    private void enableCreateButton() {
        if (btnCreateAccount == null) return;
        btnCreateAccount.setDisable(false);
        btnCreateAccount.setStyle(
                "-fx-background-color: linear-gradient(to right, #8B5CF6, #7C3AED);" +
                " -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;" +
                " -fx-background-radius: 12; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(three-pass-box, rgba(139,92,246,0.4), 15, 0, 0, 5);");
    }

    // =========================================================================
    //  SÉLECTION D'IMAGE
    // =========================================================================

    @FXML
    void handleChooseImage() {
        if (verificationInProgress) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        Stage stage = (Stage) sexeCombo.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        // Aperçu immédiat
        loadImagePreview(file);
        imageStatusLabel.setText("📎 " + file.getName());
        imageStatusLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11;");

        // Réinitialiser l'état
        selectedImagePath = null;
        imageApproved     = false;

        // Lancer la vérification asynchrone
        runModerationAsync(file);
    }

    @FXML
    void handleRemoveImage() {
        if (verificationInProgress) return;

        selectedImagePath = null;
        imageApproved     = false;
        profileImageView.setImage(null);
        profileImageView.setVisible(false);
        avatarInitialLabel.setVisible(true);
        imageStatusLabel.setText("Aucune image sélectionnée");
        imageStatusLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11;");
        hideVerificationRow();
        enableCreateButton();
    }

    private void loadImagePreview(File f) {
        try {
            profileImageView.setImage(
                    new Image(f.toURI().toString(), 90, 90, true, true));
            profileImageView.setVisible(true);
            avatarInitialLabel.setVisible(false);
        } catch (Exception e) {
            System.err.println("Aperçu image : " + e.getMessage());
        }
    }

    // =========================================================================
    //  VÉRIFICATION ASYNCHRONE (Sightengine API)
    // =========================================================================

    private void runModerationAsync(File imageFile) {
        verificationInProgress = true;
        setStateVerifying();

        Task<ImageModerationService.ModerationResult> task = new Task<>() {
            @Override
            protected ImageModerationService.ModerationResult call() throws Exception {
                return moderator.checkImage(imageFile);
            }
        };

        task.setOnSucceeded(ev -> {
            verificationInProgress = false;
            ImageModerationService.ModerationResult result = task.getValue();
            Platform.runLater(() -> {
                if (result.isApproved()) {
                    selectedImagePath = imageFile.getAbsolutePath();
                    imageApproved     = true;
                    imageStatusLabel.setText("✅ " + imageFile.getName());
                    imageStatusLabel.setStyle(
                            "-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
                    setStateApproved();
                } else {
                    selectedImagePath = null;
                    imageApproved     = false;
                    profileImageView.setImage(null);
                    profileImageView.setVisible(false);
                    avatarInitialLabel.setVisible(true);
                    imageStatusLabel.setText("❌ Image refusée");
                    imageStatusLabel.setStyle(
                            "-fx-text-fill: #EF4444; -fx-font-size: 11; -fx-font-weight: bold;");
                    setStateRejected(result.getReason());
                }
            });
        });

        task.setOnFailed(ev -> {
            verificationInProgress = false;
            Throwable ex = task.getException();
            System.err.println("[Moderation] Erreur : " + ex.getMessage());
            Platform.runLater(() -> {
                selectedImagePath = null;
                imageApproved     = false;
                profileImageView.setImage(null);
                profileImageView.setVisible(false);
                avatarInitialLabel.setVisible(true);
                imageStatusLabel.setText("⚠ Vérification impossible");
                imageStatusLabel.setStyle(
                        "-fx-text-fill: #F59E0B; -fx-font-size: 11; -fx-font-weight: bold;");

                String msg;
                if (ex instanceof java.net.ConnectException
                        || ex instanceof java.net.UnknownHostException) {
                    msg = "Connexion internet indisponible. Image non acceptée.";
                } else if (ex instanceof java.net.http.HttpTimeoutException) {
                    msg = "Délai dépassé. Veuillez réessayer.";
                } else {
                    msg = "Service indisponible. Image non acceptée.";
                }
                setStateNetworkError(msg);
            });
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    //  VALIDATION FORMULAIRE
    // =========================================================================

    private boolean validatePoids() {
        String v = poidsField.getText().trim();
        if (v.isEmpty()) { showError(errorPoids, "Le poids est obligatoire"); return false; }
        try {
            double d = Double.parseDouble(v);
            if (!ValidationUtils.isValidPoids(d)) {
                showError(errorPoids, ValidationUtils.getPoidsErrorMessage(d)); return false;
            }
            hideError(errorPoids); return true;
        } catch (NumberFormatException e) {
            showError(errorPoids, "Veuillez entrer un nombre valide (ex: 70.5)"); return false;
        }
    }

    private boolean validateTaille() {
        String v = tailleField.getText().trim();
        if (v.isEmpty()) { showError(errorTaille, "La taille est obligatoire"); return false; }
        try {
            int i = Integer.parseInt(v);
            if (!ValidationUtils.isValidTaille(i)) {
                showError(errorTaille, ValidationUtils.getTailleErrorMessage(i)); return false;
            }
            hideError(errorTaille); return true;
        } catch (NumberFormatException e) {
            showError(errorTaille, "Veuillez entrer un nombre entier valide (ex: 175)"); return false;
        }
    }

    private boolean validateEtablissement() {
        String msg = ValidationUtils.getEtablissementErrorMessage(etablissementField.getText());
        if (!msg.isEmpty()) { showError(errorEtablissement, msg); return false; }
        hideError(errorEtablissement); return true;
    }

    private void showError(Label lbl, String msg) {
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
    }

    private void hideError(Label lbl) {
        lbl.setVisible(false);
    }

    // =========================================================================
    //  ✅ CONTINUER VERS ÉTAPE 3  (anciennement "Créer le compte")
    //
    //  ⚠ CORRECTION PRINCIPALE :
    //     Cette méthode ne crée PLUS le compte.
    //     Elle sauvegarde toutes les données dans UserRegistrationData
    //     et navigue vers InscriptionStep3.fxml (capture biométrique).
    //     C'est Step3 (handleConfirmer) qui appelle service.add() et crée le compte.
    // =========================================================================

    @FXML
    void handleCreateAccount() {
        // Garde-fou : attendre la fin de la vérification image si en cours
        if (verificationInProgress) {
            showAlert("Vérification en cours",
                    "Veuillez patienter pendant la vérification de l'image.",
                    Alert.AlertType.WARNING);
            return;
        }

        // Validation du formulaire
        boolean poidsOk        = validatePoids();
        boolean tailleOk       = validateTaille();
        boolean etablissementOk = validateEtablissement();

        if (!poidsOk || !tailleOk || !etablissementOk) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs avant de continuer.",
                    Alert.AlertType.ERROR);
            return;
        }

        // ── Sauvegarder toutes les données de l'étape 2 dans userData ────────
        userData.setSexe(sexeCombo.getValue());
        try { userData.setPoids(Double.parseDouble(poidsField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        try { userData.setTaille(Integer.parseInt(tailleField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        userData.setNiveauActivite(niveauActiviteCombo.getValue());
        userData.setNiveauScolaire(niveauScolaireCombo.getValue());
        userData.setEtablissement(etablissementField.getText().trim());
        userData.setImagePath(selectedImagePath);   // null si aucune image ou image refusée

        // ── Naviguer vers l'étape 3 (capture biométrique) ───────────────────
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/InscriptionStep3.fxml"));
            Parent root = loader.load();

            InscriptionStep3Controller step3Controller = loader.getController();
            step3Controller.setUserData(userData);   // transmettre toutes les données

            Stage stage = (Stage) sexeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 3/3)");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de passer à l'étape suivante.\nVeuillez réessayer.",
                    Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    //  RETOUR VERS ÉTAPE 1
    // =========================================================================

    @FXML
    void handleRetour() {
        if (verificationInProgress) {
            showAlert("Vérification en cours",
                    "Veuillez patienter pendant la vérification de l'image.",
                    Alert.AlertType.WARNING);
            return;
        }

        // Sauvegarder l'état partiel pour restauration
        userData.setSexe(sexeCombo.getValue());
        try { if (!poidsField.getText().trim().isEmpty())
            userData.setPoids(Double.parseDouble(poidsField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        try { if (!tailleField.getText().trim().isEmpty())
            userData.setTaille(Integer.parseInt(tailleField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        userData.setNiveauActivite(niveauActiviteCombo.getValue());
        userData.setNiveauScolaire(niveauScolaireCombo.getValue());
        userData.setEtablissement(etablissementField.getText().trim());
        userData.setImagePath(selectedImagePath);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionController step1Controller = loader.getController();
            step1Controller.setUserData(this.userData);
            Stage stage = (Stage) sexeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 1/3)");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de revenir à l'étape précédente.",
                    Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    //  UTILITAIRES
    // =========================================================================

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
