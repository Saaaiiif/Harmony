package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.Session;
import models.Role;
import models.user;
import services.SessionDAO;

import java.io.File;
import java.io.IOException;

public class DashboardAdminController {

    @FXML
    public void initialize() {
        if (!checkSession()) {
            redirectToLogin();
        }
    }

    private boolean checkSession() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn()) return false;

        SessionDAO dao = new SessionDAO();
        boolean valid = dao.isTokenValid(session.getToken());
        if (valid) {
            user currentUser = session.getUser();
            return currentUser != null && currentUser.getType_utilisateur() == Role.ADMIN;
        }
        return false;
    }

    private void redirectToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) /* vous pouvez utiliser n'importe quel node chargé après initialize, ou créer une nouvelle fenêtre */
                    new Stage(); // solution simple et fonctionnelle
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
            stage.show();
            // fermer l'ancienne fenêtre si nécessaire
            Stage current = (Stage) /* si vous avez un node fx:id dans le FXML, utilisez-le */ new Stage().getScene().getWindow();
            current.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    @FXML
    void handleLogout() {
        Session session = Session.getInstance();
        SessionDAO dao = new SessionDAO();
        if (session.getToken() != null) {
            dao.deleteSession(session.getToken());
        }
        session.clearSession();
        deleteRememberFile();
        redirectToLogin();
    }

    private void deleteRememberFile() {
        new File("remember.dat").delete();
    }
}