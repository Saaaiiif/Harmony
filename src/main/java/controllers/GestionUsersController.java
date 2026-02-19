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
        VBox card = new VBox(15);
        card.setPrefWidth(340);
        card.setMinHeight(280);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));

        // Style de base de la carte
        String defaultStyle = "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 5);" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 20;";

        card.setStyle(defaultStyle);

        // Effet dynamique au survol
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white;" +
                    "-fx-background-radius: 20;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.25), 20, 0, 0, 8);" +
                    "-fx-border-color: #8B5CF6;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 20;");
            card.setTranslateY(-5); // Fait "sauter" la carte vers le haut
        });

        card.setOnMouseExited(e -> {
            card.setStyle(defaultStyle);
            card.setTranslateY(0); // Remet la carte en place
        });

        // --- HEADER (Avatar + Nom) ---
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar stylisé avec initiale
        StackPane avatarBox = new StackPane();
        avatarBox.setPrefSize(50, 50);
        avatarBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #A78BFA, #8B5CF6); -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.4), 8, 0, 0, 3);");

        String initial = (u.getUser_prenom() != null && !u.getUser_prenom().isEmpty()) ? u.getUser_prenom().substring(0, 1).toUpperCase() : "U";
        Label avatarText = new Label(initial);
        avatarText.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        avatarBox.getChildren().add(avatarText);

        VBox nameBox = new VBox(2);
        Label fullName = new Label(u.getUser_prenom() + " " + u.getUser_nom());
        fullName.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label emailLabel = new Label(u.getUser_email());
        emailLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");

        nameBox.getChildren().addAll(fullName, emailLabel);
        header.getChildren().addAll(avatarBox, nameBox);

        // --- INFOS (Badges/Chips) ---
        FlowPane infoBox = new FlowPane(8, 8); // Espacement horizontal et vertical
        infoBox.setStyle("-fx-padding: 10 0 10 0;");

        int age = calculateAge(u.getUser_date_de_naissance());
        String sexeStr = u.getUser_sexe() != null ? u.getUser_sexe().getDisplayName() : "N/A";
        String niveauStr = u.getUser_niveau_scolaire() != null ? u.getUser_niveau_scolaire().getDisplayName() : "N/A";
        String etablissementStr = u.getUser_etablissement_scolaire() != null ? u.getUser_etablissement_scolaire() : "N/A";
        String santeStr = (u.getUser_poids() != null ? u.getUser_poids() + "kg" : "-") + " / " + (u.getUser_taille() != null ? u.getUser_taille() + "cm" : "-");

        infoBox.getChildren().addAll(
                createChip("🎂 " + age + " ans", "#F3E8FF", "#6B21A8"), // Violet clair
                createChip("🚻 " + sexeStr, "#E0F2FE", "#0369A1"), // Bleu clair
                createChip("⚖️ " + santeStr, "#D1FAE5", "#065F46"), // Vert clair
                createChip("🎓 " + niveauStr, "#FEF3C7", "#92400E"), // Jaune clair
                createChip("🏫 " + etablissementStr, "#FCE7F3", "#9D174D") // Rose clair
        );

        // --- BOTTOM (MenuButton ":" stylisé) ---
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MenuButton menuButton = new MenuButton("⋮");
        // Style du bouton
        menuButton.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12; -fx-text-fill: #4B5563; -fx-font-size: 16; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 12 2 12;");

        // Items avec icônes
        MenuItem editItem = new MenuItem("✏️ Modifier");
        editItem.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");
        editItem.setOnAction(e -> editUser(u));

        MenuItem deleteItem = new MenuItem("🗑️ Supprimer");
        deleteItem.setStyle("-fx-font-size: 14; -fx-text-fill: #DC2626;"); // Rouge pour suppression
        deleteItem.setOnAction(e -> deleteUser(u));

        menuButton.getItems().addAll(editItem, deleteItem);
        bottomBox.getChildren().addAll(spacer, menuButton);

        // --- INTERACTIONS ---
        // Double-clic pour afficher détails
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                showUserDetails(u);
            }
        });

        card.getChildren().addAll(header, infoBox, bottomBox);

        return card;
    }

    // Méthode utilitaire pour créer les jolis badges
    private Label createChip(String text, String bgColor, String textColor) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-padding: 6 12 6 12; " +
                "-fx-background-radius: 15; " +
                "-fx-font-size: 12; " +
                "-fx-font-weight: bold;");
        return chip;
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