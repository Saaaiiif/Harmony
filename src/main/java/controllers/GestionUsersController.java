package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.user;
import services.serviceUser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

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
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 15;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 15;"
        ));

        // Header avec nom/prénom
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 32;");

        VBox nameBox = new VBox(2);
        Label fullName = new Label(u.getUser_prenom() + " " + u.getUser_nom());
        fullName.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label emailLabel = new Label(u.getUser_email());
        emailLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");

        nameBox.getChildren().addAll(fullName, emailLabel);
        header.getChildren().addAll(avatar, nameBox);

        // Infos critiques
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-padding: 10 0 0 0;");

        // Âge
        int age = calculateAge(u.getUser_date_de_naissance());
        Label ageLabel = new Label("Âge: " + age + " ans");
        ageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        // Sexe
        Label sexeLabel = new Label("Sexe: " + (u.getUser_sexe() != null ? u.getUser_sexe().getDisplayName() : "Non spécifié"));
        sexeLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        // Santé basique
        Label poidsTailleLabel = new Label("Poids/Taille: " + (u.getUser_poids() != null ? u.getUser_poids() + " kg" : "N/A") + " / " + (u.getUser_taille() != null ? u.getUser_taille() + " cm" : "N/A"));
        poidsTailleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        // Scolaire
        Label niveauScolaireLabel = new Label("Niveau Scolaire: " + (u.getUser_niveau_scolaire() != null ? u.getUser_niveau_scolaire().getDisplayName() : "Non spécifié"));
        niveauScolaireLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        Label etablissementLabel = new Label("Établissement: " + (u.getUser_etablissement_scolaire() != null ? u.getUser_etablissement_scolaire() : "Non spécifié"));
        etablissementLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        infoBox.getChildren().addAll(ageLabel, sexeLabel, poidsTailleLabel, niveauScolaireLabel, etablissementLabel);

        // Bouton menu ":" en bas à droite
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MenuButton menuButton = new MenuButton("⋮");
        menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-size: 18; -fx-cursor: hand;");

        MenuItem editItem = new MenuItem("Modifier");
        editItem.setOnAction(e -> editUser(u));

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(e -> deleteUser(u));

        menuButton.getItems().addAll(editItem, deleteItem);

        bottomBox.getChildren().addAll(spacer, menuButton);

        // Double-clic pour afficher détails
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                showUserDetails(u);
            }
        });

        card.getChildren().addAll(header, infoBox, bottomBox);

        return card;
    }

    private int calculateAge(String birthDateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            LocalDate currentDate = LocalDate.now();
            return Period.between(birthDate, currentDate).getYears();
        } catch (Exception e) {
            return 0; // Default si erreur
        }
    }

    private void showUserDetails(user u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DisplayUser.fxml"));
            Parent root = loader.load();

            DisplayUserController controller = loader.getController();
            controller.setUser(u);

            Stage stage = new Stage();
            stage.setTitle("Détails Utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
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