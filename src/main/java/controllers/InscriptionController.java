package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Role;
import models.user;
import services.serviceUser;

import java.io.IOException;

public class InscriptionController {

    @FXML private TextField nomInsc, prenomInsc, emailInsc;
    @FXML private PasswordField passwordInsc;
    @FXML private DatePicker dateNaissanceInsc;
    @FXML private Hyperlink linkToLogin;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {

        linkToLogin.setOnAction(e -> goToLogin());
    }


    @FXML
    void handleRegister(ActionEvent event) {
        if (nomInsc.getText().isEmpty() || prenomInsc.getText().isEmpty() ||
                emailInsc.getText().isEmpty() || passwordInsc.getText().isEmpty() ||
                dateNaissanceInsc.getValue() == null) {

            showAlert("Erreur", "Veuillez remplir tous les champs.", Alert.AlertType.ERROR);
            return;
        }

        user newUser = new user(
                nomInsc.getText().trim(),
                prenomInsc.getText().trim(),
                emailInsc.getText().trim(),
                passwordInsc.getText(),
                dateNaissanceInsc.getValue().toString(),
                java.time.LocalDate.now().toString(),
                Role.ETUDIANT
        );

        service.add(newUser);
        showAlert("Succès", "Inscription réussie ! Vous pouvez maintenant vous connecter.", Alert.AlertType.INFORMATION);

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