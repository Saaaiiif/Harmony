package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.user;
import services.serviceUser;

public class EditUserPopupController {

    @FXML private TextField nomField, prenomField, emailField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private Label roleLabel;
    @FXML private Label dateInscriptionLabel;

    private user currentUser;
    private final serviceUser service = new serviceUser();

    public void setUser(user u) {
        this.currentUser = u;
        nomField.setText(u.getUser_nom());
        prenomField.setText(u.getUser_prenom());
        emailField.setText(u.getUser_email());
        dateNaissanceField.setValue(java.time.LocalDate.parse(u.getUser_date_de_naissance()));

        roleLabel.setText("Rôle : " + u.getType_utilisateur().name());
        dateInscriptionLabel.setText("Date d'inscription : " + u.getDate_inscription());
    }

    @FXML
    void saveChanges() {
        currentUser.setUser_nom(nomField.getText().trim());
        currentUser.setUser_prenom(prenomField.getText().trim());
        currentUser.setUser_email(emailField.getText().trim());
        currentUser.setUser_date_de_naissance(dateNaissanceField.getValue().toString());

        service.updateById(
                currentUser.getUser_id(),
                currentUser.getUser_nom(),
                currentUser.getUser_prenom(),
                currentUser.getUser_email(),
                currentUser.getUser_password(),        // On ne change pas le mot de passe
                currentUser.getUser_date_de_naissance(),
                currentUser.getDate_inscription(),     // On ne change pas la date d'inscription
                currentUser.getType_utilisateur()      // Rôle non modifiable
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