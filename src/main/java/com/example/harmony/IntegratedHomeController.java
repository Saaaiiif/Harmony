package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import models.Role;
import models.Session;
import models.user;

import java.io.IOException;

public class IntegratedHomeController {

    @FXML
    private Button forumButton;

    @FXML
    private Button planningButton;

    @FXML
    private Button adminPlanningButton;

    @FXML
    private void initialize() {
        user u = Session.getInstance().getUser();
        boolean isAdmin = u != null && u.getType_utilisateur() == Role.ADMIN;
        if (adminPlanningButton != null) {
            adminPlanningButton.setVisible(isAdmin);
            adminPlanningButton.setManaged(isAdmin);
        }
    }

    @FXML
    private void openForum() throws IOException {
        user u = Session.getInstance().getUser();
        if (u == null) return;

        String fxml = (u.getType_utilisateur() == Role.ADMIN)
                ? "/views/DashboardAdmin.fxml"
                : "/views/Accueil.fxml";

        switchScene(fxml, "Harmony — Forum & Utilisateurs");
    }

    @FXML
    private void openPlanning() throws IOException {
        user u = Session.getInstance().getUser();
        if (u == null) return;

        switchScene("/com/example/harmony/front-layout.fxml", "Harmony — Planning & Tâches");
    }

    @FXML
    private void openAdminPlanning() throws IOException {
        switchScene("/com/example/harmony/admin-backoffice.fxml", "Harmony — Backoffice Planning");
    }

    private Stage getStage() {
        if (forumButton != null && forumButton.getScene() != null && forumButton.getScene().getWindow() instanceof Stage s) {
            return s;
        }
        if (planningButton != null && planningButton.getScene() != null && planningButton.getScene().getWindow() instanceof Stage s) {
            return s;
        }
        if (adminPlanningButton != null && adminPlanningButton.getScene() != null && adminPlanningButton.getScene().getWindow() instanceof Stage s) {
            return s;
        }
        return null;
    }

    private void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = getStage();
        if (stage == null) return;

        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.centerOnScreen();

        Object controller = loader.getController();
        if (controller instanceof FrontLayoutController flc) {
            flc.setStage(stage);
        }
    }
}

