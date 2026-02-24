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

import java.io.IOException;

public class DashboardAdminController {

    // ✅ NOUVEAU : Controller de la sidebar partagée (injecté via fx:include)
    @FXML private AdminSidebarController sidebarController;

    @FXML
    public void initialize() {
        if (!checkSession()) {
            switchToLogin();
            return;
        }
        // ✅ Activer le bouton "Dashboard" dans la sidebar
        if (sidebarController != null) {
            sidebarController.setActiveButton("dashboard");
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

    // Bouton "Ouvrir →" dans la zone principale du dashboard
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

    private void switchToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage currentStage = (Stage) javafx.stage.Window.getWindows().get(0);
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Harmony - Connexion");
            currentStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
