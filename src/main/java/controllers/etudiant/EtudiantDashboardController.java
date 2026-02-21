package controllers.etudiant;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EtudiantDashboardController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnMeditations;

    @FXML
    private Button btnJournal;

    @FXML
    private Label userNameLabel;

    public static final int ETUDIANT_USER_ID = 2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showMeditations();
    }

    @FXML
    public void showMeditations() {
        updateActiveButton(btnMeditations);
        loadContent("/etudiant/EtudiantMeditation.fxml");
    }

    @FXML
    public void showJournal() {
        updateActiveButton(btnJournal);
        loadContent("/etudiant/EtudiantJournal.fxml");
    }

    public void showMeditationDetail(int sessionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/etudiant/EtudiantMeditationDetail.fxml"));
            Node content = loader.load();
            
            EtudiantMeditationDetailController controller = loader.getController();
            controller.setSessionId(sessionId);
            controller.setDashboardController(this);
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading meditation detail");
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EtudiantMeditationController) {
                ((EtudiantMeditationController) controller).setDashboardController(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading: " + fxmlPath);
        }
    }

    private void updateActiveButton(Button activeButton) {
        btnMeditations.getStyleClass().remove("sidebar-btn-active");
        btnJournal.getStyleClass().remove("sidebar-btn-active");

        if (!activeButton.getStyleClass().contains("sidebar-btn-active")) {
            activeButton.getStyleClass().add("sidebar-btn-active");
        }
    }
}
