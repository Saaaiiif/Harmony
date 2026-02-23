package controllers.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Conseil;
import models.SessionMeditation;
import services.ConseilService;
import services.GroqAIService;
import services.SessionMeditationService;
import utils.ValidationUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminMeditationController implements Initializable {

    @FXML
    private VBox formSection;

    @FXML
    private Label formTitle;

    @FXML
    private TextField themeField;

    @FXML
    private TextField auteurField;

    @FXML
    private TextField dureeField;

    @FXML
    private TextField audioUrlField;

    @FXML
    private VBox conseilsContainer;

    @FXML
    private Button submitBtn;

    @FXML
    private VBox sessionsContainer;

    @FXML
    private VBox emptyState;

    @FXML
    private Label themeError;

    @FXML
    private Label auteurError;

    @FXML
    private Label dureeError;

    @FXML
    private Label audioUrlError;

    @FXML
    private Label conseilsError;

    @FXML
    private Button generateAIBtn;

    @FXML
    private Label aiStatusLabel;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService conseilService = new ConseilService();
    private final GroqAIService groqService = new GroqAIService();

    private SessionMeditation editingSession = null;
    private List<TextField> conseilFields = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSessions();
    }

    private void loadSessions() {
        try {
            List<SessionMeditation> sessions = sessionService.getAll();
            sessionsContainer.getChildren().clear();

            if (sessions.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                sessionsContainer.setVisible(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                sessionsContainer.setVisible(true);

                for (SessionMeditation session : sessions) {
                    sessionsContainer.getChildren().add(createSessionCard(session));
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les sessions: " + e.getMessage());
        }
    }

    private VBox createSessionCard(SessionMeditation session) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label(session.getTheme());
        themeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label durationLabel = new Label(session.getDureeFormatted());
        durationLabel.getStyleClass().add("duration-tag");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("button");
        editBtn.setOnAction(e -> editSession(session));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteSession(session));

        header.getChildren().addAll(themeLabel, durationLabel, spacer, editBtn, deleteBtn);

        Label auteurLabel = new Label("Par: " + session.getAuteur());
        auteurLabel.getStyleClass().add("subtitle-label");

        HBox audioBox = new HBox(5);
        audioBox.setAlignment(Pos.CENTER_LEFT);
        Label audioIcon = new Label("🎵");
        Label audioLabel = new Label(session.getAudioUrl() != null && !session.getAudioUrl().isEmpty() 
                ? session.getAudioUrl() : "Aucun audio");
        audioLabel.getStyleClass().add("subtitle-label");
        audioLabel.setWrapText(true);
        audioBox.getChildren().addAll(audioIcon, audioLabel);

        try {
            List<Conseil> conseils = conseilService.getBySessionId(session.getId());
            Label conseilsLabel = new Label("💡 " + conseils.size() + " conseil(s)");
            conseilsLabel.getStyleClass().add("theme-tag");
            card.getChildren().addAll(header, auteurLabel, audioBox, conseilsLabel);
        } catch (Exception e) {
            card.getChildren().addAll(header, auteurLabel, audioBox);
        }

        return card;
    }

    @FXML
    public void showAddForm() {
        editingSession = null;
        formTitle.setText("Ajouter une Session");
        submitBtn.setText("Enregistrer");
        clearForm();
        addConseilField();
        formSection.setVisible(true);
        formSection.setManaged(true);
    }

    @FXML
    public void hideForm() {
        formSection.setVisible(false);
        formSection.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        themeField.clear();
        auteurField.clear();
        dureeField.clear();
        audioUrlField.clear();
        conseilsContainer.getChildren().clear();
        conseilFields.clear();
        clearErrors();
    }

    private void clearErrors() {
        Label[] errorLabels = {themeError, auteurError, dureeError, audioUrlError, conseilsError};
        for (Label lbl : errorLabels) {
            lbl.setVisible(false);
            lbl.setManaged(false);
            lbl.setText("");
        }
        themeField.getStyleClass().remove("field-error");
        auteurField.getStyleClass().remove("field-error");
        dureeField.getStyleClass().remove("field-error");
        audioUrlField.getStyleClass().remove("field-error");
        for (TextField cf : conseilFields) {
            cf.getStyleClass().remove("field-error");
        }
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void setFieldError(TextField field, Label errorLabel, String message) {
        showFieldError(errorLabel, message);
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
    }

    @FXML
    public void addConseilField() {
        HBox conseilRow = new HBox(10);
        conseilRow.setAlignment(Pos.CENTER_LEFT);

        TextField conseilField = new TextField();
        conseilField.setPromptText("Entrez un conseil...");
        conseilField.getStyleClass().add("text-field");
        HBox.setHgrow(conseilField, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5 10;");
        removeBtn.setOnAction(e -> {
            conseilsContainer.getChildren().remove(conseilRow);
            conseilFields.remove(conseilField);
        });

        conseilRow.getChildren().addAll(conseilField, removeBtn);
        conseilsContainer.getChildren().add(conseilRow);
        conseilFields.add(conseilField);
    }

    @FXML
    public void generateWithAI() {
        String theme = themeField.getText() == null ? "" : themeField.getText().trim();
        if (theme.isEmpty()) {
            showFieldError(themeError, "Veuillez saisir un thème avant de générer");
            return;
        }

        generateAIBtn.setDisable(true);
        aiStatusLabel.setText("⏳ Génération en cours...");
        aiStatusLabel.setVisible(true);
        aiStatusLabel.setManaged(true);
        aiStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c6daf;");

        new Thread(() -> {
            try {
                String response = groqService.generateMeditationFromTheme(theme);
                Platform.runLater(() -> {
                    parseAndFillMeditationFields(response);
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
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndFillMeditationFields(String response) {
        String[] lines = response.split("\n");
        List<String> conseils = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("AUTEUR:")) {
                auteurField.setText(line.substring("AUTEUR:".length()).trim());
            } else if (line.startsWith("DUREE:")) {
                String dureeStr = line.substring("DUREE:".length()).trim().replaceAll("[^0-9]", "");
                dureeField.setText(dureeStr);
            } else if (line.startsWith("AUDIO:")) {
                audioUrlField.setText(line.substring("AUDIO:".length()).trim());
            } else if (line.startsWith("CONSEIL")) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    conseils.add(line.substring(colonIdx + 1).trim());
                }
            }
        }

        conseilsContainer.getChildren().clear();
        conseilFields.clear();
        for (String conseil : conseils) {
            addConseilField();
            conseilFields.get(conseilFields.size() - 1).setText(conseil);
        }
        if (conseilFields.isEmpty()) {
            addConseilField();
        }
    }

    @FXML
    public void saveSession() {
        clearErrors();
        boolean hasError = false;
        String error;

        // Validate theme
        error = ValidationUtils.getRequiredFieldError(themeField.getText(), "Le thème");
        if (error != null) {
            setFieldError(themeField, themeError, error);
            hasError = true;
        } else {
            error = ValidationUtils.getLengthError(themeField.getText(), "Le thème", 3, 100);
            if (error != null) {
                setFieldError(themeField, themeError, error);
                hasError = true;
            }
        }

        // Validate auteur
        error = ValidationUtils.getRequiredFieldError(auteurField.getText(), "L'auteur");
        if (error != null) {
            setFieldError(auteurField, auteurError, error);
            hasError = true;
        } else {
            error = ValidationUtils.getLengthError(auteurField.getText(), "L'auteur", 2, 100);
            if (error != null) {
                setFieldError(auteurField, auteurError, error);
                hasError = true;
            }
        }

        // Validate duree
        error = ValidationUtils.getPositiveNumberError(dureeField.getText(), "La durée");
        if (error != null) {
            setFieldError(dureeField, dureeError, error);
            hasError = true;
        } else {
            int dureeVal = Integer.parseInt(dureeField.getText().trim());
            if (dureeVal > 180) {
                setFieldError(dureeField, dureeError, "La durée ne peut pas dépasser 180 minutes");
                hasError = true;
            }
        }

        // Validate audio URL (required)
        error = ValidationUtils.getRequiredFieldError(audioUrlField.getText(), "L'URL audio");
        if (error != null) {
            setFieldError(audioUrlField, audioUrlError, error);
            hasError = true;
        } else {
            error = ValidationUtils.getUrlError(audioUrlField.getText());
            if (error != null) {
                setFieldError(audioUrlField, audioUrlError, error);
                hasError = true;
            }
        }

        // Validate conseils
        boolean hasValidConseil = false;
        for (TextField field : conseilFields) {
            String conseil = field.getText().trim();
            if (!conseil.isEmpty()) {
                if (conseil.length() < 10) {
                    if (!field.getStyleClass().contains("field-error")) {
                        field.getStyleClass().add("field-error");
                    }
                    showFieldError(conseilsError, "Chaque conseil doit contenir au moins 10 caractères");
                    hasError = true;
                } else if (conseil.length() > 500) {
                    if (!field.getStyleClass().contains("field-error")) {
                        field.getStyleClass().add("field-error");
                    }
                    showFieldError(conseilsError, "Chaque conseil ne doit pas dépasser 500 caractères");
                    hasError = true;
                } else {
                    hasValidConseil = true;
                }
            }
        }
        if (!hasValidConseil && !hasError) {
            showFieldError(conseilsError, "Veuillez ajouter au moins un conseil");
            hasError = true;
        } else if (!hasValidConseil && !conseilsError.isVisible()) {
            showFieldError(conseilsError, "Veuillez ajouter au moins un conseil");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        int duree = Integer.parseInt(dureeField.getText().trim());

        try {
            if (editingSession == null) {
                SessionMeditation session = new SessionMeditation(
                        AdminDashboardController.ADMIN_USER_ID,
                        auteurField.getText().trim(),
                        duree,
                        themeField.getText().trim(),
                        audioUrlField.getText().trim()
                );
                sessionService.add(session);

                for (TextField field : conseilFields) {
                    if (!field.getText().trim().isEmpty()) {
                        Conseil conseil = new Conseil(session.getId(), field.getText().trim());
                        conseilService.add(conseil);
                    }
                }
            } else {
                editingSession.setTheme(themeField.getText().trim());
                editingSession.setAuteur(auteurField.getText().trim());
                editingSession.setDuree(duree);
                editingSession.setAudioUrl(audioUrlField.getText().trim());
                sessionService.update(editingSession);

                conseilService.deleteBySessionId(editingSession.getId());
                for (TextField field : conseilFields) {
                    if (!field.getText().trim().isEmpty()) {
                        Conseil conseil = new Conseil(editingSession.getId(), field.getText().trim());
                        conseilService.add(conseil);
                    }
                }
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
        audioUrlField.setText(session.getAudioUrl());

        conseilsContainer.getChildren().clear();
        conseilFields.clear();

        try {
            List<Conseil> conseils = conseilService.getBySessionId(session.getId());
            for (Conseil conseil : conseils) {
                addConseilField();
                conseilFields.get(conseilFields.size() - 1).setText(conseil.getContenu());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (conseilFields.isEmpty()) {
            addConseilField();
        }

        formSection.setVisible(true);
        formSection.setManaged(true);
    }

    private void deleteSession(SessionMeditation session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la session");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette session et tous ses conseils ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sessionService.delete(session.getId());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Session supprimée!");
                loadSessions();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
