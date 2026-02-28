package controllers.MeditationControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.MeditationModels.Conseil;
import models.MeditationModels.SessionMeditation;
import services.MeditationServices.ConseilService;
import services.MeditationServices.GroqAIService;
import services.MeditationServices.SessionMeditationService;
import utils.ValidationUtils;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class AdminMeditationController implements Initializable {

    @FXML private VBox formSection;
    @FXML private Label formTitle;
    @FXML private TextField themeField;
    @FXML private TextField auteurField;
    @FXML private TextField dureeField;
    @FXML private TextField audioUrlField;
    @FXML private VBox conseilsContainer;
    @FXML private Button submitBtn;
    @FXML private VBox sessionsContainer;
    @FXML private VBox emptyState;
    @FXML private Label themeError;
    @FXML private Label auteurError;
    @FXML private Label dureeError;
    @FXML private Label audioUrlError;
    @FXML private Label conseilsError;
    @FXML private Button generateAIBtn;
    @FXML private Label aiStatusLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService conseilService = new ConseilService();
    private final GroqAIService groqService = new GroqAIService();

    private SessionMeditation editingSession = null;
    private final List<TextField> conseilFields = new ArrayList<>();
    private List<SessionMeditation> allSessions = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inject stylesheet at Scene level so ComboBox popup dropdown inherits it
        sortCombo.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass().getResource("/views/HarmonieViews/harmonie-style.css").toExternalForm();
                if (!newScene.getStylesheets().contains(css)) newScene.getStylesheets().add(css);
            }
        });
        sortCombo.getItems().addAll(
                "Thème (A-Z)", "Thème (Z-A)",
                "Auteur (A-Z)", "Auteur (Z-A)",
                "Durée (croissant)", "Durée (décroissant)");
        loadSessions();
    }

    // ── Search / Sort ────────────────────────────────────────────────────────

    @FXML private void onSearchChanged() { applySearchAndSort(); }
    @FXML private void onSortChanged()   { applySearchAndSort(); }

    private void applySearchAndSort() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String sort  = sortCombo.getValue();

        List<SessionMeditation> filtered = allSessions.stream()
                .filter(s -> query.isEmpty()
                        || (s.getTheme()  != null && s.getTheme().toLowerCase().contains(query))
                        || (s.getAuteur() != null && s.getAuteur().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Thème (A-Z)"        -> filtered.sort(Comparator.comparing(s -> s.getTheme().toLowerCase()));
            case "Thème (Z-A)"        -> filtered.sort(Comparator.comparing((SessionMeditation s) -> s.getTheme().toLowerCase()).reversed());
            case "Auteur (A-Z)"       -> filtered.sort(Comparator.comparing(s -> s.getAuteur().toLowerCase()));
            case "Auteur (Z-A)"       -> filtered.sort(Comparator.comparing((SessionMeditation s) -> s.getAuteur().toLowerCase()).reversed());
            case "Durée (croissant)"  -> filtered.sort(Comparator.comparingInt(SessionMeditation::getDuree));
            case "Durée (décroissant)"-> filtered.sort(Comparator.comparingInt(SessionMeditation::getDuree).reversed());
        }
        displaySessions(filtered);
    }

    // ── Display ──────────────────────────────────────────────────────────────

    private void displaySessions(List<SessionMeditation> sessions) {
        sessionsContainer.getChildren().clear();
        if (sessions.isEmpty()) {
            emptyState.setVisible(true); emptyState.setManaged(true);
            sessionsContainer.setVisible(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            sessionsContainer.setVisible(true);
            for (SessionMeditation s : sessions) sessionsContainer.getChildren().add(createSessionCard(s));
        }
    }

    private void loadSessions() {
        try {
            allSessions = sessionService.getAll();
            applySearchAndSort();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les sessions: " + e.getMessage());
        }
    }

    private VBox createSessionCard(SessionMeditation session) {
        VBox card = new VBox(10);
        card.getStyleClass().add("harmonie-card");
        card.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label(session.getTheme());
        themeLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label durationLabel = new Label("⏱ " + session.getDureeFormatted());
        durationLabel.setStyle("-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        editBtn.setOnAction(e -> editSession(session));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteSession(session));

        header.getChildren().addAll(themeLabel, durationLabel, spacer, editBtn, deleteBtn);

        Label auteurLabel = new Label("👨‍⚕️ " + session.getAuteur());
        auteurLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");

        String audioText = (session.getAudioUrl() != null && !session.getAudioUrl().isEmpty())
                ? session.getAudioUrl() : "Aucun audio";
        Label audioLabel = new Label("🎵 " + audioText);
        audioLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        audioLabel.setWrapText(true);

        card.getChildren().addAll(header, auteurLabel, audioLabel);

        try {
            List<Conseil> conseils = conseilService.getBySessionId(session.getId());
            Label c = new Label("💡 " + conseils.size() + " conseil(s)");
            c.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 12px;");
            card.getChildren().add(c);
        } catch (Exception ignored) {}

        return card;
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    @FXML public void showAddForm() {
        editingSession = null;
        formTitle.setText("Ajouter une Session");
        submitBtn.setText("Enregistrer");
        clearForm();
        addConseilField();
        formSection.setVisible(true); formSection.setManaged(true);
    }

    @FXML public void hideForm() {
        formSection.setVisible(false); formSection.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        themeField.clear(); auteurField.clear(); dureeField.clear(); audioUrlField.clear();
        conseilsContainer.getChildren().clear(); conseilFields.clear();
        clearErrors();
    }

    private void clearErrors() {
        for (Label l : new Label[]{themeError, auteurError, dureeError, audioUrlError, conseilsError}) {
            l.setVisible(false); l.setManaged(false); l.setText("");
        }
        for (TextField f : new TextField[]{themeField, auteurField, dureeField, audioUrlField})
            f.getStyleClass().remove("field-error");
        for (TextField cf : conseilFields) cf.getStyleClass().remove("field-error");
    }

    private void showFieldError(Label label, String msg) { label.setText(msg); label.setVisible(true); label.setManaged(true); }
    private void setFieldError(TextField field, Label label, String msg) { showFieldError(label, msg); if (!field.getStyleClass().contains("field-error")) field.getStyleClass().add("field-error"); }

    @FXML public void addConseilField() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField tf = new TextField();
        tf.setPromptText("Entrez un conseil...");
        tf.setStyle("-fx-border-color: #D1D5DB; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 10px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> { conseilsContainer.getChildren().remove(row); conseilFields.remove(tf); });

        row.getChildren().addAll(tf, removeBtn);
        conseilsContainer.getChildren().add(row);
        conseilFields.add(tf);
    }

    // ── AI generation ────────────────────────────────────────────────────────

    @FXML public void generateWithAI() {
        String theme = themeField.getText() == null ? "" : themeField.getText().trim();
        if (theme.isEmpty()) { showFieldError(themeError, "Veuillez saisir un thème avant de générer"); return; }

        generateAIBtn.setDisable(true);
        aiStatusLabel.setText("⏳ Génération en cours..."); aiStatusLabel.setVisible(true); aiStatusLabel.setManaged(true);
        aiStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c6daf;");

        new Thread(() -> {
            try {
                String response = groqService.generateMeditationFromTheme(theme);
                Platform.runLater(() -> {
                    parseAndFillFields(response);
                    aiStatusLabel.setText("✅ Généré avec succès ! Vous pouvez modifier les champs.");
                    aiStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
                    generateAIBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    aiStatusLabel.setText("❌ Erreur: " + e.getMessage());
                    aiStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
                    generateAIBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void parseAndFillFields(String response) {
        List<String> conseils = new ArrayList<>();
        for (String line : response.split("\n")) {
            line = line.trim();
            if      (line.startsWith("AUTEUR:")) auteurField.setText(line.substring("AUTEUR:".length()).trim());
            else if (line.startsWith("DUREE:"))  dureeField.setText(line.substring("DUREE:".length()).trim().replaceAll("[^0-9]", ""));
            else if (line.startsWith("AUDIO:"))  audioUrlField.setText(line.substring("AUDIO:".length()).trim());
            else if (line.startsWith("CONSEIL")) { int i = line.indexOf(':'); if (i != -1) conseils.add(line.substring(i + 1).trim()); }
        }
        conseilsContainer.getChildren().clear(); conseilFields.clear();
        for (String c : conseils) { addConseilField(); conseilFields.get(conseilFields.size() - 1).setText(c); }
        if (conseilFields.isEmpty()) addConseilField();
    }

    // ── Save / Edit / Delete ─────────────────────────────────────────────────

    @FXML public void saveSession() {
        clearErrors();
        boolean hasError = false;
        String err;

        err = ValidationUtils.getRequiredFieldError(themeField.getText(), "Le thème");
        if (err != null) { setFieldError(themeField, themeError, err); hasError = true; }
        else { err = ValidationUtils.getLengthError(themeField.getText(), "Le thème", 3, 100); if (err != null) { setFieldError(themeField, themeError, err); hasError = true; } }

        err = ValidationUtils.getRequiredFieldError(auteurField.getText(), "L'auteur");
        if (err != null) { setFieldError(auteurField, auteurError, err); hasError = true; }
        else { err = ValidationUtils.getLengthError(auteurField.getText(), "L'auteur", 2, 100); if (err != null) { setFieldError(auteurField, auteurError, err); hasError = true; } }

        err = ValidationUtils.getPositiveNumberError(dureeField.getText(), "La durée");
        if (err != null) { setFieldError(dureeField, dureeError, err); hasError = true; }

        err = ValidationUtils.getUrlError(audioUrlField.getText());
        if (err != null) { setFieldError(audioUrlField, audioUrlError, err); hasError = true; }

        List<String> conseilContents = new ArrayList<>();
        for (TextField cf : conseilFields) {
            String v = cf.getText() == null ? "" : cf.getText().trim();
            if (!v.isEmpty()) { if (v.length() < 5) { showFieldError(conseilsError, "Chaque conseil doit contenir au moins 5 caractères"); hasError = true; } else conseilContents.add(v); }
        }

        if (hasError) return;

        try {
            int duree = Integer.parseInt(dureeField.getText().trim());
            if (editingSession == null) {
                SessionMeditation session = new SessionMeditation(1, auteurField.getText().trim(), duree, themeField.getText().trim(), audioUrlField.getText().trim());
                sessionService.add(session);
                for (String c : conseilContents) conseilService.add(new Conseil(session.getId(), c));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Session créée avec succès !");
            } else {
                editingSession.setTheme(themeField.getText().trim());
                editingSession.setAuteur(auteurField.getText().trim());
                editingSession.setDuree(duree);
                editingSession.setAudioUrl(audioUrlField.getText().trim());
                sessionService.update(editingSession);
                conseilService.deleteBySessionId(editingSession.getId());
                for (String c : conseilContents) conseilService.add(new Conseil(editingSession.getId(), c));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Session modifiée avec succès !");
            }
            hideForm();
            loadSessions();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder: " + e.getMessage());
        }
    }

    private void editSession(SessionMeditation session) {
        editingSession = session;
        formTitle.setText("Modifier la Session");
        submitBtn.setText("Mettre à jour");
        themeField.setText(session.getTheme());
        auteurField.setText(session.getAuteur());
        dureeField.setText(String.valueOf(session.getDuree()));
        audioUrlField.setText(session.getAudioUrl() != null ? session.getAudioUrl() : "");
        conseilsContainer.getChildren().clear(); conseilFields.clear();
        try {
            for (Conseil c : conseilService.getBySessionId(session.getId())) {
                addConseilField(); conseilFields.get(conseilFields.size() - 1).setText(c.getContenu());
            }
        } catch (Exception ignored) {}
        if (conseilFields.isEmpty()) addConseilField();
        formSection.setVisible(true); formSection.setManaged(true);
    }

    private void deleteSession(SessionMeditation session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la session \"" + session.getTheme() + "\" et tous ses conseils ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                conseilService.deleteBySessionId(session.getId());
                sessionService.delete(session.getId());
                loadSessions();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }
}
