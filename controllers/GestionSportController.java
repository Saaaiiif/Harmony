package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Exercice;
import services.ServiceExercice;

public class GestionSportController {

    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TableView<Exercice> tableExercices;
    //@FXML private TableColumn<Exercice, Integer> colId;
    @FXML private TableColumn<Exercice, String> colNom;
    @FXML private TableColumn<Exercice, String> colType;

    private ServiceExercice service = new ServiceExercice();
    private ObservableList<Exercice> exercicesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("CARDIO", "MUSCULATION"));
        //colId.setCellValueFactory(new PropertyValueFactory<>("id_exercice"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom_exercice"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_exercice"));

        chargerDonnees();

        tableExercices.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                nomField.setText(newV.getNom_exercice());
                typeCombo.setValue(newV.getType_exercice());
            }
        });
    }

    private void chargerDonnees() {
        exercicesList.setAll(service.afficherTout());
        tableExercices.setItems(exercicesList);
    }

    @FXML void ajouterExercice(ActionEvent event) {
        if (champsValides()) {
            service.ajouter(new Exercice(nomField.getText(), typeCombo.getValue(), ""));
            chargerDonnees();
            viderChamps(null);
        }
    }

    @FXML void modifierExercice(ActionEvent event) {
        Exercice e = tableExercices.getSelectionModel().getSelectedItem();
        if (e != null && champsValides()) {
            e.setNom_exercice(nomField.getText());
            e.setType_exercice(typeCombo.getValue());
            service.modifier(e);
            chargerDonnees();
            viderChamps(null);
        }
    }

    @FXML void supprimerExercice(ActionEvent event) {
        Exercice e = tableExercices.getSelectionModel().getSelectedItem();
        if (e != null) {
            service.supprimer(e.getId_exercice());
            chargerDonnees();
            viderChamps(null);
        }
    }

    @FXML void viderChamps(ActionEvent event) {
        nomField.clear();
        typeCombo.setValue(null);
        tableExercices.getSelectionModel().clearSelection();
    }

    private boolean champsValides() {
        if (nomField.getText().isEmpty() || typeCombo.getValue() == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Veuillez remplir tous les champs.");
            a.show();
            return false;
        }
        return true;
    }
}