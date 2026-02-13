package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;

public class JournalExercicesController {

    @FXML private TextArea notesTextArea;
    @FXML private Label erreurLabel;
    @FXML private TableView<?> seancesTable; // Plus tard, on liera ça à ta base de données

    // Navigation de la barre violette
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    @FXML
    void ajouterCardio(ActionEvent event) {
        // Charge la NOUVELLE page Cardio
        FrontLayoutController.instance.loadPage("/AjouterExerciceCardio.fxml");
    }

    @FXML
    void ajouterMusculation(ActionEvent event) {
        // Charge la NOUVELLE page Musculation
        FrontLayoutController.instance.loadPage("/AjouterExerciceMuscu.fxml");
    }

    @FXML
    void enregistrerSeance(ActionEvent event) {
        String note = notesTextArea.getText();

        // Contrôle de saisie obligatoire pour les notes
        if (note == null || note.trim().isEmpty()) {
            erreurLabel.setText("Veuillez obligatoirement ajouter une note pour votre séance.");
            erreurLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Si tout est bon
        erreurLabel.setText("Séance enregistrée avec succès !");
        erreurLabel.setStyle("-fx-text-fill: green;");
        System.out.println("Séance sauvegardée. Note : " + note);

        // Vider le champ après enregistrement
        notesTextArea.clear();

        // Bientôt : Ajouter cette séance à la TableView (Historique) via la Base de Données
    }
}