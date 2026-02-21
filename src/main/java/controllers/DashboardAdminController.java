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
            switchToLogin();
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
    void handleLogout(ActionEvent event) {
        // Logique de session (inchangée comme demandé)
        Session session = Session.getInstance();
        SessionDAO dao = new SessionDAO();
        if (session.getToken() != null) {
            dao.deleteSession(session.getToken());
        }
        session.clearSession();
        deleteRememberFile();

        // Fermeture propre + passage au Login dans la même fenêtre
        switchToLogin(event);
    }

    private void switchToLogin() {
        switchToLogin(null);
    }

    private void switchToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));

            Stage currentStage;
            if (event != null) {
                currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            } else {
                currentStage = (Stage) javafx.stage.Window.getWindows().get(0);
            }

            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Harmony - Connexion");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteRememberFile() {
        new File("remember.dat").delete();
    }
}