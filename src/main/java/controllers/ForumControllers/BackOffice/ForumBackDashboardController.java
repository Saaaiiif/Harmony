package controllers.ForumControllers.BackOffice;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class ForumBackDashboardController {

    @FXML private StackPane contentArea;

    private AccueilController accueilController;

    public void setAccueilController(AccueilController controller) {
        this.accueilController = controller;
    }

    // ── Vue par défaut : contenu cards (Catégories + Posts + Commentaires) ────
    private Parent defaultContent = null;

    @FXML
    public void initialize() {
        // Sauvegarde le contenu par défaut (les 3 sections déjà incluses via fx:include)
        if (!contentArea.getChildren().isEmpty()) {
            defaultContent = (Parent) contentArea.getChildren().get(0);
        }
    }

    // ── Bouton ← Retour au dashboard admin ───────────────────────────────────
    @FXML
    private void handleBack() {
        try {
            javafx.scene.layout.BorderPane root =
                    (javafx.scene.layout.BorderPane) contentArea.getScene().getRoot();
            javafx.scene.layout.StackPane mainContent =
                    (javafx.scene.layout.StackPane) root.getCenter();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/modif/DashboardContent.fxml")
            );
            Parent dashboard = loader.load();
            mainContent.getChildren().setAll(dashboard);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Bouton 📋 Contenu — revenir aux cards ─────────────────────────────────
    @FXML
    private void showContenu() {
        try {
            // Recharge les 3 sections (CategoriesCards + PostsCards + CommentairesCards)
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/ForumBackOffice/ForumBackDashboard.fxml")
            );
            // On recharge juste le ScrollPane avec les 3 includes
            // En pratique on reconstruit le contenu par défaut
            javafx.scene.control.ScrollPane scroll = buildDefaultScroll();
            contentArea.getChildren().setAll(scroll);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Bouton 📊 Statistiques ────────────────────────────────────────────────
    @FXML
    private void showStats() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/ForumBackOffice/ForumStats.fxml")
            );
            Parent stats = loader.load();

            // Transition fade
            javafx.animation.FadeTransition ft =
                    new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), stats);
            ft.setFromValue(0);
            ft.setToValue(1);

            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(stats);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            contentArea.getChildren().setAll(scroll);
            ft.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helper : reconstruit le ScrollPane avec les 3 cards sections ──────────
    private javafx.scene.control.ScrollPane buildDefaultScroll() throws Exception {
        FXMLLoader catLoader = new FXMLLoader(
                getClass().getResource("/views/ForumViews/ForumBackOffice/CategoriesCards.fxml"));
        FXMLLoader postLoader = new FXMLLoader(
                getClass().getResource("/views/ForumViews/ForumBackOffice/PostsCards.fxml"));
        FXMLLoader comLoader = new FXMLLoader(
                getClass().getResource("/views/ForumViews/ForumBackOffice/CommentairesCards.fxml"));

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(20);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.getChildren().addAll(catLoader.load(), postLoader.load(), comLoader.load());

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }
}
