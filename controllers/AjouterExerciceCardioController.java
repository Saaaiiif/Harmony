package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class AjouterExerciceCardioController {

    @FXML private TextField tfRecherche;
    @FXML private ListView<Exercice> listResultats;
    @FXML private DatePicker dpDate;
    @FXML private Spinner<Integer> spinnerDuree;
    @FXML private Spinner<Integer> spinnerCalories;
    @FXML private TextField tfNotes;
    @FXML private Label erreurLabel;

    private ServiceExercice serviceExercice = new ServiceExercice();
    private ServiceActivite serviceActivite = new ServiceActivite();
    private ObservableList<Exercice> exercicesCardio;
    private Exercice exerciceSelectionne = null;

    // Empêche la recherche de se relancer quand on clique sur un résultat
    private boolean isUpdatingFromList = false;

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());

        spinnerDuree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 30));
        spinnerCalories.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2000, 100));

        exercicesCardio = FXCollections.observableArrayList(
                serviceExercice.afficherTout().stream()
                        .filter(e -> "Cardio".equalsIgnoreCase(e.getType_exercice()))
                        .collect(Collectors.toList())
        );

        // Recherche dynamique qui commence par la lettre tapée
        tfRecherche.textProperty().addListener((obs, oldText, newText) -> {
            if (isUpdatingFromList) return;

            if (newText == null || newText.trim().isEmpty()) {
                listResultats.setVisible(false);
                listResultats.setManaged(false);
                exerciceSelectionne = null;
            } else {
                List<Exercice> trouves = exercicesCardio.stream()
                        // Filtre "commence par"
                        .filter(e -> e.getNom_exercice().toLowerCase().startsWith(newText.toLowerCase()))
                        .collect(Collectors.toList());

                listResultats.setItems(FXCollections.observableArrayList(trouves));

                boolean hasResults = !trouves.isEmpty();
                listResultats.setVisible(hasResults);
                listResultats.setManaged(hasResults);
            }
        });

        // Clic sur un exercice de la liste
        listResultats.setOnMouseClicked(event -> {
            Exercice selected = listResultats.getSelectionModel().getSelectedItem();
            if (selected != null) {
                exerciceSelectionne = selected;

                isUpdatingFromList = true;
                tfRecherche.setText(selected.getNom_exercice());
                isUpdatingFromList = false;

                // Cache la liste après sélection
                listResultats.setVisible(false);
                listResultats.setManaged(false);
            }
        });
    }

    @FXML
    void validerAjoutCardio(ActionEvent event) {
        if (exerciceSelectionne == null || dpDate.getValue() == null) {
            erreurLabel.setText("⚠️ Veuillez rechercher et sélectionner un exercice.");
            return;
        }

        try {
            Activite a = new Activite();
            a.setDate_activite(Timestamp.valueOf(dpDate.getValue().atTime(LocalTime.now())));
            a.setId_exercice(exerciceSelectionne.getId_exercice());

            a.setDuree_minutes(spinnerDuree.getValue() != null ? spinnerDuree.getValue() : 0);
            a.setCalories_brulees(spinnerCalories.getValue() != null ? spinnerCalories.getValue() : 0);

            a.setNb_series(0);
            a.setNb_repetitions(0);
            a.setPoids(0.0f);

            a.setNotes((tfNotes.getText() == null || tfNotes.getText().trim().isEmpty()) ? "" : tfNotes.getText());

            serviceActivite.ajouter(a);
            goToExercices(event);

        } catch (Exception e) {
            erreurLabel.setText("❌ Erreur système lors de l'enregistrement.");
        }
    }

    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
}