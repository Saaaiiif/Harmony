package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardAdminController {

    @FXML
    void openGestionUsers(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/GestionUsers.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Gestion des Utilisateurs");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}