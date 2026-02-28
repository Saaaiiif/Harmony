package com.example.harmony.user.controllers;

import com.example.harmony.user.models.Role;
import com.example.harmony.user.models.user;
import com.example.harmony.user.services.serviceUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailLogin;
    @FXML private PasswordField passwordLogin;
    @FXML private Label errorLabel;
    @FXML private Hyperlink linkToRegister;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        linkToRegister.setOnAction(e -> goToRegister());
    }

    // ====================== CONNEXION ======================
    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailLogin.getText().trim();
        String pass = passwordLogin.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            errorLabel.setVisible(true);
            return;
        }

        user utilisateur = service.getByEmailAndPassword(email, pass);

        if (utilisateur != null) {
            redirectAccordingToRole(utilisateur);
        } else {
            errorLabel.setText("Email ou mot de passe incorrect");
            errorLabel.setVisible(true);
        }
    }

    // ====================== REDIRECTION SELON RÔLE ======================
    private void redirectAccordingToRole(user user) {
        try {
            String fxmlPath = (user.getType_utilisateur() == Role.ETUDIANT)
                    ? "/views/Accueil.fxml"
                    : "/views/DashboardAdmin.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) emailLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(user.getType_utilisateur() == Role.ETUDIANT
                    ? "Accueil - Harmony"
                    : "Dashboard Admin - Harmony");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ====================== ALLER VERS INSCRIPTION ======================
    @FXML
    void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Inscription.fxml"));
            Stage stage = (Stage) linkToRegister.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}