package controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class AjouterExerciceMuscuController {
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }

    @FXML void validerAjoutMuscu(ActionEvent event) {
        System.out.println("Exercice Muscu ajouté !");
        // Retourne au journal après l'ajout
        FrontLayoutController.instance.loadPage("/JournalExercices.fxml");
    }
}