package com.example.harmony;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Window;
import models.Salle;
import models.Seance;
import services.SalleService;
import services.SeanceService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminBackofficeController implements Initializable {

    @FXML private TabPane adminTabPane;
    @FXML private TableView<Salle> salleTable;
    @FXML private TableColumn<Salle, Number> colSalleId;
    @FXML private TableColumn<Salle, String> colSalleNom;
    @FXML private TableColumn<Salle, Number> colSalleCapacite;
    @FXML private TableColumn<Salle, String> colSalleEquipements;
    @FXML private TableColumn<Salle, String> colSalleDisponible;
    @FXML private TableColumn<Salle, String> colSalleDescription;

    @FXML private TableView<Seance> seanceTable;
    @FXML private TableColumn<Seance, Number> colSeanceId;
    @FXML private TableColumn<Seance, String> colSeanceTitre;
    @FXML private TableColumn<Seance, String> colSeanceDateDebut;
    @FXML private TableColumn<Seance, String> colSeanceDateFin;
    @FXML private TableColumn<Seance, String> colSeanceSalle;
    @FXML private TableColumn<Seance, String> colSeanceConfirmee;
    @FXML private TableColumn<Seance, String> colSeanceType;
    @FXML private TableColumn<Seance, Number> colSeanceParticipants;

    private final SalleService salleService = new SalleService();
    private final SeanceService seanceService = new SeanceService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnesSalle();
        configurerColonnesSeance();
        chargerSalles();
        chargerSeances();
    }

    private void configurerColonnesSalle() {
        colSalleId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colSalleNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colSalleCapacite.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacite()));
        colSalleEquipements.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEquipements() != null ? cell.getValue().getEquipements() : ""));
        colSalleDisponible.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isDisponible() ? "Oui" : "Non"));
        colSalleDescription.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription() != null ? cell.getValue().getDescription() : ""));
    }

    private void configurerColonnesSeance() {
        colSeanceId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colSeanceTitre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitre()));
        colSeanceDateDebut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateDebut() != null ? DATE_FORMAT.format(cell.getValue().getDateDebut()) : ""));
        colSeanceDateFin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateFin() != null ? DATE_FORMAT.format(cell.getValue().getDateFin()) : ""));
        colSeanceSalle.setCellValueFactory(cell -> {
            Salle salle = salleService.getById(cell.getValue().getSalleId());
            return new SimpleStringProperty(salle != null ? salle.getNom() : "N/A");
        });
        colSeanceConfirmee.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isConfirmee() ? "Oui" : "Non"));
        colSeanceType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypeSeance()));
        colSeanceParticipants.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getNombreParticipants()));
    }

    private void chargerSalles() {
        List<Salle> salles = salleService.getAll();
        salleTable.setItems(FXCollections.observableArrayList(salles));
    }

    private void chargerSeances() {
        List<Seance> seances = seanceService.getAll();
        seanceTable.setItems(FXCollections.observableArrayList(seances));
    }

    @FXML
    private void onAjouterSalle() {
        // TODO: Créer formulaire d'ajout de salle
        showInfo("Fonctionnalité à implémenter : Ajouter une salle");
    }

    @FXML
    private void onModifierSalle() {
        ObservableList<Salle> selected = salleTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une salle à modifier.");
            return;
        }
        // TODO: Créer formulaire de modification
        showInfo("Fonctionnalité à implémenter : Modifier une salle");
    }

    @FXML
    private void onSupprimerSalle() {
        ObservableList<Salle> selected = salleTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs salles à supprimer.");
            return;
        }
        int count = selected.size();
        String message = count == 1 
            ? "Supprimer la salle \"" + selected.get(0).getNom() + "\" ?"
            : "Supprimer les " + count + " salles sélectionnées ?";
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        Optional<ButtonType> rep = confirm.showAndWait();
        if (rep.isPresent() && rep.get() == ButtonType.OK) {
            try {
                for (Salle s : selected) {
                    salleService.delete(s.getId());
                }
                chargerSalles();
            } catch (Exception e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    private void onConfirmerSeance() {
        ObservableList<Seance> selected = seanceTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs séances à confirmer.");
            return;
        }
        try {
            for (Seance s : selected) {
                s.setConfirmee(true);
                seanceService.update(s);
            }
            chargerSeances();
            showInfo("Séance(s) confirmée(s) avec succès.");
        } catch (Exception e) {
            showError("Erreur lors de la confirmation : " + e.getMessage());
        }
    }

    @FXML
    private void onRejeterSeance() {
        ObservableList<Seance> selected = seanceTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs séances à rejeter.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le rejet");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer cette/ces séance(s) ?");
        Optional<ButtonType> rep = confirm.showAndWait();
        if (rep.isPresent() && rep.get() == ButtonType.OK) {
            try {
                for (Seance s : selected) {
                    seanceService.delete(s.getId());
                }
                chargerSeances();
            } catch (Exception e) {
                showError("Erreur lors du rejet : " + e.getMessage());
            }
        }
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
