package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.*;
import services.serviceUser;
import utils.ValidationUtils;

import java.io.IOException;
import java.time.LocalDate;

public class InscriptionStep2Controller {

    // Champs SANTÉ
    @FXML private ComboBox<Sexe> sexeCombo;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private ComboBox<NiveauActivitePhysique> niveauActiviteCombo;

    // Champs SCOLAIRE
    @FXML private ComboBox<NiveauScolaire> niveauScolaireCombo;
    @FXML private TextField etablissementField;

    // Labels d'erreur
    @FXML private Label errorPoids, errorTaille, errorEtablissement;

    // Données de l'étape 1
    private UserRegistrationData userData;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        // Remplir les ComboBox
        populateComboBoxes();

        // Masquer les erreurs
        hideAllErrors();

        // Setup validation en temps réel
        setupRealtimeValidation();
    }

    private void populateComboBoxes() {
        // ComboBox Sexe
        sexeCombo.setItems(FXCollections.observableArrayList(Sexe.values()));
        sexeCombo.setPromptText("Sélectionnez votre sexe");

        // ComboBox Niveau Activité Physique
        niveauActiviteCombo.setItems(FXCollections.observableArrayList(NiveauActivitePhysique.values()));
        niveauActiviteCombo.setPromptText("Sélectionnez votre niveau");

        // ComboBox Niveau Scolaire
        niveauScolaireCombo.setItems(FXCollections.observableArrayList(NiveauScolaire.values()));
        niveauScolaireCombo.setPromptText("Sélectionnez votre niveau");
    }

    private void hideAllErrors() {
        errorPoids.setVisible(false);
        errorTaille.setVisible(false);
        errorEtablissement.setVisible(false);
    }

    private void setupRealtimeValidation() {
        // Validation poids
        poidsField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validatePoids();
        });

        // Validation taille
        tailleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateTaille();
        });

        // Validation établissement
        etablissementField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateEtablissement();
        });
    }

    private boolean validatePoids() {
        String poidsStr = poidsField.getText().trim();
        if (poidsStr.isEmpty()) {
            showError(errorPoids, "Le poids est obligatoire");
            return false;
        }
        try {
            double poids = Double.parseDouble(poidsStr);
            if (!ValidationUtils.isValidPoids(poids)) {
                showError(errorPoids, ValidationUtils.getPoidsErrorMessage(poids));
                return false;
            }
            hideError(errorPoids);
            return true;
        } catch (NumberFormatException e) {
            showError(errorPoids, "Veuillez entrer un nombre valide (ex: 70.5)");
            return false;
        }
    }

    private boolean validateTaille() {
        String tailleStr = tailleField.getText().trim();
        if (tailleStr.isEmpty()) {
            showError(errorTaille, "La taille est obligatoire");
            return false;
        }
        try {
            int taille = Integer.parseInt(tailleStr);
            if (!ValidationUtils.isValidTaille(taille)) {
                showError(errorTaille, ValidationUtils.getTailleErrorMessage(taille));
                return false;
            }
            hideError(errorTaille);
            return true;
        } catch (NumberFormatException e) {
            showError(errorTaille, "Veuillez entrer un nombre entier valide (ex: 175)");
            return false;
        }
    }

    private boolean validateEtablissement() {
        String etablissement = etablissementField.getText();
        String errorMsg = ValidationUtils.getEtablissementErrorMessage(etablissement);
        if (!errorMsg.isEmpty()) {
            showError(errorEtablissement, errorMsg);
            return false;
        }
        hideError(errorEtablissement);
        return true;
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
    }

    public void setUserData(UserRegistrationData data) {
        this.userData = data;
        // Pré-remplissage des champs de l'étape 2 si disponibles
        if (data.getSexe() != null) {
            sexeCombo.setValue(data.getSexe());
        }
        if (data.getPoids() != null) {
            poidsField.setText(data.getPoids().toString());
        }
        if (data.getTaille() != null) {
            tailleField.setText(data.getTaille().toString());
        }
        if (data.getNiveauActivite() != null) {
            niveauActiviteCombo.setValue(data.getNiveauActivite());
        }
        if (data.getNiveauScolaire() != null) {
            niveauScolaireCombo.setValue(data.getNiveauScolaire());
        }
        if (data.getEtablissement() != null) {
            etablissementField.setText(data.getEtablissement());
        }
    }

    @FXML
    void handleCreateAccount() {
        // Valider les champs
        boolean poidsValid = validatePoids();
        boolean tailleValid = validateTaille();
        boolean etablissementValid = validateEtablissement();

        if (!poidsValid || !tailleValid || !etablissementValid) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs avant de continuer.",
                    Alert.AlertType.ERROR);
            return;
        }

        // Récupérer les valeurs (null si non remplis, mais comme validés, ils le sont)
        Sexe sexe = sexeCombo.getValue();
        Double poids = Double.parseDouble(poidsField.getText().trim());
        Integer taille = Integer.parseInt(tailleField.getText().trim());
        NiveauActivitePhysique niveauActivite = niveauActiviteCombo.getValue();
        NiveauScolaire niveauScolaire = niveauScolaireCombo.getValue();
        String etablissement = etablissementField.getText().trim();

        // Créer l'utilisateur complet
        user newUser = new user(
                userData.getNom(),
                userData.getPrenom(),
                userData.getEmail(),
                userData.getPassword(),
                userData.getDateNaissance(),
                LocalDate.now().toString(),
                Role.ETUDIANT,
                // Données étape 2
                sexe,
                poids,
                taille,
                niveauActivite,
                niveauScolaire,
                etablissement
        );

        // Sauvegarder en base de données
        try {
            service.add(newUser);

            showAlert("Succès",
                    "Votre compte a été créé avec succès !\nVous pouvez maintenant vous connecter.",
                    Alert.AlertType.INFORMATION);

            goToLogin();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "Une erreur est survenue lors de la création du compte.\nVeuillez réessayer.",
                    Alert.AlertType.ERROR);
        }
    }

    // ====================== RETOUR VERS ÉTAPE 1 ======================

    @FXML
    void handleRetour() {
        // Sauvegardez les données partielles de l'étape 2
        userData.setSexe(sexeCombo.getValue());
        if (!poidsField.getText().trim().isEmpty()) {
            try {
                userData.setPoids(Double.parseDouble(poidsField.getText().trim()));
            } catch (NumberFormatException e) {
                // Ignorer si invalide
            }
        }
        if (!tailleField.getText().trim().isEmpty()) {
            try {
                userData.setTaille(Integer.parseInt(tailleField.getText().trim()));
            } catch (NumberFormatException e) {
                // Ignorer si invalide
            }
        }
        userData.setNiveauActivite(niveauActiviteCombo.getValue());
        userData.setNiveauScolaire(niveauScolaireCombo.getValue());
        userData.setEtablissement(etablissementField.getText().trim());

        try {
            // Chargez le loader pour obtenir le controller
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Inscription.fxml"));
            Parent root = loader.load();

            // Obtenez le controller de l'étape 1 et passez les données
            InscriptionController step1Controller = loader.getController();
            step1Controller.setUserData(this.userData);  // Passe les données de l'étape 1 en arrière

            // Changez la scène
            Stage stage = (Stage) sexeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 1/2)");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir à l'étape précédente.", Alert.AlertType.ERROR);
        }
    }

    // ====================== NAVIGATION ======================

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) sexeCombo.getScene().getWindow();
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