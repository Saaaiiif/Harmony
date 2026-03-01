package controllers.MeditationControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.MeditationModels.SessionMeditation;
import services.MeditationServices.ConseilService;
import services.MeditationServices.SessionMeditationService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantMeditationController implements Initializable {

    @FXML private ComboBox<String> themeFilter;
    @FXML private TextField        searchField;
    @FXML private FlowPane         sessionsContainer;
    @FXML private VBox             emptyState;

    private final SessionMeditationService sessionService = new SessionMeditationService();
    private final ConseilService           conseilService = new ConseilService();

    private List<SessionMeditation> allSessions;
    private AccueilController       accueilController;

    
    public void setAccueilController(AccueilController ctrl) {
        this.accueilController = ctrl;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        themeFilter.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass().getResource("/views/HarmonieViews/harmonie-style.css").toExternalForm();
                if (!newScene.getStylesheets().contains(css)) newScene.getStylesheets().add(css);
            }
        });
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

    @FXML public void filterByTheme() { filterSessions(); }
    @FXML public void onSearch()      { filterSessions(); }

    private void filterSessions() {
        String selectedTheme = themeFilter.getValue();
        String searchText    = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        List<SessionMeditation> filtered = allSessions.stream()
                .filter(s -> {
                    boolean matchesTheme  = selectedTheme == null
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
            emptyState.setVisible(true);  emptyState.setManaged(true);
            sessionsContainer.setVisible(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            sessionsContainer.setVisible(true);
            for (SessionMeditation s : sessions) sessionsContainer.getChildren().add(createSessionCard(s));
        }
    }

    private VBox createSessionCard(SessionMeditation session) {
        VBox card = new VBox(12);
        card.getStyleClass().add("meditation-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(280); card.setMinWidth(280); card.setMaxWidth(280);

        Label themeLabel = new Label(session.getTheme());
        themeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        themeLabel.setWrapText(true);

        HBox auteurBox = new HBox(5);
        auteurBox.setAlignment(Pos.CENTER_LEFT);
        auteurBox.getChildren().addAll(new Label("👨‍⚕️"), labelWith(session.getAuteur(), "subtitle-label"));

        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        durationBox.getChildren().addAll(new Label("⏱️"), labelWith(session.getDureeFormatted(), "subtitle-label"));

        int conseilsCount = 0;
        try { conseilsCount = conseilService.getBySessionId(session.getId()).size(); } catch (Exception ignored) {}

        HBox conseilsBox = new HBox(5);
        conseilsBox.setAlignment(Pos.CENTER_LEFT);
        conseilsBox.getChildren().addAll(new Label("💡"), labelWith(conseilsCount + " conseil(s)", "subtitle-label"));

        Button viewBtn = new Button("Voir les détails →");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> viewDetails(session));

        card.getChildren().addAll(themeLabel, auteurBox, durationBox, conseilsBox, viewBtn);
        return card;
    }

    private Label labelWith(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private void viewDetails(SessionMeditation session) {
        if (accueilController != null) {
            accueilController.showMeditationDetail(session.getId());
        }
    }
}
