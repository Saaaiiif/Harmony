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
import models.ForumModels.Commentaire;
import services.ForumServices.BadWordsService;
import services.ForumServices.ServiceCommentaire;
import javafx.geometry.Insets;
import java.util.List;

public class CommentairesCardController {

    @FXML private FlowPane commentairesContainer;
    private ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private BadWordsService badWordsService = new BadWordsService(); // ✅ AJOUTÉ

    @FXML public void initialize() { loadCommentaires(); }
    @FXML private void handleRefresh() { loadCommentaires(); }

    public void loadCommentaires() {
        commentairesContainer.getChildren().clear();
        List<Commentaire> list = serviceCommentaire.getAll();
        for (Commentaire c : list) {
            boolean flagged = badWordsService.containsBadWordsLocal(c.getContenu()); // ✅
            commentairesContainer.getChildren().add(buildCard(c, flagged));
        }
    }

    private VBox buildCard(Commentaire c, boolean isFlagged) {
        VBox card = new VBox(0);
        card.setPrefWidth(270);

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
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 18px;");
        headerRow.getChildren().add(icon);
        if (isFlagged) {
            Label badge = new Label("🚨 Signalé");
            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;"
                    + "-fx-font-size: 10px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 6; -fx-padding: 2 8;");
            headerRow.getChildren().add(badge);
        }

        String ct = c.getContenu() != null
                ? (c.getContenu().length() > 100 ? c.getContenu().substring(0, 100) + "..." : c.getContenu())
                : "—";
        Label contenu = new Label(ct);
        contenu.setStyle(isFlagged
                ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dc2626;"
                : "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        contenu.setWrapText(true);
        contenu.setMaxWidth(240);

        Label postLbl = new Label("📝 " + (c.getTitrePost() != null ? c.getTitrePost() : "Post inconnu"));
        postLbl.getStyleClass().add("bo-card-desc");

        Label meta = new Label("👤 " + (c.getNomEtudiant() != null ? c.getNomEtudiant() : "Anonyme"));
        meta.getStyleClass().add("bo-card-meta");

        Separator sep = new Separator();

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().add("bo-btn-edit");
        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("bo-btn-delete");

        btnDelete.setOnAction(e -> { serviceCommentaire.delete(c); loadCommentaires(); });
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/ForumViews/ForumBackOffice/EditCommentaire.fxml")); // ✅ ton vrai chemin
                Parent root = loader.load();
                EditCommentaireController ctrl = loader.getController();
                ctrl.setCommentaire(c);
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Commentaire");
                stage.showAndWait();
                loadCommentaires();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        actions.getChildren().addAll(btnEdit, btnDelete);

        if (isFlagged) {
            Button btnForce = new Button("⛔ Retirer");
            btnForce.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;"
                    + "-fx-font-weight: bold; -fx-font-size: 11px;"
                    + "-fx-background-radius: 7; -fx-padding: 5 10; -fx-cursor: hand;");
            btnForce.setOnAction(e -> { serviceCommentaire.delete(c); loadCommentaires(); });
            actions.getChildren().add(btnForce);
        }

        body.getChildren().addAll(headerRow, contenu, postLbl, meta, sep, actions);
        card.getChildren().addAll(accent, body);
        return card;
    }
}
