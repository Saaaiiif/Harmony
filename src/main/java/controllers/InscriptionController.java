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
import models.UserRegistrationData;
import utils.ValidationUtils;

import java.io.IOException;
import java.time.LocalDate;

public class InscriptionController {

    @FXML private TextField nomInsc, prenomInsc, emailInsc;
    @FXML private PasswordField passwordInsc;
    @FXML private DatePicker dateNaissanceInsc;
    @FXML private Hyperlink linkToLogin;
    
    // Labels d'erreur
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorDate;
    
    // Indicateur de force du mot de passe
    @FXML private Label passwordStrengthLabel;
    @FXML private Region passwordStrengthBar;
    @FXML private HBox passwordStrengthContainer;

    @FXML
    public void initialize() {
        linkToLogin.setOnAction(e -> goToLogin());
        
        hideAllErrors();
        setupRealtimeValidation();
        setupPasswordStrengthIndicator();
    }
    public void setUserData(UserRegistrationData data) {
        if (data != null) {
            nomInsc.setText(data.getNom());
            prenomInsc.setText(data.getPrenom());
            emailInsc.setText(data.getEmail());
            passwordInsc.setText(data.getPassword());  // Mot de passe en clair (temporaire)
            try {
                dateNaissanceInsc.setValue(LocalDate.parse(data.getDateNaissance()));
            } catch (Exception e) {
                // Ignorer si date invalide, mais loggez si needed
                System.err.println("Erreur lors du parsing de la date: " + e.getMessage());
            }
            hideAllErrors();  // Réinitialiser les erreurs
        }
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
            if (!newVal) validateNom();
        });
        
        prenomInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validatePrenom();
        });
        
        emailInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateEmail();
        });
        
        dateNaissanceInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateDate();
        });
    }

    private void setupPasswordStrengthIndicator() {
        passwordInsc.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
        
        passwordInsc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validatePassword();
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
            case 0: width = 100; break;
            case 1: width = 200; break;
            case 2: width = 300; break;
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

    // ====================== CONTINUER VERS ÉTAPE 2 ======================
    
    @FXML
    void handleRegister(ActionEvent event) {
        // Valider tous les champs de l'étape 1
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

        // Créer l'objet de données temporaires
        UserRegistrationData tempData = new UserRegistrationData(
                nomInsc.getText().trim(),
                prenomInsc.getText().trim(),
                emailInsc.getText().trim(),
                passwordInsc.getText(), // Ne pas trim le mot de passe
                dateNaissanceInsc.getValue().toString()
        );

        // Charger l'étape 2 et passer les données
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InscriptionStep2.fxml"));
            Parent root = loader.load();

            // Récupérer le controller de l'étape 2 et lui passer les données
            InscriptionStep2Controller step2Controller = loader.getController();
            step2Controller.setUserData(tempData);

            // Changer de scène
            Stage stage = (Stage) nomInsc.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription (Étape 2/2)");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", 
                     "Impossible de charger l'étape suivante.", 
                     Alert.AlertType.ERROR);
        }
    }

    // ====================== RETOUR VERS LOGIN ======================
    
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
