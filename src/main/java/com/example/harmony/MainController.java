package com.example.harmony;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML
    private ImageView logoImage;

    @FXML
    private Button upNextButton;

    // Field for window operations
    private Stage stage;

    @FXML
    public void initialize() {


        // Highlight the current page's navigation label
        if (upNextButton != null) {
            upNextButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleUpNextButtonClick() {
        try {
            SceneTransitionUtil.changeContent(
                    "/com/example/harmony/hello-view.fxml",
                SceneTransitionUtil.TransitionType.FADE, 
                MainController.class
            );
        } catch (IOException e) {
            System.err.println("Failed to load hello-view.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Sets the stage for window operations.
     * @param stage The primary stage of the application
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
