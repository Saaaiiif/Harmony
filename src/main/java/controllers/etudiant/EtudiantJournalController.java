package controllers.etudiant;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Humeur;
import models.JournalHumeur;
import services.JournalHumeurService;
import utils.ValidationUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EtudiantJournalController implements Initializable {

    @FXML
    private VBox formSection;

    @FXML
    private Label formTitle;

    @FXML
    private ComboBox<Humeur> humeurCombo;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextArea contenuArea;

    @FXML
    private Button submitBtn;

    @FXML
    private Label monthEntriesLabel;

    @FXML
    private Label avgHumeurLabel;

    @FXML
    private HBox avgHumeurContainer;

    @FXML
    private Label lastEntryLabel;

    @FXML
    private VBox journalContainer;

    @FXML
    private VBox emptyState;

    @FXML
    private Label humeurError;

    @FXML
    private Label dateError;

    @FXML
    private Label contenuError;

    private final JournalHumeurService journalService = new JournalHumeurService();

    private JournalHumeur editingJournal = null;
    private List<JournalHumeur> myJournals;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int ETUDIANT_USER_ID = EtudiantDashboardController.ETUDIANT_USER_ID;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHumeurCombo();
        datePicker.setValue(LocalDate.now());
        loadJournals();
    }

    private void setupHumeurCombo() {
        humeurCombo.getItems().addAll(Humeur.values());
        
        humeurCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Humeur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String emoji = getHumeurEmoji(item);
                    setText(emoji + " " + item.getLabel());
                }
            }
        });

        humeurCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Humeur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String emoji = getHumeurEmoji(item);
                    setText(emoji + " " + item.getLabel());
                }
            }
        });
    }

    private String getHumeurEmoji(Humeur humeur) {
        return switch (humeur) {
            case TRES_BIEN -> "😄";
            case BIEN -> "🙂";
            case NEUTRE -> "😐";
            case MAL -> "😔";
            case TRES_MAL -> "😢";
        };
    }

    private void loadJournals() {
        try {
            myJournals = journalService.getByUserId(ETUDIANT_USER_ID);
            displayJournals();
            updateStats();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos entrées: " + e.getMessage());
        }
    }

    private void displayJournals() {
        journalContainer.getChildren().clear();

        if (myJournals.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            journalContainer.setVisible(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            journalContainer.setVisible(true);

            for (JournalHumeur journal : myJournals) {
                journalContainer.getChildren().add(createJournalCard(journal));
            }
        }
    }

    private VBox createJournalCard(JournalHumeur journal) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));

        String cardStyle = getHumeurCardStyle(journal.getHumeur());
        card.getStyleClass().addAll("journal-card", cardStyle);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(journal.getDate() != null ? journal.getDate().format(dateFormatter) : "Aujourd'hui");
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String emoji = getHumeurEmoji(journal.getHumeur());
        Label humeurBadge = new Label(emoji + " " + journal.getHumeur().getLabel());
        humeurBadge.getStyleClass().addAll("humeur-badge", getHumeurBadgeStyle(journal.getHumeur()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️");
        editBtn.getStyleClass().add("button");
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
        contenuLabel.getStyleClass().add("label");
        contenuLabel.setStyle("-fx-font-size: 13px;");

        card.getChildren().addAll(header, contenuLabel);

        return card;
    }

    private void updateStats() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        long monthCount = myJournals.stream()
                .filter(j -> j.getDate() != null && !j.getDate().isBefore(startOfMonth))
                .count();
        monthEntriesLabel.setText(String.valueOf(monthCount));

        if (!myJournals.isEmpty()) {
            double avg = myJournals.stream()
                    .mapToInt(JournalHumeur::getScore)
                    .average()
                    .orElse(0);
            Humeur avgHumeur = Humeur.fromScore((int) Math.round(avg));
            avgHumeurLabel.setText(getHumeurEmoji(avgHumeur) + " " + avgHumeur.getLabel());
        } else {
            avgHumeurLabel.setText("--");
        }

        if (!myJournals.isEmpty() && myJournals.get(0).getDate() != null) {
            lastEntryLabel.setText(myJournals.get(0).getDate().format(dateFormatter));
        } else {
            lastEntryLabel.setText("--");
        }
    }

    @FXML
    public void showAddForm() {
        editingJournal = null;
        formTitle.setText("Comment vous sentez-vous ?");
        submitBtn.setText("Enregistrer");
        clearForm();
        datePicker.setValue(LocalDate.now());
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
        humeurCombo.setValue(null);
        humeurCombo.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now());
        contenuArea.clear();
        clearErrors();
    }

    private void clearErrors() {
        Label[] errorLabels = {humeurError, dateError, contenuError};
        for (Label lbl : errorLabels) {
            lbl.setVisible(false);
            lbl.setManaged(false);
            lbl.setText("");
        }
        humeurCombo.getStyleClass().remove("field-error");
        datePicker.getStyleClass().remove("field-error");
        contenuArea.getStyleClass().remove("field-error");
    }

    private void showFieldError(Label errorLabel, String message, Control field) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
    }

    @FXML
    public void saveJournal() {
        clearErrors();
        boolean hasError = false;

        if (humeurCombo.getValue() == null && humeurCombo.getSelectionModel().getSelectedItem() == null) {
            showFieldError(humeurError, "Veuillez sélectionner une humeur", humeurCombo);
            hasError = true;
        }

        if (datePicker.getValue() == null) {
            showFieldError(dateError, "Veuillez sélectionner une date", datePicker);
            hasError = true;
        }

        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();
        if (contenu.isEmpty()) {
            showFieldError(contenuError, "Le contenu est obligatoire", contenuArea);
            hasError = true;
        } else if (contenu.length() > 1000) {
            showFieldError(contenuError, "Le contenu ne doit pas dépasser 1000 caractères", contenuArea);
            hasError = true;
        }

        if (hasError) {
            return;
        }

        try {
            Humeur selectedHumeur = humeurCombo.getValue() != null 
                    ? humeurCombo.getValue() 
                    : humeurCombo.getSelectionModel().getSelectedItem();
            
            if (editingJournal == null) {
                JournalHumeur journal = new JournalHumeur(
                        ETUDIANT_USER_ID,
                        datePicker.getValue(),
                        selectedHumeur,
                        selectedHumeur.getScore(),
                        contenu
                );
                journalService.add(journal);
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder: " + e.getMessage());
        }
    }

    private void editJournal(JournalHumeur journal) {
        editingJournal = journal;
        formTitle.setText("Modifier votre entrée");
        submitBtn.setText("Mettre à jour");

        humeurCombo.setValue(journal.getHumeur());
        datePicker.setValue(journal.getDate());
        contenuArea.setText(journal.getContenu());

        formSection.setVisible(true);
        formSection.setManaged(true);
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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Entrée supprimée!");
                loadJournals();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    private String getHumeurCardStyle(Humeur humeur) {
        return switch (humeur) {
            case TRES_BIEN, BIEN -> "journal-card-good";
            case NEUTRE -> "journal-card-neutral";
            case MAL, TRES_MAL -> "journal-card-bad";
        };
    }

    private String getHumeurBadgeStyle(Humeur humeur) {
        return switch (humeur) {
            case TRES_BIEN -> "humeur-tres-bien";
            case BIEN -> "humeur-bien";
            case NEUTRE -> "humeur-neutre";
            case MAL -> "humeur-mal";
            case TRES_MAL -> "humeur-tres-mal";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
