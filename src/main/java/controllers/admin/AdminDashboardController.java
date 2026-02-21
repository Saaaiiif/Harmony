package controllers.admin;

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

public class AdminDashboardController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnMeditations;

    @FXML
    private Button btnJournaux;

    @FXML
    private Label userNameLabel;

    public static final int ADMIN_USER_ID = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showMeditations();
    }

    @FXML
    public void showMeditations() {
        updateActiveButton(btnMeditations);
        loadContent("/admin/AdminMeditation.fxml");
    }

    @FXML
    public void showJournaux() {
        updateActiveButton(btnJournaux);
        loadContent("/admin/AdminJournal.fxml");
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading: " + fxmlPath);
        }
    }

    private void updateActiveButton(Button activeButton) {
        btnMeditations.getStyleClass().remove("sidebar-btn-active");
        btnJournaux.getStyleClass().remove("sidebar-btn-active");

        if (!activeButton.getStyleClass().contains("sidebar-btn-active")) {
            activeButton.getStyleClass().add("sidebar-btn-active");
        }
    }
}
