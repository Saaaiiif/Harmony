package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class JournalAlimentaireController {

    @FXML
    void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }

    @FXML
    void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }

    @FXML
    void goToAjouterAliment(ActionEvent event) { FrontLayoutController.instance.loadPage("/AjouterAlimentFront.fxml"); }

    @FXML
    void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }

    @FXML
    void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }
}