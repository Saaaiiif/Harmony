package controllers.etudiant;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.SessionMeditation;
import services.ConseilService;
import services.SessionMeditationService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantMeditationController implements Initializable {

    @FXML
    private ComboBox<String> themeFilter;

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane sessionsContainer;

    @FXML
    private VBox emptyState;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService conseilService = new ConseilService();

    private List<SessionMeditation> allSessions;
    private EtudiantDashboardController dashboardController;

    public void setDashboardController(EtudiantDashboardController controller) {
        this.dashboardController = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadThemes();
        loadSessions();
    }

    private void loadThemes() {
        try {
            List<String> themes = sessionService.getAllThemes();
            themeFilter.getItems().clear();
            themeFilter.getItems().add("Tous les thèmes");
            themeFilter.getItems().addAll(themes);
            themeFilter.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSessions() {
        try {
            allSessions = sessionService.getAll();
            displaySessions(allSessions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void filterByTheme() {
        filterSessions();
    }

    @FXML
    public void onSearch() {
        filterSessions();
    }

    private void filterSessions() {
        String selectedTheme = themeFilter.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        List<SessionMeditation> filtered = allSessions.stream()
                .filter(s -> {
                    boolean matchesTheme = selectedTheme == null 
                            || selectedTheme.equals("Tous les thèmes")
                            || s.getTheme().equals(selectedTheme);
                    boolean matchesSearch = searchText.isEmpty()
                            || s.getTheme().toLowerCase().contains(searchText)
                            || s.getAuteur().toLowerCase().contains(searchText);
                    return matchesTheme && matchesSearch;
                })
                .toList();

        displaySessions(filtered);
    }

    private void displaySessions(List<SessionMeditation> sessions) {
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
    }

    private VBox createSessionCard(SessionMeditation session) {
        VBox card = new VBox(12);
        card.getStyleClass().add("meditation-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setMaxWidth(280);

        Label themeLabel = new Label(session.getTheme());
        themeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        themeLabel.setWrapText(true);

        HBox auteurBox = new HBox(5);
        auteurBox.setAlignment(Pos.CENTER_LEFT);
        Label auteurIcon = new Label("👨‍⚕️");
        Label auteurLabel = new Label(session.getAuteur());
        auteurLabel.getStyleClass().add("subtitle-label");
        auteurBox.getChildren().addAll(auteurIcon, auteurLabel);

        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label durationIcon = new Label("⏱️");
        Label durationLabel = new Label(session.getDureeFormatted());
        durationLabel.getStyleClass().add("subtitle-label");
        durationBox.getChildren().addAll(durationIcon, durationLabel);

        int conseilsCount = 0;
        try {
            conseilsCount = conseilService.getBySessionId(session.getId()).size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        HBox conseilsBox = new HBox(5);
        conseilsBox.setAlignment(Pos.CENTER_LEFT);
        Label conseilsIcon = new Label("💡");
        Label conseilsLabel = new Label(conseilsCount + " conseil(s)");
        conseilsLabel.getStyleClass().add("subtitle-label");
        conseilsBox.getChildren().addAll(conseilsIcon, conseilsLabel);

        Button viewBtn = new Button("Voir les détails →");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> viewDetails(session));

        card.getChildren().addAll(themeLabel, auteurBox, durationBox, conseilsBox, viewBtn);

        return card;
    }

    private void viewDetails(SessionMeditation session) {
        if (dashboardController != null) {
            dashboardController.showMeditationDetail(session.getId());
        }
    }
}
