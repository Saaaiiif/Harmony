package controllers.MeditationControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.MeditationModels.Humeur;
import models.MeditationModels.JournalHumeur;
import models.UserModels.Session;
import services.MeditationServices.GroqAIService;
import services.MeditationServices.JournalHumeurService;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class EtudiantJournalController implements Initializable {

    @FXML private VBox      formSection;
    @FXML private Label     formTitle;
    @FXML private ComboBox<Humeur> humeurCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextArea  contenuArea;
    @FXML private Button    submitBtn;
    @FXML private Label     monthEntriesLabel;
    @FXML private Label     avgHumeurLabel;
    @FXML private HBox      avgHumeurContainer;
    @FXML private Label     lastEntryLabel;
    @FXML private VBox      journalContainer;
    @FXML private VBox      emptyState;
    @FXML private Label     humeurError;
    @FXML private Label     dateError;
    @FXML private Label     contenuError;
    @FXML private Button    voiceBtn;
    @FXML private Label     voiceStatusLabel;

    private final JournalHumeurService journalService = new JournalHumeurService();
    private final GroqAIService         groqService    = new GroqAIService();

    private JournalHumeur       editingJournal = null;
    private List<JournalHumeur> myJournals;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private boolean           isRecording = false;
    private TargetDataLine    targetLine;
    private ByteArrayOutputStream audioBytes;

    private AccueilController accueilController;

    public void setAccueilController(AccueilController ctrl) {
        this.accueilController = ctrl;
    }

    private int getCurrentUserId() {
        return Session.getInstance().getUser().getUser_id();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        humeurCombo.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass().getResource("/views/HarmonieViews/harmonie-style.css").toExternalForm();
                if (!newScene.getStylesheets().contains(css)) newScene.getStylesheets().add(css);
            }
        });
        setupHumeurCombo();
        datePicker.setValue(LocalDate.now());
        loadJournals();
    }


    private void setupHumeurCombo() {
        humeurCombo.getItems().addAll(Humeur.values());
        humeurCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Humeur item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : getHumeurEmoji(item) + " " + item.getLabel());
            }
        });
        humeurCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Humeur item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : getHumeurEmoji(item) + " " + item.getLabel());
            }
        });
    }

    private String getHumeurEmoji(Humeur h) {
        return switch (h) {
            case TRES_BIEN -> "😄";
            case BIEN      -> "🙂";
            case NEUTRE    -> "😐";
            case MAL       -> "😔";
            case TRES_MAL  -> "😢";
        };
    }


    private void loadJournals() {
        try {
            myJournals = journalService.getByUserId(getCurrentUserId());
            displayJournals();
            updateStats();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos entrées: " + e.getMessage());
        }
    }

    private void displayJournals() {
        journalContainer.getChildren().clear();
        if (myJournals.isEmpty()) {
            emptyState.setVisible(true);  emptyState.setManaged(true);
            journalContainer.setVisible(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            journalContainer.setVisible(true);
            for (JournalHumeur j : myJournals) journalContainer.getChildren().add(createJournalCard(j));
        }
    }

    private VBox createJournalCard(JournalHumeur journal) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.getStyleClass().addAll("journal-card", getHumeurCardStyle(journal.getHumeur()));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(journal.getDate() != null ? journal.getDate().format(dateFormatter) : "Aujourd'hui");
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label humeurBadge = new Label(getHumeurEmoji(journal.getHumeur()) + " " + journal.getHumeur().getLabel());
        humeurBadge.getStyleClass().addAll("humeur-badge", getHumeurBadgeStyle(journal.getHumeur()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;");
        editBtn.setOnAction(e -> editJournal(journal));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;");
        deleteBtn.setOnAction(e -> deleteJournal(journal));

        header.getChildren().addAll(dateLabel, humeurBadge, spacer, editBtn, deleteBtn);

        Label contenuLabel = new Label(journal.getContenu() != null && !journal.getContenu().isEmpty()
                ? journal.getContenu() : "Aucune note");
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-font-size: 13px;");

        card.getChildren().addAll(header, contenuLabel);
        return card;
    }

    private void updateStats() {
        LocalDate now         = LocalDate.now();
        LocalDate startMonth  = now.with(TemporalAdjusters.firstDayOfMonth());
        long monthCount = myJournals.stream()
                .filter(j -> j.getDate() != null && !j.getDate().isBefore(startMonth))
                .count();
        monthEntriesLabel.setText(String.valueOf(monthCount));

        if (!myJournals.isEmpty()) {
            double avg     = myJournals.stream().mapToInt(JournalHumeur::getScore).average().orElse(0);
            Humeur avgH    = Humeur.fromScore((int) Math.round(avg));
            avgHumeurLabel.setText(getHumeurEmoji(avgH) + " " + avgH.getLabel());
        } else {
            avgHumeurLabel.setText("--");
        }

        if (!myJournals.isEmpty() && myJournals.get(0).getDate() != null) {
            lastEntryLabel.setText(myJournals.get(0).getDate().format(dateFormatter));
        } else {
            lastEntryLabel.setText("--");
        }
    }


    @FXML public void showAddForm() {
        editingJournal = null;
        formTitle.setText("Comment vous sentez-vous ?");
        submitBtn.setText("Enregistrer");
        clearForm();
        datePicker.setValue(LocalDate.now());
        formSection.setVisible(true); formSection.setManaged(true);
    }

    @FXML public void hideForm() {
        formSection.setVisible(false); formSection.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        humeurCombo.setValue(null);
        humeurCombo.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now());
        contenuArea.clear();
        clearErrors();
    }

    private void clearErrors() {
        for (Label l : new Label[]{humeurError, dateError, contenuError}) {
            l.setVisible(false); l.setManaged(false); l.setText("");
        }
        humeurCombo.getStyleClass().remove("field-error");
        datePicker.getStyleClass().remove("field-error");
        contenuArea.getStyleClass().remove("field-error");
    }

    private void showFieldError(Label errLabel, String msg, javafx.scene.control.Control field) {
        errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true);
        if (!field.getStyleClass().contains("field-error")) field.getStyleClass().add("field-error");
    }

    @FXML public void saveJournal() {
        clearErrors();
        boolean err = false;

        Humeur selectedHumeur = humeurCombo.getValue() != null
                ? humeurCombo.getValue()
                : humeurCombo.getSelectionModel().getSelectedItem();

        if (selectedHumeur == null) {
            showFieldError(humeurError, "Veuillez sélectionner une humeur", humeurCombo);
            err = true;
        }
        if (datePicker.getValue() == null) {
            showFieldError(dateError, "Veuillez sélectionner une date", datePicker);
            err = true;
        }
        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();
        if (contenu.isEmpty()) {
            showFieldError(contenuError, "Le contenu est obligatoire", contenuArea);
            err = true;
        } else if (contenu.length() > 1000) {
            showFieldError(contenuError, "Le contenu ne doit pas dépasser 1000 caractères", contenuArea);
            err = true;
        }
        if (err) return;

        try {
            if (editingJournal == null) {
                journalService.add(new JournalHumeur(getCurrentUserId(), datePicker.getValue(),
                        selectedHumeur, selectedHumeur.getScore(), contenu));
            } else {
                editingJournal.setDate(datePicker.getValue());
                editingJournal.setHumeur(selectedHumeur);
                editingJournal.setScore(selectedHumeur.getScore());
                editingJournal.setContenu(contenu);
                journalService.update(editingJournal);
            }
            hideForm();
            loadJournals();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder: " + e.getMessage());
        }
    }

    private void editJournal(JournalHumeur journal) {
        editingJournal = journal;
        formTitle.setText("Modifier votre entrée");
        submitBtn.setText("Mettre à jour");
        humeurCombo.setValue(journal.getHumeur());
        datePicker.setValue(journal.getDate());
        contenuArea.setText(journal.getContenu());
        formSection.setVisible(true); formSection.setManaged(true);
    }

    private void deleteJournal(JournalHumeur journal) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette entrée ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                journalService.delete(journal.getId());
                alert(Alert.AlertType.INFORMATION, "Succès", "Entrée supprimée !");
                loadJournals();
            } catch (Exception e) {
                alert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }


    private String getHumeurCardStyle(Humeur h) {
        return switch (h) {
            case TRES_BIEN, BIEN -> "journal-card-good";
            case NEUTRE          -> "journal-card-neutral";
            case MAL, TRES_MAL   -> "journal-card-bad";
        };
    }

    private String getHumeurBadgeStyle(Humeur h) {
        return switch (h) {
            case TRES_BIEN -> "humeur-tres-bien";
            case BIEN      -> "humeur-bien";
            case NEUTRE    -> "humeur-neutre";
            case MAL       -> "humeur-mal";
            case TRES_MAL  -> "humeur-tres-mal";
        };
    }


    @FXML public void toggleVoiceRecording() {
        if (!isRecording) startRecording();
        else stopRecordingAndProcess();
    }

    private void startRecording() {
        try {
            AudioFormat format  = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                alert(Alert.AlertType.ERROR, "Erreur", "Microphone non disponible.");
                return;
            }
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
            audioBytes  = new ByteArrayOutputStream();
            isRecording = true;

            voiceBtn.setText("🔴 Arrêter");
            voiceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; "
                    + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            voiceStatusLabel.setText("🎙️ Enregistrement en cours... Parlez maintenant");
            voiceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c;");
            voiceStatusLabel.setVisible(true); voiceStatusLabel.setManaged(true);

            new Thread(() -> {
                byte[] buf = new byte[4096];
                while (isRecording) {
                    int n = targetLine.read(buf, 0, buf.length);
                    if (n > 0) audioBytes.write(buf, 0, n);
                }
            }).start();

        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'accéder au microphone: " + e.getMessage());
        }
    }

    private void stopRecordingAndProcess() {
        isRecording = false;
        if (targetLine != null) { targetLine.stop(); targetLine.close(); }

        voiceBtn.setText("🎤 Dicter une entrée");
        voiceBtn.setStyle("-fx-background-color: #7c6daf; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        voiceStatusLabel.setText("⏳ Traitement de votre voix...");
        voiceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7c6daf;");
        voiceBtn.setDisable(true);

        new Thread(() -> {
            try {
                byte[]      audioData = audioBytes.toByteArray();
                AudioFormat format    = new AudioFormat(16000, 16, 1, true, false);

                File tempFile = File.createTempFile("harmonie_voice_", ".wav");
                tempFile.deleteOnExit();
                try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData)) {
                    AudioInputStream ais = new AudioInputStream(bais, format,
                            audioData.length / format.getFrameSize());
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempFile);
                }

                Platform.runLater(() -> voiceStatusLabel.setText("⏳ Transcription en cours..."));
                String transcription = groqService.transcribeAudio(tempFile);

                Platform.runLater(() -> voiceStatusLabel.setText("⏳ Analyse du contenu..."));
                String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String parsed = groqService.parseJournalFromSpeech(transcription, currentDate);

                String exDate    = currentDate;
                String exHumeur  = "NEUTRE";
                String exContenu = transcription;

                for (String line : parsed.split("\n")) {
                    line = line.trim();
                    if      (line.startsWith("DATE:"))    exDate    = line.substring(5).trim();
                    else if (line.startsWith("HUMEUR:"))  exHumeur  = line.substring(7).trim();
                    else if (line.startsWith("CONTENU:")) exContenu = line.substring(8).trim();
                }

                final String fDate = exDate, fHumeur = exHumeur, fContenu = exContenu;

                Platform.runLater(() -> {
                    try {
                        LocalDate date;
                        try { date = LocalDate.parse(fDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
                        catch (DateTimeParseException ex) { date = LocalDate.now(); }

                        Humeur humeur = Humeur.fromString(fHumeur);
                        journalService.add(new JournalHumeur(getCurrentUserId(), date,
                                humeur, humeur.getScore(), fContenu));

                        voiceStatusLabel.setText("✅ Entrée ajoutée ! Date: "
                                + date.format(dateFormatter) + " | Humeur: " + humeur.getLabel());
                        voiceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #27ae60;");
                        voiceBtn.setDisable(false);
                        loadJournals();
                    } catch (Exception ex) {
                        voiceStatusLabel.setText("❌ Erreur: " + ex.getMessage());
                        voiceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c;");
                        voiceBtn.setDisable(false);
                    }
                });
                tempFile.delete();

            } catch (Exception e) {
                Platform.runLater(() -> {
                    voiceStatusLabel.setText("❌ Erreur: " + e.getMessage());
                    voiceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c;");
                    voiceBtn.setDisable(false);
                });
            }
        }).start();
    }


    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
