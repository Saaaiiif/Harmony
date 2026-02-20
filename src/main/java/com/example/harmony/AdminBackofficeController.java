package com.example.harmony;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import models.Evenement;
import models.Salle;
import models.StatutDemandeSalle;
import services.EvenementService;
import services.SalleService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
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

    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, Number> colEventId;
    @FXML private TableColumn<Evenement, String> colEventTitre;
    @FXML private TableColumn<Evenement, String> colEventDateDebut;
    @FXML private TableColumn<Evenement, String> colEventDateFin;
    @FXML private TableColumn<Evenement, String> colEventLieu;
    @FXML private TableColumn<Evenement, String> colEventSalle;
    @FXML private TableColumn<Evenement, String> colEventStatut;
    @FXML private TableColumn<Evenement, String> colEventType;

    @FXML private TableView<Evenement> demandesTable;
    @FXML private TableColumn<Evenement, Number> colDemandeId;
    @FXML private TableColumn<Evenement, String> colDemandeTitre;
    @FXML private TableColumn<Evenement, String> colDemandeDateDebut;
    @FXML private TableColumn<Evenement, String> colDemandeDateFin;
    @FXML private TableColumn<Evenement, String> colDemandeSalle;
    @FXML private TableColumn<Evenement, String> colDemandeType;
    @FXML private Label demandeNotificationLabel;

    private final SalleService salleService = new SalleService();
    private final EvenementService evenementService = new EvenementService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnesSalle();
        configurerColonnesEvent();
        configurerColonnesDemandes();
        chargerSalles();
        chargerEvents();
        chargerDemandes();
        if (adminTabPane != null) {
            adminTabPane.getSelectionModel().selectedItemProperty().addListener((o, oldTab, newTab) -> {
                if (newTab == null) return;
                switch (newTab.getText()) {
                    case "Événements": chargerEvents(); break;
                    case "Demandes de salles": chargerDemandes(); break;
                }
            });
        }
    }

    private void configurerColonnesSalle() {
        colSalleId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colSalleNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colSalleCapacite.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacite()));
        colSalleEquipements.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEquipements() != null ? cell.getValue().getEquipements() : ""));
        colSalleDisponible.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isDisponible() ? "Oui" : "Non"));
        colSalleDescription.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription() != null ? cell.getValue().getDescription() : ""));
    }

    private void configurerColonnesEvent() {
        if (eventsTable == null) return;
        if (colEventId != null) colEventId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        if (colEventTitre != null) colEventTitre.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getTitre())));
        if (colEventDateDebut != null) colEventDateDebut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateDebut() != null ? DATE_FORMAT.format(cell.getValue().getDateDebut()) : ""));
        if (colEventDateFin != null) colEventDateFin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateFin() != null ? DATE_FORMAT.format(cell.getValue().getDateFin()) : ""));
        if (colEventLieu != null) colEventLieu.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getLieu())));
        if (colEventSalle != null) colEventSalle.setCellValueFactory(cell -> {
            if (cell.getValue().getSalleId() == null) return new SimpleStringProperty("");
            Salle s = salleService.getById(cell.getValue().getSalleId());
            return new SimpleStringProperty(s != null ? s.getNom() : "");
        });
        if (colEventStatut != null) colEventStatut.setCellValueFactory(cell -> {
            if (cell.getValue().getStatutDemandeSalle() == null) return new SimpleStringProperty("");
            switch (cell.getValue().getStatutDemandeSalle()) {
                case EN_ATTENTE: return new SimpleStringProperty("En attente");
                case REFUSE: return new SimpleStringProperty("Refusé");
                case CONFIRME: return new SimpleStringProperty("Confirmé");
                default: return new SimpleStringProperty("");
            }
        });
        if (colEventType != null) colEventType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType() != null ? cell.getValue().getType().name() : ""));
        eventsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void configurerColonnesDemandes() {
        if (colDemandeId != null) colDemandeId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        if (colDemandeTitre != null) colDemandeTitre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitre() != null ? cell.getValue().getTitre() : ""));
        if (colDemandeDateDebut != null) colDemandeDateDebut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateDebut() != null ? DATE_FORMAT.format(cell.getValue().getDateDebut()) : ""));
        if (colDemandeDateFin != null) colDemandeDateFin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateFin() != null ? DATE_FORMAT.format(cell.getValue().getDateFin()) : ""));
        if (colDemandeSalle != null) colDemandeSalle.setCellValueFactory(cell -> {
            if (cell.getValue().getSalleId() == null) return new SimpleStringProperty("");
            Salle s = salleService.getById(cell.getValue().getSalleId());
            return new SimpleStringProperty(s != null ? s.getNom() : "");
        });
        if (colDemandeType != null) colDemandeType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType() != null ? cell.getValue().getType().name() : ""));
        if (demandesTable != null) demandesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void chargerSalles() {
        List<Salle> salles = salleService.getAll();
        salleTable.setItems(FXCollections.observableArrayList(salles));
    }

    private void chargerEvents() {
        if (eventsTable == null) return;
        List<Evenement> list = evenementService.getAll();
        eventsTable.setItems(FXCollections.observableArrayList(list));
    }

    private void chargerDemandes() {
        List<Evenement> list = evenementService.getDemandesEnAttente();
        demandesTable.setItems(FXCollections.observableArrayList(list));
        majNotificationDemandes();
    }

    private Dialog<Evenement> creerDialogEvenement(String titre, Evenement initial) {
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (getDialogOwner() != null) dialog.initOwner(getDialogOwner());
        dialog.getDialogPane().setMinWidth(540);
        dialog.getDialogPane().setMinHeight(520);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/harmony/event-form.fxml"));
            Parent form = loader.load();
            EventFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/com/example/harmony/styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.addEventFilter(ActionEvent.ACTION, e -> { if (!ctrl.validate()) e.consume(); });
            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                Evenement e = ctrl.buildEvenement(initial != null ? initial.getId() : 0);
                if (e != null && e.getSalleId() != null && initial == null) {
                    e.setStatutDemandeSalle(StatutDemandeSalle.CONFIRME);
                }
                return e;
            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger le formulaire : " + e.getMessage());
            return null;
        }
        return dialog;
    }

    @FXML
    private void onAjouterEvent() {
        Dialog<Evenement> d = creerDialogEvenement("Nouvel événement", null);
        if (d == null) return;
        d.showAndWait().filter(e -> e != null).ifPresent(e -> {
            try {
                evenementService.add(e);
                chargerEvents();
                chargerDemandes();
                showInfo("Événement créé.");
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onModifierEvent() {
        if (eventsTable == null) return;
        ObservableList<Evenement> sel = eventsTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner un événement à modifier."); return; }
        if (sel.size() > 1) { showInfo("Veuillez sélectionner un seul événement."); return; }
        Evenement ev = sel.get(0);
        Dialog<Evenement> d = creerDialogEvenement("Modifier l'événement", ev);
        if (d == null) return;
        d.showAndWait().filter(e -> e != null).ifPresent(e -> {
            try {
                e.setId(ev.getId());
                evenementService.update(e);
                chargerEvents();
                chargerDemandes();
                showInfo("Événement modifié.");
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onSupprimerEvent() {
        if (eventsTable == null) return;
        ObservableList<Evenement> sel = eventsTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner un ou plusieurs événements à supprimer."); return; }
        int n = sel.size();
        String msg = n == 1 ? "Supprimer l'événement \"" + sel.get(0).getTitre() + "\" ?" : "Supprimer les " + n + " événements sélectionnés ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                sel.forEach(e -> evenementService.delete(e.getId()));
                chargerEvents();
                chargerDemandes();
                showInfo("Événement(s) supprimé(s).");
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onConsulterEvent() {
        if (eventsTable == null) return;
        ObservableList<Evenement> sel = eventsTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner un événement à consulter."); return; }
        if (sel.size() > 1) { showInfo("Veuillez sélectionner un seul événement."); return; }
        Evenement e = sel.get(0);
        String salleInfo = "";
        if (e.getSalleId() != null) {
            Salle s = salleService.getById(e.getSalleId());
            salleInfo = "\nSalle : " + (s != null ? s.getNom() : "") + "\nStatut demande : " + (e.getStatutDemandeSalle() != null ? e.getStatutDemandeSalle() : "");
        }
        String detail = String.format("Titre : %s\nDescription : %s\nDate début : %s\nDate fin : %s\nLieu : %s%s\nType : %s",
                nullToEmpty(e.getTitre()), nullToEmpty(e.getDescription()),
                e.getDateDebut() != null ? DATE_FORMAT.format(e.getDateDebut()) : "",
                e.getDateFin() != null ? DATE_FORMAT.format(e.getDateFin()) : "",
                nullToEmpty(e.getLieu()), salleInfo, e.getType() != null ? e.getType().name() : "");
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détail de l'événement");
        info.setHeaderText(e.getTitre());
        info.setContentText(detail);
        info.showAndWait();
    }

    private void majNotificationDemandes() {
        if (demandeNotificationLabel == null) return;
        int n = evenementService.getDemandesEnAttente().size();
        if (n > 0) {
            demandeNotificationLabel.setText("🔔 " + n + " demande(s) de salle en attente");
            demandeNotificationLabel.setVisible(true);
            demandeNotificationLabel.setManaged(true);
        } else {
            demandeNotificationLabel.setVisible(false);
            demandeNotificationLabel.setManaged(false);
        }
    }

    @FXML
    private void onAccepterDemande() {
        ObservableList<Evenement> sel = demandesTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs demandes à accepter.");
            return;
        }
        try {
            for (Evenement e : sel) {
                Salle salle = salleService.getById(e.getSalleId());
                if (salle == null || !salle.isDisponible()) {
                    showError("La salle \"" + (salle != null ? salle.getNom() : "?") + "\" n'est pas disponible. Impossible d'accepter la demande \"" + e.getTitre() + "\".");
                    return;
                }
                if (!evenementService.isSalleDisponiblePourEvenement(e)) {
                    showError("La salle n'est pas disponible sur ce créneau (chevauchement avec un événement déjà confirmé). Demande : \"" + e.getTitre() + "\".");
                    return;
                }
                e.setStatutDemandeSalle(StatutDemandeSalle.CONFIRME);
                evenementService.update(e);
            }
            chargerDemandes();
            showInfo("Demande(s) acceptée(s) avec succès.");
        } catch (Exception ex) {
            showError("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void onRefuserDemande() {
        ObservableList<Evenement> sel = demandesTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs demandes à refuser.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le refus");
        confirm.setHeaderText(null);
        confirm.setContentText("Refuser " + sel.size() + " demande(s) ? L'utilisateur verra le statut \"Refusé\".");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                for (Evenement e : sel) {
                    e.setStatutDemandeSalle(StatutDemandeSalle.REFUSE);
                    evenementService.update(e);
                }
                chargerDemandes();
                showInfo("Demande(s) refusée(s).");
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onAjouterSalle() {
        Dialog<Salle> d = creerDialogSalle("Nouvelle salle", null);
        if (d == null) return;
        d.showAndWait().filter(s -> s != null).ifPresent(s -> {
            try {
                salleService.add(s);
                chargerSalles();
                showInfo("Salle ajoutée. Les utilisateurs pourront la sélectionner pour des événements \"Esprit\".");
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }

    @FXML
    private void onModifierSalle() {
        ObservableList<Salle> selected = salleTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une salle à modifier.");
            return;
        }
        if (selected.size() > 1) {
            showInfo("Veuillez sélectionner une seule salle.");
            return;
        }
        Salle s = selected.get(0);
        Dialog<Salle> d = creerDialogSalle("Modifier la salle", s);
        if (d == null) return;
        d.showAndWait().filter(x -> x != null).ifPresent(x -> {
            try {
                x.setId(s.getId());
                salleService.update(x);
                chargerSalles();
                showInfo("Salle modifiée.");
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }

    private Window getDialogOwner() {
        if (salleTable != null && salleTable.getScene() != null) return salleTable.getScene().getWindow();
        if (adminTabPane != null && adminTabPane.getScene() != null) return adminTabPane.getScene().getWindow();
        return null;
    }

    private Dialog<Salle> creerDialogSalle(String titre, Salle initial) {
        Dialog<Salle> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (getDialogOwner() != null) dialog.initOwner(getDialogOwner());
        dialog.getDialogPane().setMinWidth(460);
        dialog.getDialogPane().setMinHeight(440);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/harmony/salle-form.fxml"));
            Parent form = loader.load();
            SalleFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/com/example/harmony/styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.addEventFilter(ActionEvent.ACTION, e -> { if (!ctrl.validate()) e.consume(); });
            dialog.setResultConverter(btn -> btn == ButtonType.OK ? ctrl.buildSalle(initial != null ? initial.getId() : 0) : null);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger le formulaire : " + e.getMessage());
            return null;
        }
        return dialog;
    }

    @FXML
    private void onSupprimerSalle() {
        ObservableList<Salle> selected = salleTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner une ou plusieurs salles à supprimer.");
            return;
        }
        int count = selected.size();
        String message = count == 1 ? "Supprimer la salle \"" + selected.get(0).getNom() + "\" ?" : "Supprimer les " + count + " salles sélectionnées ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                selected.forEach(s -> salleService.delete(s.getId()));
                chargerSalles();
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
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
