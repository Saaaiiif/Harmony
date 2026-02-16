package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import models.Evenement;
import models.TypeEvenement;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class EventFormController implements Initializable {

    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private DatePicker fieldDateDebut;
    @FXML private DatePicker fieldDateFin;
    @FXML private TextField fieldLieu;
    @FXML private Spinner<Integer> fieldPriorite;
    @FXML private CheckBox fieldRappelActif;
    @FXML private ComboBox<String> fieldType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (fieldPriorite != null) {
            fieldPriorite.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        }
        if (fieldType != null) {
            fieldType.getItems().setAll("COURS", "REUNION", "LOISIR");
            if (!fieldType.getItems().isEmpty()) {
                fieldType.getSelectionModel().selectFirst();
            }
        }
    }

    public void initFrom(Evenement e) {
        if (e == null) return;
        if (fieldTitre != null) {
            fieldTitre.setText(e.getTitre() != null ? e.getTitre() : "");
        }
        if (fieldDescription != null) {
            fieldDescription.setText(e.getDescription() != null ? e.getDescription() : "");
        }
        if (fieldDateDebut != null) {
            fieldDateDebut.setValue(dateToLocalDate(e.getDateDebut()));
        }
        if (fieldDateFin != null) {
            fieldDateFin.setValue(dateToLocalDate(e.getDateFin()));
        }
        if (fieldLieu != null) {
            fieldLieu.setText(e.getLieu() != null ? e.getLieu() : "");
        }
        if (fieldPriorite != null && fieldPriorite.getValueFactory() != null) {
            fieldPriorite.getValueFactory().setValue(Math.max(1, Math.min(10, e.getPriorite())));
        }
        if (fieldRappelActif != null) {
            fieldRappelActif.setSelected(e.isRappelActif());
        }
        if (fieldType != null && e.getType() != null) {
            fieldType.getSelectionModel().select(e.getType().name());
        }
    }

    /** Construit un Evenement à partir des champs (id = 0 pour nouvel événement). */
    public Evenement buildEvenement(int id) {
        String titre = (fieldTitre != null && fieldTitre.getText() != null) ? fieldTitre.getText().trim() : "";
        String description = (fieldDescription != null && fieldDescription.getText() != null) ? fieldDescription.getText().trim() : "";
        LocalDate deb = fieldDateDebut != null ? fieldDateDebut.getValue() : null;
        LocalDate fin = fieldDateFin != null ? fieldDateFin.getValue() : null;
        String lieu = (fieldLieu != null && fieldLieu.getText() != null) ? fieldLieu.getText().trim() : "";
        int priorite = (fieldPriorite != null && fieldPriorite.getValue() != null) ? fieldPriorite.getValue() : 1;
        boolean rappel = fieldRappelActif != null && fieldRappelActif.isSelected();
        String typeStr = (fieldType != null && fieldType.getSelectionModel().getSelectedItem() != null) 
            ? fieldType.getSelectionModel().getSelectedItem() : "REUNION";
        TypeEvenement type = TypeEvenement.REUNION;
        try {
            type = TypeEvenement.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = TypeEvenement.REUNION;
        }

        Date dateDebut = deb != null ? localDateToDate(deb) : new Date();
        Date dateFin = fin != null ? localDateToDate(fin) : new Date();

        return new Evenement(id, titre, description, dateDebut, dateFin, lieu, priorite, rappel, type);
    }

    public boolean validate() {
        if (fieldTitre == null || fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            showError("Le titre est obligatoire.");
            return false;
        }
        if (fieldDateDebut == null || fieldDateDebut.getValue() == null) {
            showError("La date de début est obligatoire.");
            return false;
        }
        if (fieldDateFin == null || fieldDateFin.getValue() == null) {
            showError("La date de fin est obligatoire.");
            return false;
        }
        if (fieldDateFin.getValue().isBefore(fieldDateDebut.getValue())) {
            showError("La date de fin doit être après la date de début.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Champs invalides");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    /** Convertit java.util.Date ou java.sql.Date en LocalDate. java.sql.Date n'a pas toInstant(). */
    private static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private static Date localDateToDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
