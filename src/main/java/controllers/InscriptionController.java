package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import models.Role;
import models.user;
import services.serviceUser;
import utils.ValidationUtils;

import java.io.IOException;
import java.time.LocalDate;

public class InscriptionController {

    @FXML private TextField nomInsc, prenomInsc, emailInsc;
    @FXML private PasswordField passwordInsc;
    @FXML private DatePicker dateNaissanceInsc;
    @FXML private Hyperlink linkToLogin;


    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorDate;

    @FXML private Label passwordStrengthLabel;
    @FXML private Region passwordStrengthBar;
    @FXML private HBox passwordStrengthContainer;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        linkToLogin.setOnAction(e -> goToLogin());

        hideAllErrors();

        setupRealtimeValidation();

        setupPasswordStrengthIndicator();
    }

    private void hideAllErrors() {
        errorNom.setVisible(false);
        errorPrenom.setVisible(false);
        errorEmail.setVisible(false);
        errorPassword.setVisible(false);
        errorDate.setVisible(false);
    }

    private void setupRealtimeValidation() {
        nomInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Quand le champ perd le focus
                validateNom();
            }
        });

        prenomInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePrenom();
            }
        });

        emailInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateEmail();
            }
        });

        dateNaissanceInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateDate();
            }
        });
    }

    private void setupPasswordStrengthIndicator() {
        passwordInsc.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });

        passwordInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePassword();
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            passwordStrengthLabel.setText("");
            passwordStrengthBar.setPrefWidth(0);
            return;
        }

        int strength = ValidationUtils.getPasswordStrength(password);
        String label = ValidationUtils.getPasswordStrengthLabel(strength);
        String color = ValidationUtils.getPasswordStrengthColor(strength);

        passwordStrengthLabel.setText(label);
        passwordStrengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        double width = 0;
        switch (strength) {
            case 0: width = 100; break;  // Faible = 33%
            case 1: width = 200; break;  // Moyen = 66%
            case 2: width = 300; break;  // Fort = 100%
        }

        passwordStrengthBar.setPrefWidth(width);
        passwordStrengthBar.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 5;"
        );
    }


    private boolean validateNom() {
        String nom = nomInsc.getText();
        if (!ValidationUtils.isValidName(nom)) {
            showError(errorNom, ValidationUtils.getNameErrorMessage(nom));
            return false;
        }
        hideError(errorNom);
        return true;
    }

    private boolean validatePrenom() {
        String prenom = prenomInsc.getText();
        if (!ValidationUtils.isValidName(prenom)) {
            showError(errorPrenom, ValidationUtils.getNameErrorMessage(prenom));
            return false;
        }
        hideError(errorPrenom);
        return true;
    }

    private boolean validateEmail() {
        String email = emailInsc.getText();
        if (!ValidationUtils.isValidEmail(email)) {
            showError(errorEmail, ValidationUtils.getEmailErrorMessage(email));
            return false;
        }
        hideError(errorEmail);
        return true;
    }

    private boolean validatePassword() {
        String password = passwordInsc.getText();
        if (!ValidationUtils.isValidPassword(password)) {
            showError(errorPassword, ValidationUtils.getPasswordErrorMessage(password));
            return false;
        }
        hideError(errorPassword);
        return true;
    }

    private boolean validateDate() {
        LocalDate date = dateNaissanceInsc.getValue();
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
    void handleRegister(ActionEvent event) {

        boolean nomValid = validateNom();
        boolean prenomValid = validatePrenom();
        boolean emailValid = validateEmail();
        boolean passwordValid = validatePassword();
        boolean dateValid = validateDate();

        if (!nomValid || !prenomValid || !emailValid || !passwordValid || !dateValid) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs avant de continuer.",
                    Alert.AlertType.ERROR);
            return;
        }

        user newUser = new user(
                nomInsc.getText().trim(),
                prenomInsc.getText().trim(),
                emailInsc.getText().trim(),
                passwordInsc.getText(),
                dateNaissanceInsc.getValue().toString(),
                LocalDate.now().toString(),
                Role.ETUDIANT
        );

        service.add(newUser);

        showAlert("Succès",
                "Inscription réussie ! Vous pouvez maintenant vous connecter.",
                Alert.AlertType.INFORMATION);

        goToLogin();
    }


    @FXML
    void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) linkToLogin.getScene().getWindow();
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