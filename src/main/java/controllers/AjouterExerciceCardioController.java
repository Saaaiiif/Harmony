package controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class AjouterExerciceCardioController {
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    @FXML void validerAjoutCardio(ActionEvent event) {
        System.out.println("Exercice Cardio ajouté !");
        // Retourne au journal après l'ajout
        FrontLayoutController.instance.loadPage("/JournalExercices.fxml");
    }
}