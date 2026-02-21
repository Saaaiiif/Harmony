package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import models.StatutTache;
import models.Tache;
import services.TacheService;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class TacheFormController implements Initializable {

    @FXML private TextField fieldNom;
    @FXML private DatePicker fieldDeadline;
    @FXML private TextArea fieldNotes;
    @FXML private ComboBox<String> fieldStatut;

    private final TacheService tacheService = new TacheService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fieldStatut.getItems().setAll("A_FAIRE", "EN_COURS", "TERMINEE");
        fieldStatut.getSelectionModel().selectFirst();
    }

    public void initFrom(Tache t) {
        if (t == null) return;
        fieldNom.setText(t.getNom() != null ? t.getNom() : "");
        fieldDeadline.setValue(dateToLocalDate(t.getDeadline()));
        fieldNotes.setText(t.getNotes() != null ? t.getNotes() : "");
        if (t.getStatut() != null) {
            fieldStatut.getSelectionModel().select(t.getStatut().name());
        }
    }

    public Tache buildTache(int id) {
        String nom = fieldNom.getText() != null ? fieldNom.getText().trim() : "";
        Date deadline = localDateToDate(fieldDeadline.getValue());
        String notes = fieldNotes.getText() != null ? fieldNotes.getText().trim() : "";
        String statutStr = fieldStatut.getSelectionModel().getSelectedItem();
        StatutTache statut = statutStr != null ? StatutTache.valueOf(statutStr) : StatutTache.A_FAIRE;
        return new Tache(id, nom, deadline, notes, statut);
    }

    public boolean validate() {
        if (fieldNom == null || fieldNom.getText() == null || fieldNom.getText().trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le nom est obligatoire.").showAndWait();
            return false;
        }
        return true;
    }

    private static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date localDateToDate(LocalDate d) {
        if (d == null) return null;
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
