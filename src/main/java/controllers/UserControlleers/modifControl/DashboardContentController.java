package controllers.UserControlleers.modifControl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Controller du contenu dashboard.
 * Léger : le bouton "Ouvrir →" déclenche la navigation vers GestionUsersContent
 * en passant par le DashboardAdminController parent (qui gère le StackPane).
 *
 * Astuce : on remonte au DashboardAdminController via la scène.
 */
public class DashboardContentController {

    @FXML
    void openGestionUsers(ActionEvent event) {
        // Récupérer le controller parent (DashboardAdminController) depuis la scène
        // Le StackPane contentArea est dans DashboardAdmin.fxml dont le controller est DashboardAdminController
        try {
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            // Remonter jusqu'à la racine BorderPane de DashboardAdmin.fxml
            javafx.scene.layout.BorderPane root =
                    (javafx.scene.layout.BorderPane) source.getScene().getRoot();

            // Charger GestionUsersContent dans le StackPane contentArea
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/GestionUsersContent.fxml"));
            Parent content = loader.load();

            javafx.scene.layout.StackPane contentArea =
                    (javafx.scene.layout.StackPane) root.getCenter();

            javafx.animation.FadeTransition fadeOut =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(180),
                            (javafx.scene.Node) contentArea.getChildren().get(0));
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().setAll(content);
                javafx.animation.FadeTransition fadeIn =
                        new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(220), content);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
