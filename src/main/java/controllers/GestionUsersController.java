package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.user;
import models.Session;
import models.Role;
import services.serviceUser;
import services.SessionDAO;
import utils.FakeAccountDetector;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class GestionUsersController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Button btnSortSuspicion;

    private final serviceUser service = new serviceUser();
    private ObservableList<user> observableList;
    private boolean sortedBySuspicion = false;

    @FXML
    public void initialize() {
        if (!checkSession()) {
            switchToLogin();
            return;
        }
        observableList = FXCollections.observableArrayList(service.getAll());
        displayUserCards();
    }

    private void switchToLogin() { switchToLogin(null); }

    private void switchToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage currentStage;
            if (event != null) currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            else currentStage = (Stage) javafx.stage.Window.getWindows().get(0);
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Harmony - Connexion");
            currentStage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private boolean checkSession() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn()) return false;
        SessionDAO dao = new SessionDAO();
        boolean valid = dao.isTokenValid(session.getToken());
        if (valid) {
            user currentUser = session.getUser();
            return currentUser != null && currentUser.getType_utilisateur() == Role.ADMIN;
        }
        return false;
    }

    private void displayUserCards() {
        cardsContainer.getChildren().clear();
        for (user u : observableList) {
            VBox card = createUserCard(u);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createUserCard(user u) {
        List<user> allUsers = observableList;
        int suspicionScore = FakeAccountDetector.calculateSuspicionScore(u, allUsers);
        String suspicionColor = FakeAccountDetector.getSuspicionColor(suspicionScore);
        String suspicionLabel = FakeAccountDetector.getSuspicionLabel(suspicionScore);

        VBox card = new VBox(15);
        card.setPrefWidth(340);
        card.setMinHeight(280);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));

        String defaultStyle = "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 5);" +
                "-fx-border-color: " + suspicionColor + ";" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 20;";

        card.setStyle(defaultStyle);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white;" +
                    "-fx-background-radius: 20;" +
                    "-fx-effect: dropshadow(three-pass-box, " + hexToRgba(suspicionColor, 0.25) + ", 20, 0, 0, 8);" +
                    "-fx-border-color: " + suspicionColor + ";" +
                    "-fx-border-width: 3;" +
                    "-fx-border-radius: 20;");
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> { card.setStyle(defaultStyle); card.setTranslateY(0); });

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- AVATAR avec image ou initiale ----
        StackPane avatarBox = new StackPane();
        avatarBox.setPrefSize(50, 50);
        avatarBox.setMinSize(50, 50);
        avatarBox.setMaxSize(50, 50);
        avatarBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #A78BFA, #8B5CF6); -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.4), 8, 0, 0, 3);");

        String imagePath = u.getUser_image_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 50, 50, true, true);
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(50);
                    imageView.setFitHeight(50);
                    imageView.setPreserveRatio(false);
                    // Clip circulaire
                    Circle clip = new Circle(25, 25, 25);
                    imageView.setClip(clip);
                    avatarBox.getChildren().add(imageView);
                } catch (Exception e) {
                    // Fallback initiale
                    addAvatarInitial(avatarBox, u);
                }
            } else {
                addAvatarInitial(avatarBox, u);
            }
        } else {
            addAvatarInitial(avatarBox, u);
        }

        VBox nameBox = new VBox(2);
        Label fullName = new Label(u.getUser_prenom() + " " + u.getUser_nom());
        fullName.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label emailLabel = new Label(u.getUser_email());
        emailLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");
        nameBox.getChildren().addAll(fullName, emailLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label suspicionBadge = new Label(suspicionLabel);
        suspicionBadge.setStyle("-fx-background-color: " + suspicionColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 5 12 5 12;" +
                "-fx-background-radius: 12;" +
                "-fx-font-size: 11;" +
                "-fx-font-weight: bold;");

        header.getChildren().addAll(avatarBox, nameBox, spacer, suspicionBadge);

        FlowPane infoBox = new FlowPane(8, 8);
        infoBox.setStyle("-fx-padding: 10 0 10 0;");

        int age = calculateAge(u.getUser_date_de_naissance());
        String sexeStr = u.getUser_sexe() != null ? u.getUser_sexe().getDisplayName() : "N/A";
        String niveauStr = u.getUser_niveau_scolaire() != null ? u.getUser_niveau_scolaire().getDisplayName() : "N/A";
        String etablissementStr = u.getUser_etablissement_scolaire() != null ? u.getUser_etablissement_scolaire() : "N/A";
        String santeStr = (u.getUser_poids() != null ? u.getUser_poids() + "kg" : "-") + " / " + (u.getUser_taille() != null ? u.getUser_taille() + "cm" : "-");

        infoBox.getChildren().addAll(
                createChip("🎂 " + age + " ans", "#F3E8FF", "#6B21A8"),
                createChip("🚻 " + sexeStr, "#E0F2FE", "#0369A1"),
                createChip("⚖️ " + santeStr, "#D1FAE5", "#065F46"),
                createChip("🎓 " + niveauStr, "#FEF3C7", "#92400E"),
                createChip("🏫 " + etablissementStr, "#FCE7F3", "#9D174D")
        );

        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        MenuButton menuButton = new MenuButton("⋮");
        menuButton.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12; -fx-text-fill: #4B5563; -fx-font-size: 16; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 12 2 12;");

        MenuItem editItem = new MenuItem("✏️ Modifier");
        editItem.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");
        editItem.setOnAction(e -> editUser(u));

        MenuItem deleteItem = new MenuItem("🗑️ Supprimer");
        deleteItem.setStyle("-fx-font-size: 14; -fx-text-fill: #DC2626;");
        deleteItem.setOnAction(e -> deleteUser(u));

        MenuItem suspicionItem = new MenuItem("🔍 Détails suspicion (" + suspicionScore + " pts)");
        suspicionItem.setStyle("-fx-font-size: 14; -fx-text-fill: " + suspicionColor + "; -fx-font-weight: bold;");
        suspicionItem.setOnAction(e -> showSuspicionDetails(u, suspicionScore));

        menuButton.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), suspicionItem);
        bottomBox.getChildren().addAll(spacer2, menuButton);

        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                showUserDetails(u);
            }
        });

        card.getChildren().addAll(header, infoBox, bottomBox);
        return card;
    }

    private void addAvatarInitial(StackPane avatarBox, user u) {
        String initial = (u.getUser_prenom() != null && !u.getUser_prenom().isEmpty())
                ? u.getUser_prenom().substring(0, 1).toUpperCase() : "U";
        Label avatarText = new Label(initial);
        avatarText.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        avatarBox.getChildren().add(avatarText);
    }

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
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (Exception e) { return 0; }
    }

    private String hexToRgba(String hex, double opacity) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return "rgba(" + r + ", " + g + ", " + b + ", " + opacity + ")";
    }

    // ==================== TRI PAR SUSPICION ====================

    @FXML
    void handleSortBySuspicion() {
        if (!sortedBySuspicion) {
            observableList.sort((u1, u2) -> {
                int score1 = FakeAccountDetector.calculateSuspicionScore(u1, observableList);
                int score2 = FakeAccountDetector.calculateSuspicionScore(u2, observableList);
                return Integer.compare(score2, score1);
            });
            btnSortSuspicion.setText("🔄 Réinitialiser tri");
            btnSortSuspicion.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.3), 10, 0, 0, 3);");
            sortedBySuspicion = true;
        } else {
            observableList.setAll(service.getAll());
            btnSortSuspicion.setText("⚠️ Trier par suspicion");
            btnSortSuspicion.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.3), 10, 0, 0, 3);");
            sortedBySuspicion = false;
        }
        displayUserCards();
    }

    // ==================== DÉTAILS SUSPICION ====================

    private void showSuspicionDetails(user u, int score) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analyse de suspicion");
        alert.setHeaderText("Compte : " + u.getUser_prenom() + " " + u.getUser_nom());

        String levelLabel = FakeAccountDetector.getSuspicionLabel(score);
        StringBuilder content = new StringBuilder();
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("Score de suspicion : ").append(score).append(" / 20 pts\n");
        content.append("Niveau : ").append(levelLabel).append("\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        content.append("Signaux détectés :\n\n");
        boolean hasSignals = false;

        String nomLower = u.getUser_nom().toLowerCase();
        if (nomLower.contains("test") || nomLower.contains("user") || nomLower.contains("aaa") || nomLower.matches(".*\\d{2,}.*")) {
            content.append("⚠️ Nom suspect : \"").append(u.getUser_nom()).append("\"\n"); hasSignals = true;
        }
        String prenomLower = u.getUser_prenom().toLowerCase();
        if (prenomLower.contains("test") || prenomLower.contains("user") || prenomLower.contains("aaa") || prenomLower.matches(".*\\d{2,}.*")) {
            content.append("⚠️ Prénom suspect : \"").append(u.getUser_prenom()).append("\"\n"); hasSignals = true;
        }
        if (u.getUser_email().matches(".*\\d{3,}.*")) {
            content.append("⚠️ Email contient beaucoup de chiffres\n"); hasSignals = true;
        }
        String emailLower = u.getUser_email().toLowerCase();
        if (emailLower.contains("temp") || emailLower.contains("test") || emailLower.contains("yopmail") || emailLower.contains("fake")) {
            content.append("⚠️ Email temporaire/suspect détecté\n"); hasSignals = true;
        }
        long duplicates = observableList.stream()
                .filter(other -> other.getUser_id() != u.getUser_id())
                .filter(other -> other.getUser_email().equalsIgnoreCase(u.getUser_email()) ||
                        (other.getUser_nom().equalsIgnoreCase(u.getUser_nom()) && other.getUser_prenom().equalsIgnoreCase(u.getUser_prenom())))
                .count();
        if (duplicates > 0) { content.append("⚠️ ").append(duplicates).append(" doublon(s) détecté(s)\n"); hasSignals = true; }

        if (u.getUser_date_de_naissance() != null && u.getUser_niveau_activite_physique() != null) {
            try {
                LocalDate birthDate = LocalDate.parse(u.getUser_date_de_naissance());
                int age = Period.between(birthDate, LocalDate.now()).getYears();
                String niveauActivite = u.getUser_niveau_activite_physique().name();
                if ((age < 15 && niveauActivite.equals("TRES_INTENSE")) || (age > 70 && niveauActivite.equals("TRES_INTENSE"))) {
                    content.append("⚠️ Activité physique incohérente avec l'âge\n"); hasSignals = true;
                }
            } catch (Exception e) {}
        }
        if (u.getUser_etablissement_scolaire() != null) {
            String etabLower = u.getUser_etablissement_scolaire().toLowerCase();
            if (etabLower.contains("test") || etabLower.contains("aaa") || etabLower.length() <= 2) {
                content.append("⚠️ Établissement suspect : \"").append(u.getUser_etablissement_scolaire()).append("\"\n"); hasSignals = true;
            }
        }

        if (!hasSignals) {
            content.append("✅ Aucun signal majeur détecté\n\nCe compte semble fiable !\n");
        } else {
            content.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━\nRecommandation :\n");
            if (score <= 2) content.append("✅ Compte fiable - Aucune action requise\n");
            else if (score <= 5) content.append("⚠️ Compte moyennement suspect - Surveillance recommandée\n");
            else content.append("🚨 Compte très suspect - Vérification manuelle nécessaire\n");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // ==================== MÉTHODES EXISTANTES ====================

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
        } catch (IOException e) { e.printStackTrace(); }
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void refreshTable() {
        observableList.setAll(service.getAll());
        sortedBySuspicion = false;
        btnSortSuspicion.setText("⚠️ Trier par suspicion");
        btnSortSuspicion.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.3), 10, 0, 0, 3);");
        displayUserCards();
    }

    @FXML
    void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase().trim();
        if (searchTerm.isEmpty()) { displayUserCards(); return; }
        cardsContainer.getChildren().clear();
        for (user u : observableList) {
            if (u.getUser_nom().toLowerCase().contains(searchTerm) ||
                    u.getUser_prenom().toLowerCase().contains(searchTerm) ||
                    u.getUser_email().toLowerCase().contains(searchTerm)) {
                cardsContainer.getChildren().add(createUserCard(u));
            }
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session session = Session.getInstance();
        SessionDAO dao = new SessionDAO();
        if (session.getToken() != null) dao.deleteSession(session.getToken());
        session.clearSession();
        deleteRememberFile();
        switchToLogin(event);
    }

    private void deleteRememberFile() { new File("remember.dat").delete(); }
}
