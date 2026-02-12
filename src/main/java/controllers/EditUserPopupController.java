package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Role;
import models.user;
import services.serviceUser;

public class EditUserPopupController {

    @FXML private TextField nomField, prenomField, emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private Label roleLabel;

    private user currentUser;
    private final serviceUser service = new serviceUser();

    // Méthode appelée depuis GestionUsersController
    public void setUser(user u) {
        this.currentUser = u;
        nomField.setText(u.getUser_nom());
        prenomField.setText(u.getUser_prenom());
        emailField.setText(u.getUser_email());
        dateNaissanceField.setValue(java.time.LocalDate.parse(u.getUser_date_de_naissance()));
        roleLabel.setText("Rôle : " + u.getType_utilisateur().name());
    }

    @FXML
    void saveChanges() {
        currentUser.setUser_nom(nomField.getText().trim());
        currentUser.setUser_prenom(prenomField.getText().trim());
        currentUser.setUser_email(emailField.getText().trim());
        currentUser.setUser_date_de_naissance(dateNaissanceField.getValue().toString());

        // Mise à jour du mot de passe seulement s'il est saisi
        if (!passwordField.getText().isEmpty()) {
            currentUser.setUser_password(passwordField.getText());
        }

        service.updateById(
                currentUser.getUser_id(),
                currentUser.getUser_nom(),
                currentUser.getUser_prenom(),
                currentUser.getUser_email(),
                currentUser.getUser_password(),
                currentUser.getUser_date_de_naissance(),
                currentUser.getDate_inscription(),
                currentUser.getType_utilisateur()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Modifications enregistrées avec succès !");
        alert.showAndWait();

        closeWindow();
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}