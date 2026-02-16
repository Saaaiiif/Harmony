package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.user;
import services.serviceUser;

import java.io.IOException;

public class GestionUsersController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;

    private final serviceUser service = new serviceUser();
    private ObservableList<user> observableList;

    @FXML
    public void initialize() {
        observableList = FXCollections.observableArrayList(service.getAll());
        displayUserCards();
    }

    private void displayUserCards() {
        cardsContainer.getChildren().clear();

        for (user u : observableList) {
            VBox card = createUserCard(u);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createUserCard(user u) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setPrefHeight(280);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 15;"
        );


        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.3), 15, 0, 0, 5);" +
                        "-fx-border-color: #8B5CF6;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 15;"
        ));


        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("👤");
        iconLabel.setStyle("-fx-font-size: 32;");

        VBox headerInfo = new VBox(3);
        Label nameLabel = new Label(u.getUser_prenom() + " " + u.getUser_nom());
        nameLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label roleLabel = new Label(u.getType_utilisateur().name());
        roleLabel.setPadding(new Insets(3, 10, 3, 10));
        roleLabel.setStyle(
                "-fx-background-color: " + (u.getType_utilisateur().name().equals("ADMIN") ? "#8B5CF6" : "#6366F1") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;"
        );

        headerInfo.getChildren().addAll(nameLabel, roleLabel);
        header.getChildren().addAll(iconLabel, headerInfo);


        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(10, 0, 0, 0));

        Label emailLabel = new Label("📧 " + u.getUser_email());
        emailLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");

        Label birthLabel = new Label("🎂 " + u.getUser_date_de_naissance());
        birthLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");

        Label inscriptionLabel = new Label("📅 Inscrit le " + u.getDate_inscription());
        inscriptionLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #9CA3AF;");

        infoBox.getChildren().addAll(emailLabel, birthLabel, inscriptionLabel);


        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);


        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Button btnEdit = new Button("✏️ Modifier");
        btnEdit.setPrefWidth(140);
        btnEdit.setStyle(
                "-fx-background-color: #8B5CF6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 15 8 15;" +
                        "-fx-cursor: hand;"
        );

        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                "-fx-background-color: #7C3AED;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 15 8 15;" +
                        "-fx-cursor: hand;"
        ));

        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                "-fx-background-color: #8B5CF6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 15 8 15;" +
                        "-fx-cursor: hand;"
        ));

        btnEdit.setOnAction(e -> editUser(u));

        Button btnDelete = new Button("🗑️");
        btnDelete.setPrefWidth(45);
        btnDelete.setStyle(
                "-fx-background-color: #EF4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-cursor: hand;"
        );

        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(
                "-fx-background-color: #DC2626;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-cursor: hand;"
        ));

        btnDelete.setOnMouseExited(e -> btnDelete.setStyle(
                "-fx-background-color: #EF4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;" +
                        "-fx-cursor: hand;"
        ));

        btnDelete.setOnAction(e -> deleteUser(u));

        actionBox.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(header, infoBox, spacer, actionBox);

        return card;
    }

    private void deleteUser(user selected) {
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cet utilisateur ?");
        confirm.setContentText(selected.getUser_prenom() + " " + selected.getUser_nom() + " sera définitivement supprimé.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            service.deleteById(selected.getUser_id());
            observableList.remove(selected);
            displayUserCards();
        }
    }

    private void editUser(user selected) {
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EditUserPopup.fxml"));
            Parent root = loader.load();

            EditUserPopupController popupController = loader.getController();
            popupController.setUser(selected);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Utilisateur");
            popupStage.setScene(new Scene(root));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.showAndWait();

            observableList.setAll(service.getAll());
            displayUserCards();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void refreshTable() {
        observableList.setAll(service.getAll());
        displayUserCards();
    }

    @FXML
    void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase().trim();

        if (searchTerm.isEmpty()) {
            displayUserCards();
            return;
        }

        cardsContainer.getChildren().clear();

        for (user u : observableList) {
            if (u.getUser_nom().toLowerCase().contains(searchTerm) ||
                    u.getUser_prenom().toLowerCase().contains(searchTerm) ||
                    u.getUser_email().toLowerCase().contains(searchTerm)) {
                cardsContainer.getChildren().add(createUserCard(u));
            }
        }
    }
}