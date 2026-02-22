package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class AccueilActiviteController {

    // --- NAVIGATION DU FRONT OFFICE ---
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    // --- NAVIGATION VERS LE BACK OFFICE (ADMIN) ---
    @FXML
    void goToBackOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/root-layout.fxml"));
            Parent root = loader.load();
            controllers.RootLayoutController controller = loader.getController();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (controller != null) controller.setStage(stage);

            Scene scene = new Scene(root);

            URL cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

