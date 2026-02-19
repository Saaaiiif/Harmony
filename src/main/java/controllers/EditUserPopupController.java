package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.user;
import services.serviceUser;
import utils.ValidationUtils;

import java.time.LocalDate;

public class EditUserPopupController {

    @FXML private TextField nomField, prenomField, emailField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private Label roleLabel;
    @FXML private Label dateInscriptionLabel;


    @FXML private Label errorNom, errorPrenom, errorEmail, errorDate;

    private user currentUser;
    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {

        hideAllErrors();


        setupRealtimeValidation(); //validation en temsp reel par ouvrir les listeners
    }

    private void hideAllErrors() {
        errorNom.setVisible(false);
        errorPrenom.setVisible(false);
        errorEmail.setVisible(false);
        errorDate.setVisible(false);
    }

    private void setupRealtimeValidation() {

        nomField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateNom();
            }
        });

        prenomField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePrenom();
            }
        });

        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateEmail();
            }
        });

        dateNaissanceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateDate();
            }
        });
    }

    public void setUser(user u) {
        this.currentUser = u;
        nomField.setText(u.getUser_nom());
        prenomField.setText(u.getUser_prenom());
        emailField.setText(u.getUser_email());
        dateNaissanceField.setValue(LocalDate.parse(u.getUser_date_de_naissance()));

        roleLabel.setText("Rôle : " + u.getType_utilisateur().name());
        dateInscriptionLabel.setText("Date d'inscription : " + u.getDate_inscription());
    }


    private boolean validateNom() {
        String nom = nomField.getText();
        if (!ValidationUtils.isValidName(nom)) {
            showError(errorNom, ValidationUtils.getNameErrorMessage(nom));
            return false;
        }
        hideError(errorNom);
        return true;
    }

    private boolean validatePrenom() {
        String prenom = prenomField.getText();
        if (!ValidationUtils.isValidName(prenom)) {
            showError(errorPrenom, ValidationUtils.getNameErrorMessage(prenom));
            return false;
        }
        hideError(errorPrenom);
        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText();
        if (!ValidationUtils.isValidEmail(email)) {
            showError(errorEmail, ValidationUtils.getEmailErrorMessage(email));
            return false;
        }
        hideError(errorEmail);
        return true;
    }

    private boolean validateDate() {
        LocalDate date = dateNaissanceField.getValue();
        if (!ValidationUtils.isValidBirthDate(date)) {
            showError(errorDate, ValidationUtils.getBirthDateErrorMessage(date));
            return false;
        }
        hideError(errorDate);
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
        // Valider tous les champs
        boolean nomValid = validateNom();
        boolean prenomValid = validatePrenom();
        boolean emailValid = validateEmail();
        boolean dateValid = validateDate();

        if (!nomValid || !prenomValid || !emailValid || !dateValid) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez corriger les erreurs avant de sauvegarder.");
            alert.showAndWait();
            return;
        }

        currentUser.setUser_nom(nomField.getText().trim());
        currentUser.setUser_prenom(prenomField.getText().trim());
        currentUser.setUser_email(emailField.getText().trim());
        currentUser.setUser_date_de_naissance(dateNaissanceField.getValue().toString());

//        service.updateById(
//                currentUser.getUser_id(),
//                currentUser.getUser_nom(),
//                currentUser.getUser_prenom(),
//                currentUser.getUser_email(),
//                currentUser.getUser_password(),        // On ne change pas le mot de passe
//                currentUser.getUser_date_de_naissance(),
//                currentUser.getDate_inscription(),     // On ne change pas la date d'inscription
//                currentUser.getType_utilisateur()      // Rôle non modifiable
//        );

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