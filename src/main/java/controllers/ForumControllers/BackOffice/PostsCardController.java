package controllers.ForumControllers.BackOffice;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.ForumModels.Post;
import services.ForumServices.BadWordsService;
import services.ForumServices.ServicePost;
import javafx.geometry.Insets;
import java.util.List;

public class PostsCardController {

    @FXML private FlowPane postsContainer;
    private ServicePost servicePost = new ServicePost();
    private BadWordsService badWordsService = new BadWordsService(); // ✅ AJOUTÉ

    @FXML public void initialize() { loadPosts(); }
    @FXML private void handleRefresh() { loadPosts(); }

    public void loadPosts() {
        postsContainer.getChildren().clear();
        List<Post> posts = servicePost.getAll();
        for (Post post : posts) {
            // ✅ AJOUTÉ — détection locale rapide
            boolean flagged = badWordsService.containsBadWordsLocal(post.getTitre())
                    || badWordsService.containsBadWordsLocal(post.getContenu());
            postsContainer.getChildren().add(buildCard(post, flagged));
        }
    }

    private VBox buildCard(Post post, boolean isFlagged) {
        VBox card = new VBox(0);
        card.setPrefWidth(270);

        // ✅ MODIFIÉ — style rouge si signalé, violet sinon
        if (isFlagged) {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                    + "-fx-border-radius: 12; -fx-border-color: #fca5a5;"
                    + "-fx-border-width: 1.5;"
                    + "-fx-effect: dropshadow(gaussian,rgba(239,68,68,0.18),10,0,0,3);");
        } else {
            card.getStyleClass().add("bo-card");
        }

        Pane accent = new Pane();
        accent.setPrefHeight(4);
        accent.setStyle(isFlagged
                ? "-fx-background-color: #ef4444; -fx-background-radius: 12 12 0 0;"
                : "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #7c3aed, #a855f7); -fx-background-radius: 12 12 0 0;");

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 12, 14));

        // ✅ AJOUTÉ — header avec badge si signalé
        HBox headerRow = new HBox(8);
        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 18px;");
        headerRow.getChildren().add(icon);
        if (isFlagged) {
            Label badge = new Label("🚨 Signalé");
            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;"
                    + "-fx-font-size: 10px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 6; -fx-padding: 2 8;");
            headerRow.getChildren().add(badge);
        }

        Label titre = new Label(post.getTitre() != null ? post.getTitre() : "—");
        titre.setStyle(isFlagged && badWordsService.containsBadWordsLocal(post.getTitre())
                ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dc2626;"
                : "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        titre.setWrapText(true);
        titre.setMaxWidth(240);

        String ct = post.getContenu() != null
                ? (post.getContenu().length() > 80 ? post.getContenu().substring(0, 80) + "..." : post.getContenu())
                : "";
        Label contenu = new Label(ct);
        contenu.setStyle(isFlagged && badWordsService.containsBadWordsLocal(post.getContenu())
                ? "-fx-font-size: 12px; -fx-text-fill: #ef4444;"
                : "-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        contenu.setWrapText(true);
        contenu.setMaxWidth(240);

        Label meta = new Label("👤 " + (post.getNomEtudiant() != null ? post.getNomEtudiant() : "Inconnu")
                + "   🗂 " + (post.getNomCategorie() != null ? post.getNomCategorie() : ""));
        meta.getStyleClass().add("bo-card-meta");

        Separator sep = new Separator();

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().add("bo-btn-edit");
        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("bo-btn-delete");

        btnDelete.setOnAction(e -> { servicePost.delete(post); loadPosts(); });
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/ForumViews/ForumBackOffice/EditPost.fxml")); // ✅ ton vrai chemin
                Parent root = loader.load();
                EditPostController ctrl = loader.getController();
                ctrl.setPost(post);
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Post");
                stage.showAndWait();
                loadPosts();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        actions.getChildren().addAll(btnEdit, btnDelete);

        // ✅ AJOUTÉ — bouton retrait rapide si signalé
        if (isFlagged) {
            Button btnForce = new Button("⛔ Retirer");
            btnForce.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;"
                    + "-fx-font-weight: bold; -fx-font-size: 11px;"
                    + "-fx-background-radius: 7; -fx-padding: 5 10; -fx-cursor: hand;");
            btnForce.setOnAction(e -> { servicePost.delete(post); loadPosts(); });
            actions.getChildren().add(btnForce);
        }

        body.getChildren().addAll(headerRow, titre, contenu, meta, sep, actions);
        card.getChildren().addAll(accent, body);
        return card;
    }
}
