package controllers.RessourceControllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import models.RessourceModels.Salle;

import java.net.URL;
import java.util.ResourceBundle;

public class SalleFormController implements Initializable {

    @FXML private TextField fieldNom;
    @FXML private Spinner<Integer> fieldCapacite;
    @FXML private TextArea fieldEquipements;
    @FXML private CheckBox fieldDisponible;
    @FXML private TextField fieldDescription;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (fieldCapacite != null) {
            fieldCapacite.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 20));
        }
        if (fieldDisponible != null) {
            fieldDisponible.setSelected(true);
        }
    }

    public void initFrom(Salle s) {
        if (s == null) return;
        if (fieldNom != null) fieldNom.setText(s.getNom() != null ? s.getNom() : "");
        if (fieldCapacite != null) fieldCapacite.getValueFactory().setValue(Math.max(1, s.getCapacite()));
        if (fieldEquipements != null) fieldEquipements.setText(s.getEquipements() != null ? s.getEquipements() : "");
        if (fieldDisponible != null) fieldDisponible.setSelected(s.isDisponible());
        if (fieldDescription != null) fieldDescription.setText(s.getDescription() != null ? s.getDescription() : "");
    }

    public Salle buildSalle(int id) {
        String nom = fieldNom != null && fieldNom.getText() != null ? fieldNom.getText().trim() : "";
        int capacite = fieldCapacite != null && fieldCapacite.getValue() != null ? fieldCapacite.getValue() : 20;
        String equipements = fieldEquipements != null && fieldEquipements.getText() != null ? fieldEquipements.getText().trim() : null;
        boolean dispo = fieldDisponible != null && fieldDisponible.isSelected();
        String desc = fieldDescription != null && fieldDescription.getText() != null ? fieldDescription.getText().trim() : null;
        return new Salle(id, nom, capacite, equipements, dispo, desc, null);
    }

    public boolean validate() {
        if (fieldNom == null || fieldNom.getText() == null || fieldNom.getText().trim().isEmpty()) {
            showError("Le nom de la salle est obligatoire.");
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
}
