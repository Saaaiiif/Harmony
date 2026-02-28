package controllers.ForumControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.ForumServices.ServiceCategorie;
import services.ForumServices.JokeNotificationService;   // ✅ NOUVEAU
import models.ForumModels.Categorie;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class ForumHomeController {

    // ════════════════════════AI══════════════════════════════════════

    @FXML
    private void openImageGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/ImageGenerator.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("✨ AI Image Generator");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    private ServiceCategorie serviceCategorie = new ServiceCategorie();

    @FXML
    private FlowPane categoriesPane;

    // ✅ NOUVEAU : overlay pour les toasts (à ajouter dans ForumHome.fxml)
    @FXML
    private StackPane toastOverlay;

    // ✅ NOUVEAU : service de notifications fun
    private JokeNotificationService jokeService;

    private AccueilController accueilController;

    public void setAccueilController(AccueilController controller) {
        this.accueilController = controller;
    }

    @FXML
    public void initialize() {
        loadCategoriesFromDB();

        // ✅ NOUVEAU : démarrer les notifications fun dès l'ouverture du forum
        if (toastOverlay != null) {
            jokeService = new JokeNotificationService(toastOverlay);
            jokeService.start();
        }
    }

    @FXML
    private void handleBack() {
        // ✅ NOUVEAU : arrêter le service quand on quitte la page
        if (jokeService != null) {
            jokeService.stop();
        }
        if (accueilController != null) {
            accueilController.goHome();
        }
    }

    private void loadCategoriesFromDB() {
        categoriesPane.getChildren().clear();

        List<Categorie> categories = serviceCategorie.getAll();

        for (Categorie c : categories) {
            addCategorieCard(c);
        }

        addAddCategorieCard();
    }

    private void addCategorieCard(Categorie categorie) {

        VBox card = new VBox();
        card.getStyleClass().add("forum-card");

        card.setPrefWidth(200);
        card.setPrefHeight(120);
        card.setMinWidth(200);
        card.setMinHeight(120);
        card.setMaxWidth(200);
        card.setMaxHeight(120);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setSpacing(8);

        Label lbl = new Label(categorie.getNomCategorie());
        lbl.getStyleClass().add("forum-card-title");
        lbl.setWrapText(true);
        lbl.setAlignment(javafx.geometry.Pos.CENTER);
        lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lbl.setMaxWidth(180);

        if (categorie.getDescription() != null && !categorie.getDescription().isEmpty()) {
            String desc = categorie.getDescription();
            if (desc.length() > 50) desc = desc.substring(0, 47) + "...";
            Label descLbl = new Label(desc);
            descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
            descLbl.setWrapText(true);
            descLbl.setAlignment(javafx.geometry.Pos.CENTER);
            descLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            descLbl.setMaxWidth(180);
            card.getChildren().addAll(lbl, descLbl);
        } else {
            card.getChildren().add(lbl);
        }

        card.setOnMouseClicked(e -> openPostsPage(categorie));
        categoriesPane.getChildren().add(card);
    }

    private void openPostsPage(Categorie categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/ForumPosts.fxml")
            );

            Parent page = loader.load();

            ForumPostsController controller = loader.getController();
            controller.setCategorie(categorie);
            controller.setAccueilController(accueilController);

            accueilController.setContent(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAddCategorieCard() {

        VBox card = new VBox();
        card.getStyleClass().add("forum-add-card");

        card.setPrefWidth(200);
        card.setPrefHeight(120);
        card.setMinWidth(200);
        card.setMinHeight(120);
        card.setMaxWidth(200);
        card.setMaxHeight(120);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setSpacing(4);

        Label plus = new Label("+");
        plus.getStyleClass().add("forum-add-plus");

        Label addLabel = new Label("Nouvelle catégorie");
        addLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.8);");

        card.getChildren().addAll(plus, addLabel);

        card.setOnMouseClicked(e -> openAddCategorieForm());

        categoriesPane.getChildren().add(card);
    }

    private void openAddCategorieForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/AddCategorie.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Catégorie");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadCategoriesFromDB();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}