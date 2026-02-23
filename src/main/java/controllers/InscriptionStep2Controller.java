package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.*;
import services.serviceUser;
import utils.ValidationUtils;

import java.io.File;
import java.io.IOException;

public class InscriptionStep2Controller {

    // Champs SANTÉ
    @FXML private ComboBox<Sexe>                  sexeCombo;
    @FXML private TextField                        poidsField;
    @FXML private TextField                        tailleField;
    @FXML private ComboBox<NiveauActivitePhysique> niveauActiviteCombo;

    // Champs SCOLAIRE
    @FXML private ComboBox<NiveauScolaire> niveauScolaireCombo;
    @FXML private TextField                etablissementField;

    // Image de profil
    @FXML private ImageView  profileImageView;
    @FXML private StackPane  avatarStackPane;
    @FXML private Label      avatarInitialLabel;
    @FXML private Label      imageStatusLabel;

    // Labels d'erreur
    @FXML private Label errorPoids, errorTaille, errorEtablissement;

    private UserRegistrationData userData;
    private String selectedImagePath = null;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        populateComboBoxes();
        hideAllErrors();
        setupRealtimeValidation();
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
        poidsField.focusedProperty().addListener((obs, oldVal, newVal)       -> { if (!newVal) validatePoids(); });
        tailleField.focusedProperty().addListener((obs, oldVal, newVal)      -> { if (!newVal) validateTaille(); });
        etablissementField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateEtablissement(); });
    }

    private boolean validatePoids() {
        String poidsStr = poidsField.getText().trim();
        if (poidsStr.isEmpty()) { showError(errorPoids, "Le poids est obligatoire"); return false; }
        try {
            double poids = Double.parseDouble(poidsStr);
            if (!ValidationUtils.isValidPoids(poids)) { showError(errorPoids, ValidationUtils.getPoidsErrorMessage(poids)); return false; }
            hideError(errorPoids); return true;
        } catch (NumberFormatException e) {
            showError(errorPoids, "Veuillez entrer un nombre valide (ex: 70.5)"); return false;
        }
    }

    private boolean validateTaille() {
        String tailleStr = tailleField.getText().trim();
        if (tailleStr.isEmpty()) { showError(errorTaille, "La taille est obligatoire"); return false; }
        try {
            int taille = Integer.parseInt(tailleStr);
            if (!ValidationUtils.isValidTaille(taille)) { showError(errorTaille, ValidationUtils.getTailleErrorMessage(taille)); return false; }
            hideError(errorTaille); return true;
        } catch (NumberFormatException e) {
            showError(errorTaille, "Veuillez entrer un nombre entier valide (ex: 175)"); return false;
        }
    }

    private boolean validateEtablissement() {
        String etablissement = etablissementField.getText();
        String errorMsg = ValidationUtils.getEtablissementErrorMessage(etablissement);
        if (!errorMsg.isEmpty()) { showError(errorEtablissement, errorMsg); return false; }
        hideError(errorEtablissement); return true;
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    private void hideError(Label errorLabel) { errorLabel.setVisible(false); }

    public void setUserData(UserRegistrationData data) {
        this.userData = data;
        if (data.getSexe() != null)           sexeCombo.setValue(data.getSexe());
        if (data.getPoids() != null)           poidsField.setText(data.getPoids().toString());
        if (data.getTaille() != null)          tailleField.setText(data.getTaille().toString());
        if (data.getNiveauActivite() != null)  niveauActiviteCombo.setValue(data.getNiveauActivite());
        if (data.getNiveauScolaire() != null)  niveauScolaireCombo.setValue(data.getNiveauScolaire());
        if (data.getEtablissement() != null)   etablissementField.setText(data.getEtablissement());
        if (data.getImagePath() != null) {
            selectedImagePath = data.getImagePath();
            loadImagePreview(new File(selectedImagePath));
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

    // ====================== SÉLECTION D'IMAGE ======================

    @FXML void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        Stage stage = (Stage) sexeCombo.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            loadImagePreview(selectedFile);
            imageStatusLabel.setText("✅ " + selectedFile.getName());
            imageStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
        }
    }

    @FXML void handleRemoveImage() {
        selectedImagePath = null;
        profileImageView.setImage(null);
        profileImageView.setVisible(false);
        avatarInitialLabel.setVisible(true);
        imageStatusLabel.setText("Aucune image sélectionnée");
        imageStatusLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11;");
    }

    private void loadImagePreview(File imageFile) {
        try {
            Image image = new Image(imageFile.toURI().toString(), 90, 90, true, true);
            profileImageView.setImage(image);
            profileImageView.setVisible(true);
            avatarInitialLabel.setVisible(false);
        } catch (Exception e) {
            System.err.println("Erreur chargement image: " + e.getMessage());
        }
    }

    // ====================== ✅ CONTINUER VERS ÉTAPE 3 ======================
    // (au lieu de créer le compte directement, on navigue vers la vérification biométrique)

    @FXML
    void handleCreateAccount() {
        boolean poidsValid         = validatePoids();
        boolean tailleValid        = validateTaille();
        boolean etablissementValid = validateEtablissement();

        if (!poidsValid || !tailleValid || !etablissementValid) {
            showAlert("Erreur de validation", "Veuillez corriger les erreurs avant de continuer.", Alert.AlertType.ERROR);
            return;
        }

        // Sauvegarder les données de l'étape 2 dans userData
        userData.setSexe(sexeCombo.getValue());
        try { userData.setPoids(Double.parseDouble(poidsField.getText().trim())); } catch (NumberFormatException ignored) {}
        try { userData.setTaille(Integer.parseInt(tailleField.getText().trim())); } catch (NumberFormatException ignored) {}
        userData.setNiveauActivite(niveauActiviteCombo.getValue());
        userData.setNiveauScolaire(niveauScolaireCombo.getValue());
        userData.setEtablissement(etablissementField.getText().trim());
        userData.setImagePath(selectedImagePath);

        // ✅ NOUVEAU : Naviguer vers Step 3 (capture du visage) au lieu de créer directement
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InscriptionStep3.fxml"));
            Parent root = loader.load();

            InscriptionStep3Controller step3Controller = loader.getController();
            step3Controller.setUserData(userData);

            Stage stage = (Stage) sexeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 3/3 — Vérification biométrique)");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'étape suivante.", Alert.AlertType.ERROR);
        }
    }

    // ====================== RETOUR VERS ÉTAPE 1 ======================

    @FXML
    void handleRetour() {
        userData.setSexe(sexeCombo.getValue());
        if (!poidsField.getText().trim().isEmpty()) {
            try { userData.setPoids(Double.parseDouble(poidsField.getText().trim())); } catch (NumberFormatException e) {}
        }
        if (!tailleField.getText().trim().isEmpty()) {
            try { userData.setTaille(Integer.parseInt(tailleField.getText().trim())); } catch (NumberFormatException e) {}
        }
        userData.setNiveauActivite(niveauActiviteCombo.getValue());
        userData.setNiveauScolaire(niveauScolaireCombo.getValue());
        userData.setEtablissement(etablissementField.getText().trim());
        userData.setImagePath(selectedImagePath);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionController step1Controller = loader.getController();
            step1Controller.setUserData(this.userData);
            Stage stage = (Stage) sexeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 1/3)");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir à l'étape précédente.", Alert.AlertType.ERROR);
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
