package controllers.UserControlleers.modifControl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller du contenu dashboard (page d'accueil admin).
 * Les boutons "Ouvrir →" chargent les pages correspondantes
 * dans le StackPane contentArea du DashboardAdmin.
 */
public class DashboardContentController {

    // ── Méthode utilitaire : récupère le StackPane contentArea depuis la scène ──

    private StackPane getContentArea(ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        BorderPane root = (BorderPane) source.getScene().getRoot();
        return (StackPane) root.getCenter();
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            StackPane contentArea = getContentArea(event);

            if (contentArea.getChildren().isEmpty()) {
                contentArea.getChildren().setAll(content);
            } else {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(180),
                        contentArea.getChildren().get(0));
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(content);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), content);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Gestion Utilisateurs ──────────────────────────────────────────────────

    @FXML
    void openGestionUsers(ActionEvent event) {
        navigateTo("/views/UserViews/GestionUsersContent.fxml", event);
    }

    // ── Gestion Forum ─────────────────────────────────────────────────────────

    @FXML
    void openGestionForum(ActionEvent event) {
        navigateTo("/views/ForumViews/ForumBackOffice/ForumBackDashboard.fxml", event);
    }

    // ── Gestion Sport ─────────────────────────────────────────────────────────

    @FXML
    void openGestionSport(ActionEvent event) {
        navigateTo("/views/ActiviteViews/GestionSport.fxml", event);
    }

    // ── Gestion Nutrition ─────────────────────────────────────────────────────

    @FXML
    void openGestionNutrition(ActionEvent event) {
        navigateTo("/views/ActiviteViews/GestionNutrition.fxml", event);
    }
}