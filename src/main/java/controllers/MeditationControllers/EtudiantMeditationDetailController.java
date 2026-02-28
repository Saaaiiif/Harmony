package controllers.MeditationControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.MeditationModels.Conseil;
import models.MeditationModels.SessionMeditation;
import services.MeditationServices.ConseilService;
import services.MeditationServices.GroqAIService;
import services.MeditationServices.SessionMeditationService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantMeditationDetailController implements Initializable {

    @FXML private Label themeLabel;
    @FXML private Label durationLabel;
    @FXML private Label auteurLabel;
    @FXML private VBox  audioSection;
    @FXML private Button playButton;
    @FXML private Label audioUrlLabel;
    @FXML private VBox  conseilsContainer;
    @FXML private VBox  noConseilsState;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService           conseilService = new ConseilService();
    private final GroqAIService            groqService    = new GroqAIService();

    private AccueilController accueilController;
    private SessionMeditation currentSession;
    private int               sessionId;

    /** Called by AccueilController.showMeditationDetail() */
    public void setAccueilController(AccueilController ctrl) {
        this.accueilController = ctrl;
    }

    public void setSessionId(int id) {
        this.sessionId = id;
        loadSessionDetails();
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}

    private void loadSessionDetails() {
        try {
            currentSession = sessionService.getById(sessionId);
            if (currentSession != null) {
                displaySessionDetails();
                loadConseils();
            }
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les détails: " + e.getMessage());
        }
    }

    private void displaySessionDetails() {
        themeLabel.setText(currentSession.getTheme());
        durationLabel.setText(currentSession.getDureeFormatted());
        auteurLabel.setText(currentSession.getAuteur());

        String audioUrl = currentSession.getAudioUrl();
        boolean hasAudio = audioUrl != null && !audioUrl.trim().isEmpty();
        audioSection.setVisible(hasAudio);
        audioSection.setManaged(hasAudio);
        if (hasAudio) audioUrlLabel.setText(audioUrl);
    }

    private void loadConseils() {
        try {
            List<Conseil> conseils = conseilService.getBySessionId(sessionId);
            conseilsContainer.getChildren().clear();

            if (conseils.isEmpty()) {
                noConseilsState.setVisible(true);  noConseilsState.setManaged(true);
                conseilsContainer.setVisible(false);
            } else {
                noConseilsState.setVisible(false); noConseilsState.setManaged(false);
                conseilsContainer.setVisible(true);
                int index = 1;
                for (Conseil c : conseils) conseilsContainer.getChildren().add(createConseilItem(c, index++));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox createConseilItem(Conseil conseil, int index) {
        HBox item = new HBox(15);
        item.getStyleClass().add("conseil-item");
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(15));

        Label numberLabel = new Label(String.valueOf(index));
        numberLabel.getStyleClass().add("conseil-number");
        numberLabel.setMinWidth(30); numberLabel.setMinHeight(30);
        numberLabel.setAlignment(Pos.CENTER);

        Label contentLabel = new Label(conseil.getContenu());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px;");
        HBox.setHgrow(contentLabel, Priority.ALWAYS);

        Button speakBtn = new Button("🔊 Lire");
        speakBtn.setStyle("-fx-font-size: 13px; -fx-background-color: #7c6daf; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
        speakBtn.setOnAction(e -> {
            if (groqService.isSpeaking()) {
                groqService.stopSpeaking();
                speakBtn.setText("🔊 Lire");
                speakBtn.setStyle("-fx-font-size: 13px; -fx-background-color: #7c6daf; -fx-text-fill: white; "
                        + "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
            } else {
                speakBtn.setText("⏹ Arrêter");
                speakBtn.setStyle("-fx-font-size: 13px; -fx-background-color: #e74c3c; -fx-text-fill: white; "
                        + "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
                groqService.speakText(conseil.getContenu(), () -> {
                    speakBtn.setText("🔊 Lire");
                    speakBtn.setStyle("-fx-font-size: 13px; -fx-background-color: #7c6daf; -fx-text-fill: white; "
                            + "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
                });
            }
        });

        item.getChildren().addAll(numberLabel, contentLabel, speakBtn);
        return item;
    }

    @FXML
    public void playAudio() {
        if (currentSession == null || currentSession.getAudioUrl() == null
                || currentSession.getAudioUrl().trim().isEmpty()) return;
        try {
            String url = currentSession.getAudioUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le lien audio: " + e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        if (accueilController != null) {
            accueilController.loadMeditationPage();
        }
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
