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
import models.ForumModels.Categorie;
import services.ForumServices.BadWordsService;
import services.ForumServices.ServiceCategorie;
import javafx.geometry.Insets;
import java.util.List;

public class CategoriesCardController {

    private PostsCardController postsController;
    private CommentairesCardController commentairesController;

    public void setOtherControllers(PostsCardController postsController,
                                    CommentairesCardController commentairesController) {
        this.postsController = postsController;
        this.commentairesController = commentairesController;
    }

    @FXML private FlowPane categoriesContainer;
    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private BadWordsService badWordsService = new BadWordsService(); // ✅ AJOUTÉ

    @FXML public void initialize() { loadCategories(); }
    @FXML private void handleRefresh() { loadCategories(); }

    public void loadCategories() {
        categoriesContainer.getChildren().clear();
        List<Categorie> list = serviceCategorie.getAll();
        for (Categorie cat : list) {
            boolean flagged = badWordsService.containsBadWordsLocal(cat.getNomCategorie())
                    || badWordsService.containsBadWordsLocal(cat.getDescription()); // ✅
            categoriesContainer.getChildren().add(buildCard(cat, flagged));
        }
    }

    private VBox buildCard(Categorie cat, boolean isFlagged) {
        VBox card = new VBox(0);
        card.setPrefWidth(250);

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

        HBox headerRow = new HBox(8);
        Label icon = new Label("🗂");
        icon.setStyle("-fx-font-size: 18px;");
        headerRow.getChildren().add(icon);
        if (isFlagged) {
            Label badge = new Label("🚨 Signalé");
            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;"
                    + "-fx-font-size: 10px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 6; -fx-padding: 2 8;");
            headerRow.getChildren().add(badge);
        }

        Label nom = new Label(cat.getNomCategorie() != null ? cat.getNomCategorie() : "—");
        nom.setStyle(isFlagged
                ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dc2626;"
                : "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        nom.setWrapText(true);
        nom.setMaxWidth(220);

        Label desc = new Label(cat.getDescription() != null ? cat.getDescription() : "Aucune description");
        desc.getStyleClass().add("bo-card-desc");
        desc.setWrapText(true);
        desc.setMaxWidth(220);

        Separator sep = new Separator();

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().add("bo-btn-edit");
        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("bo-btn-delete");

        btnDelete.setOnAction(e -> { serviceCategorie.delete(cat); loadCategories(); });
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/ForumViews/ForumBackOffice/EditCategorie.fxml")); // ✅ ton vrai chemin
                Parent root = loader.load();
                EditCategorieController ctrl = loader.getController();
                ctrl.setCategorie(cat);
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Catégorie");
                stage.showAndWait();
                loadCategories();
                if (postsController != null) postsController.loadPosts();
                if (commentairesController != null) commentairesController.loadCommentaires();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        actions.getChildren().addAll(btnEdit, btnDelete);
        body.getChildren().addAll(headerRow, nom, desc, sep, actions);
        card.getChildren().addAll(accent, body);
        return card;
    }
}
