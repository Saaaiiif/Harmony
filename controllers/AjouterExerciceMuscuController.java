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

public class AjouterExerciceMuscuController {

    @FXML private TextField tfRecherche;
    @FXML private ListView<Exercice> listResultats;
    @FXML private DatePicker dpDate;
    @FXML private Spinner<Integer> spinnerSeries;
    @FXML private Spinner<Integer> spinnerRepetitions;
    @FXML private Spinner<Double> spinnerPoids;
    @FXML private TextField tfNotes;
    @FXML private Label erreurLabel;

    private ServiceExercice serviceExercice = new ServiceExercice();
    private ServiceActivite serviceActivite = new ServiceActivite();
    private ObservableList<Exercice> exercicesMuscu;
    private Exercice exerciceSelectionne = null;

    private boolean isUpdatingFromList = false;

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());
        spinnerSeries.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3));
        spinnerRepetitions.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        spinnerPoids.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 500.0, 20.0, 2.5));

        exercicesMuscu = FXCollections.observableArrayList(
                serviceExercice.afficherTout().stream()
                        .filter(e -> "Musculation".equalsIgnoreCase(e.getType_exercice()))
                        .collect(Collectors.toList())
        );

        tfRecherche.textProperty().addListener((obs, oldText, newText) -> {
            if (isUpdatingFromList) return;

            if (newText == null || newText.trim().isEmpty()) {
                listResultats.setVisible(false);
                listResultats.setManaged(false);
                exerciceSelectionne = null;
            } else {
                List<Exercice> trouves = exercicesMuscu.stream()
                        .filter(e -> e.getNom_exercice().toLowerCase().startsWith(newText.toLowerCase()))
                        .collect(Collectors.toList());

                listResultats.setItems(FXCollections.observableArrayList(trouves));

                boolean hasResults = !trouves.isEmpty();
                listResultats.setVisible(hasResults);
                listResultats.setManaged(hasResults);
            }
        });

        listResultats.setOnMouseClicked(event -> {
            Exercice selected = listResultats.getSelectionModel().getSelectedItem();
            if (selected != null) {
                exerciceSelectionne = selected;

                isUpdatingFromList = true;
                tfRecherche.setText(selected.getNom_exercice());
                isUpdatingFromList = false;

                listResultats.setVisible(false);
                listResultats.setManaged(false);
            }
        });
    }

    @FXML
    void validerAjoutMuscu(ActionEvent event) {
        if (exerciceSelectionne == null || dpDate.getValue() == null) {
            erreurLabel.setText("⚠️ Veuillez rechercher et sélectionner un exercice.");
            return;
        }
        try {
            Activite a = new Activite();
            a.setDate_activite(Timestamp.valueOf(dpDate.getValue().atTime(LocalTime.now())));
            a.setId_exercice(exerciceSelectionne.getId_exercice());

            a.setNb_series(spinnerSeries.getValue());
            a.setNb_repetitions(spinnerRepetitions.getValue());
            a.setPoids(spinnerPoids.getValue().floatValue());

            a.setDuree_minutes(0);
            a.setCalories_brulees(0);
            a.setNotes(tfNotes.getText() != null ? tfNotes.getText() : "");

            serviceActivite.ajouter(a);
            goToExercices(event);
        } catch (Exception e) {
            erreurLabel.setText("❌ Erreur système.");
        }
    }

    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
}