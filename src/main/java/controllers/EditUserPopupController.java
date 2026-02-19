package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.*;
import services.serviceUser;
import utils.ValidationUtils;

import java.time.LocalDate;

public class EditUserPopupController {

    @FXML private TextField nomField, prenomField, emailField, poidsField, tailleField, etablissementField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private Label roleLabel;
    @FXML private Label dateInscriptionLabel;
    @FXML private ComboBox<Sexe> sexeCombo;
    @FXML private ComboBox<NiveauActivitePhysique> niveauActiviteCombo;
    @FXML private ComboBox<NiveauScolaire> niveauScolaireCombo;

    @FXML private Label errorNom, errorPrenom, errorEmail, errorDate, errorPoids, errorTaille, errorEtablissement;

    private user currentUser;
    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        hideAllErrors();
        populateComboBoxes();
        setupRealtimeValidation();
    }

    private void populateComboBoxes() {
        sexeCombo.setItems(FXCollections.observableArrayList(Sexe.values()));
        niveauActiviteCombo.setItems(FXCollections.observableArrayList(NiveauActivitePhysique.values()));
        niveauScolaireCombo.setItems(FXCollections.observableArrayList(NiveauScolaire.values()));
    }

    private void hideAllErrors() {
        errorNom.setVisible(false);
        errorPrenom.setVisible(false);
        errorEmail.setVisible(false);
        errorDate.setVisible(false);
        errorPoids.setVisible(false);
        errorTaille.setVisible(false);
        errorEtablissement.setVisible(false);
    }

    private void setupRealtimeValidation() {
        nomField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateNom(); });
        prenomField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validatePrenom(); });
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateEmail(); });
        dateNaissanceField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateDate(); });
        poidsField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validatePoids(); });
        tailleField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateTaille(); });
        etablissementField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateEtablissement(); });
    }

    public void setUser(user u) {
        this.currentUser = u;
        nomField.setText(u.getUser_nom());
        prenomField.setText(u.getUser_prenom());
        emailField.setText(u.getUser_email());
        dateNaissanceField.setValue(LocalDate.parse(u.getUser_date_de_naissance()));

        // Champs secondaires
        if (u.getUser_sexe() != null) sexeCombo.setValue(u.getUser_sexe());
        if (u.getUser_poids() != null) poidsField.setText(u.getUser_poids().toString());
        if (u.getUser_taille() != null) tailleField.setText(u.getUser_taille().toString());
        if (u.getUser_niveau_activite_physique() != null) niveauActiviteCombo.setValue(u.getUser_niveau_activite_physique());
        if (u.getUser_niveau_scolaire() != null) niveauScolaireCombo.setValue(u.getUser_niveau_scolaire());
        etablissementField.setText(u.getUser_etablissement_scolaire());

        // Labels stylisés en badges via le FXML
        roleLabel.setText("⭐ Rôle : " + u.getType_utilisateur().name());
        dateInscriptionLabel.setText("🕒 Inscrit le : " + u.getDate_inscription());
    }

    private boolean validateNom() {
        String errorMsg = ValidationUtils.getNameErrorMessage(nomField.getText());
        if (!errorMsg.isEmpty()) {
            showError(errorNom, errorMsg);
            return false;
        }
        hideError(errorNom);
        return true;
    }

    private boolean validatePrenom() {
        String errorMsg = ValidationUtils.getNameErrorMessage(prenomField.getText());
        if (!errorMsg.isEmpty()) {
            showError(errorPrenom, errorMsg);
            return false;
        }
        hideError(errorPrenom);
        return true;
    }

    private boolean validateEmail() {
        String errorMsg = ValidationUtils.getEmailErrorMessage(emailField.getText());
        if (!errorMsg.isEmpty()) {
            showError(errorEmail, errorMsg);
            return false;
        }
        hideError(errorEmail);
        return true;
    }

    private boolean validateDate() {
        if (dateNaissanceField.getValue() == null) {
            showError(errorDate, "La date est obligatoire");
            return false;
        }
        if (!ValidationUtils.isValidBirthDate(dateNaissanceField.getValue())) {
            showError(errorDate, ValidationUtils.getBirthDateErrorMessage(dateNaissanceField.getValue()));
            return false;
        }
        hideError(errorDate);
        return true;
    }

    private boolean validatePoids() {
        String poidsStr = poidsField.getText().trim();
        if (poidsStr.isEmpty()) return true; // Optionnel dans edit
        try {
            double poids = Double.parseDouble(poidsStr);
            if (!ValidationUtils.isValidPoids(poids)) {
                showError(errorPoids, ValidationUtils.getPoidsErrorMessage(poids));
                return false;
            }
            hideError(errorPoids);
            return true;
        } catch (NumberFormatException e) {
            showError(errorPoids, "Nombre valide requis");
            return false;
        }
    }

    private boolean validateTaille() {
        String tailleStr = tailleField.getText().trim();
        if (tailleStr.isEmpty()) return true; // Optionnel
        try {
            int taille = Integer.parseInt(tailleStr);
            if (!ValidationUtils.isValidTaille(taille)) {
                showError(errorTaille, ValidationUtils.getTailleErrorMessage(taille));
                return false;
            }
            hideError(errorTaille);
            return true;
        } catch (NumberFormatException e) {
            showError(errorTaille, "Nombre entier valide requis");
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

    @FXML
    void saveChanges() {
        boolean nomValid = validateNom();
        boolean prenomValid = validatePrenom();
        boolean emailValid = validateEmail();
        boolean dateValid = validateDate();
        boolean poidsValid = validatePoids();
        boolean tailleValid = validateTaille();
        boolean etablissementValid = validateEtablissement();

        if (!nomValid || !prenomValid || !emailValid || !dateValid || !poidsValid || !tailleValid || !etablissementValid) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez corriger les erreurs avant de sauvegarder.");
            alert.showAndWait();
            return;
        }

        Sexe sexe = sexeCombo.getValue();
        Double poids = poidsField.getText().trim().isEmpty() ? null : Double.parseDouble(poidsField.getText().trim());
        Integer taille = tailleField.getText().trim().isEmpty() ? null : Integer.parseInt(tailleField.getText().trim());
        NiveauActivitePhysique niveauActivite = niveauActiviteCombo.getValue();
        NiveauScolaire niveauScolaire = niveauScolaireCombo.getValue();
        String etablissement = etablissementField.getText().trim();

        service.updateById(
                currentUser.getUser_id(),
                nomField.getText().trim(),
                prenomField.getText().trim(),
                emailField.getText().trim(),
                currentUser.getUser_password(),
                dateNaissanceField.getValue().toString(),
                currentUser.getDate_inscription(),
                currentUser.getType_utilisateur(),
                sexe,
                poids,
                taille,
                niveauActivite,
                niveauScolaire,
                etablissement
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Modifications enregistrées avec succès !");
        alert.showAndWait();

        closeWindow();
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}