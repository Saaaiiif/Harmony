package controllers.etudiant;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Conseil;
import models.SessionMeditation;
import services.ConseilService;
import services.SessionMeditationService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantMeditationDetailController implements Initializable {

    @FXML
    private Label themeLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Label auteurLabel;

    @FXML
    private VBox audioSection;

    @FXML
    private Button playButton;

    @FXML
    private Label audioUrlLabel;

    @FXML
    private VBox conseilsContainer;

    @FXML
    private VBox noConseilsState;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService conseilService = new ConseilService();

    private EtudiantDashboardController dashboardController;
    private SessionMeditation currentSession;
    private int sessionId;

    public void setDashboardController(EtudiantDashboardController controller) {
        this.dashboardController = controller;
    }

    public void setSessionId(int id) {
        this.sessionId = id;
        loadSessionDetails();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    private void loadSessionDetails() {
        try {
            currentSession = sessionService.getById(sessionId);
            if (currentSession != null) {
                displaySessionDetails();
                loadConseils();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les détails: " + e.getMessage());
        }
    }

    private void displaySessionDetails() {
        themeLabel.setText(currentSession.getTheme());
        durationLabel.setText(currentSession.getDureeFormatted());
        auteurLabel.setText(currentSession.getAuteur());

        String audioUrl = currentSession.getAudioUrl();
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            audioSection.setVisible(true);
            audioSection.setManaged(true);
            audioUrlLabel.setText(audioUrl);
        } else {
            audioSection.setVisible(false);
            audioSection.setManaged(false);
        }
    }

    private void loadConseils() {
        try {
            List<Conseil> conseils = conseilService.getBySessionId(sessionId);
            conseilsContainer.getChildren().clear();

            if (conseils.isEmpty()) {
                noConseilsState.setVisible(true);
                noConseilsState.setManaged(true);
                conseilsContainer.setVisible(false);
            } else {
                noConseilsState.setVisible(false);
                noConseilsState.setManaged(false);
                conseilsContainer.setVisible(true);

                int index = 1;
                for (Conseil conseil : conseils) {
                    conseilsContainer.getChildren().add(createConseilItem(conseil, index++));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createConseilItem(Conseil conseil, int index) {
        HBox item = new HBox(15);
        item.getStyleClass().add("conseil-item");
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(15));

        Label numberLabel = new Label(String.valueOf(index));
        numberLabel.getStyleClass().add("conseil-number");
        numberLabel.setMinWidth(30);
        numberLabel.setMinHeight(30);
        numberLabel.setAlignment(Pos.CENTER);

        Label contentLabel = new Label(conseil.getContenu());
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("label");
        contentLabel.setStyle("-fx-font-size: 14px;");

        item.getChildren().addAll(numberLabel, contentLabel);

        return item;
    }

    @FXML
    public void playAudio() {
        if (currentSession != null && currentSession.getAudioUrl() != null && !currentSession.getAudioUrl().trim().isEmpty()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                String url = currentSession.getAudioUrl().trim();

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                desktop.browse(new URI(url));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le lien audio: " + e.getMessage());
            }
        }
    }

    @FXML
    public void goBack() {
        if (dashboardController != null) {
            dashboardController.showMeditations();
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
