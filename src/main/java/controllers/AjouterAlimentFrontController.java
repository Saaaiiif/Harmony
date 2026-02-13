package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AjouterAlimentFrontController {

    @FXML
    private TextField tfRecherche;

    @FXML
    private TextField tfQuantiteGrammes;

    @FXML
    void goToAccueil(ActionEvent event) {
        FrontLayoutController.instance.loadPage("/AccueilActivite.fxml");
    }

    @FXML
    void goToAliments(ActionEvent event) {
        FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml");
    }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }
}