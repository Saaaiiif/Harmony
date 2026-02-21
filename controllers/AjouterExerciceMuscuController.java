package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;
import javafx.util.Callback;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class AjouterExerciceMuscuController {
    @FXML private ComboBox<Exercice> comboExercices;
    @FXML private DatePicker dpDate;
    @FXML private Spinner<Integer> spinnerSeries;
    @FXML private Spinner<Integer> spinnerRepetitions;
    @FXML private Spinner<Double> spinnerPoids;
    @FXML private TextField tfNotes;
    @FXML private Label erreurLabel;

    private ServiceExercice serviceExercice = new ServiceExercice();
    private ServiceActivite serviceActivite = new ServiceActivite();

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());

        spinnerSeries.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3));
        spinnerRepetitions.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        spinnerPoids.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 500.0, 10.0, 2.5));

        List<Exercice> muscus = serviceExercice.afficherTout().stream()
                .filter(e -> "MUSCULATION".equalsIgnoreCase(e.getType_exercice()))
                .collect(Collectors.toList());
        comboExercices.setItems(FXCollections.observableArrayList(muscus));

        Callback<ListView<Exercice>, ListCell<Exercice>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Exercice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // ✨ LIGNE CRUCIALE : on supprime le texte parasite ici aussi
                    setText(null);

                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("🏋️");
                    icon.setStyle("-fx-font-size: 24px;");

                    VBox texts = new VBox(2);
                    Label name = new Label(item.getNom_exercice());
                    name.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 15px;");
                    Label subtitle = new Label("Développement musculaire");
                    subtitle.setStyle("-fx-text-fill: #444444; -fx-font-size: 11px; -fx-font-style: italic;");

                    texts.getChildren().addAll(name, subtitle);
                    root.getChildren().addAll(icon, texts);
                    setGraphic(root);
                }
            }
        };

        comboExercices.setCellFactory(cellFactory);
        comboExercices.setButtonCell(cellFactory.call(null));
    }

    @FXML void validerAjoutMuscu(ActionEvent event) {
        Exercice ex = comboExercices.getValue();
        if (ex == null || dpDate.getValue() == null) {
            erreurLabel.setText("⚠️ Séquence incomplète : Sélectionnez un exercice.");
            return;
        }
        try {
            Activite a = new Activite();
            a.setDate_activite(Timestamp.valueOf(dpDate.getValue().atTime(LocalTime.now())));
            a.setId_exercice(ex.getId_exercice());
            a.setNb_series(spinnerSeries.getValue());
            a.setNb_repetitions(spinnerRepetitions.getValue());
            a.setPoids(spinnerPoids.getValue().floatValue());
            String notes = (tfNotes.getText() == null || tfNotes.getText().trim().isEmpty()) ? "" : tfNotes.getText();
            a.setNotes(notes);

            serviceActivite.ajouter(a);
            goToExercices(null);

        } catch (Exception e) {
            erreurLabel.setText("❌ Erreur système lors de l'enregistrement.");
            e.printStackTrace();
        }
    }

    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }
}