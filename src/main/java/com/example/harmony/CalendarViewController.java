package com.example.harmony;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import models.Evenement;
import models.Tache;
import models.TypeEvenement;
import services.EvenementService;
import services.TacheService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CalendarViewController implements Initializable {

    @FXML private TabPane mainTabPane;
    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, Number> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colDescription;
    @FXML private TableColumn<Evenement, String> colDateDebut;
    @FXML private TableColumn<Evenement, String> colDateFin;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, Number> colPriorite;
    @FXML private TableColumn<Evenement, String> colRappel;
    @FXML private TableColumn<Evenement, String> colType;

    @FXML private TableView<Tache> tacheTable;
    @FXML private TableColumn<Tache, Number> colTacheId;
    @FXML private TableColumn<Tache, String> colTacheNom;
    @FXML private TableColumn<Tache, String> colTacheDeadline;
    @FXML private TableColumn<Tache, String> colTacheStatut;

    private final EvenementService evenementService = new EvenementService();
    private final TacheService tacheService = new TacheService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnes();
        configurerColonnesTache();
        configurerSelectionMultiple();
        chargerEvenements();
        chargerTaches();
    }

    private Window getDialogOwner() {
        if (eventsTable != null && eventsTable.getScene() != null) {
            return eventsTable.getScene().getWindow();
        }
        if (mainTabPane != null && mainTabPane.getScene() != null) {
            return mainTabPane.getScene().getWindow();
        }
        return null;
    }

    private void configurerSelectionMultiple() {
        eventsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colTitre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitre()));
        colDescription.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getDescription())));
        colDateDebut.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDateDebut())));
        colDateFin.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDateFin())));
        colLieu.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getLieu())));
        colPriorite.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getPriorite()));
        colRappel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isRappelActif() ? "Oui" : "Non"));
        colType.setCellValueFactory(cell -> {
            TypeEvenement t = cell.getValue().getType();
            return new SimpleStringProperty(t != null ? t.name() : "");
        });
    }

    private String formatDate(Date date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void configurerColonnesTache() {
        if (colTacheId != null) colTacheId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        if (colTacheNom != null) colTacheNom.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getNom())));
        if (colTacheDeadline != null) colTacheDeadline.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDeadline())));
        if (colTacheStatut != null) colTacheStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatut() != null ? cell.getValue().getStatut().name() : ""));
        if (tacheTable != null) tacheTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void chargerEvenements() {
        if (eventsTable == null) return;
        List<Evenement> list = evenementService.getAll();
        eventsTable.setItems(FXCollections.observableArrayList(list));
    }

    private void chargerTaches() {
        if (tacheTable == null) return;
        List<Tache> list = tacheService.getAll();
        tacheTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    private void onAjouter() {
        Dialog<Evenement> dialog = creerDialogEvenement("Nouvel événement", null);
        if (dialog == null) return;
        Optional<Evenement> result = dialog.showAndWait();
        result.filter(e -> e != null).ifPresent(e -> {
            evenementService.add(e);
            chargerEvenements();
        });
    }

    @FXML
    private void onModifier() {
        ObservableList<Evenement> selected = eventsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner un événement à modifier.");
            return;
        }
        if (selected.size() > 1) {
            showInfo("Veuillez sélectionner un seul événement à modifier.");
            return;
        }
        Evenement event = selected.get(0);
        Dialog<Evenement> dialog = creerDialogEvenement("Modifier l'événement", event);
        if (dialog == null) return;
        Optional<Evenement> result = dialog.showAndWait();
        result.filter(e -> e != null).ifPresent(e -> {
            e.setId(event.getId());
            evenementService.update(e);
            chargerEvenements();
        });
    }

    @FXML
    private void onSupprimer() {
        ObservableList<Evenement> selected = eventsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner un ou plusieurs événements à supprimer.");
            return;
        }
        int count = selected.size();
        String message = count == 1 
            ? "Supprimer l'événement \"" + selected.get(0).getTitre() + "\" ?"
            : "Supprimer les " + count + " événements sélectionnés ?";
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        Optional<ButtonType> rep = confirm.showAndWait();
        if (rep.isPresent() && rep.get() == ButtonType.OK) {
            for (Evenement e : selected) {
                evenementService.delete(e.getId());
            }
            chargerEvenements();
        }
    }

    @FXML
    private void onConsulter() {
        ObservableList<Evenement> selected = eventsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("Veuillez sélectionner un événement à consulter.");
            return;
        }
        if (selected.size() > 1) {
            showInfo("Veuillez sélectionner un seul événement à consulter.");
            return;
        }
        Evenement event = selected.get(0);
        String detail = String.format(
                "Titre : %s\nDescription : %s\nDate début : %s\nDate fin : %s\nLieu : %s\nPriorité : %d\nRappel : %s\nType : %s",
                nullToEmpty(event.getTitre()),
                nullToEmpty(event.getDescription()),
                formatDate(event.getDateDebut()),
                formatDate(event.getDateFin()),
                nullToEmpty(event.getLieu()),
                event.getPriorite(),
                event.isRappelActif() ? "Oui" : "Non",
                event.getType() != null ? event.getType().name() : ""
        );
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détail de l'événement");
        info.setHeaderText(event.getTitre());
        info.setContentText(detail);
        info.showAndWait();
    }

    private Dialog<Evenement> creerDialogEvenement(String titre, Evenement initial) {
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        // Obtenir la fenêtre parent depuis le tableau
        Window window = getDialogOwner();
        if (window != null) dialog.initOwner(window);
        
        dialog.getDialogPane().setMinWidth(520);
        dialog.getDialogPane().setMinHeight(480);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        try {
            URL formUrl = getClass().getResource("/com/example/harmony/event-form.fxml");
            if (formUrl == null) {
                throw new RuntimeException("Fichier event-form.fxml introuvable dans /com/example/harmony/");
            }
            
            FXMLLoader loader = new FXMLLoader(formUrl);
            Parent form = loader.load();
            EventFormController formCtrl = loader.getController();
            
            if (formCtrl == null) {
                throw new RuntimeException("Controller EventFormController non trouvé");
            }
            
            if (initial != null) {
                formCtrl.initFrom(initial);
            }
            
            dialog.getDialogPane().setContent(form);
            
            URL cssUrl = getClass().getResource("/com/example/harmony/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.addEventFilter(ActionEvent.ACTION, e -> {
                    if (!formCtrl.validate()) {
                        e.consume();
                    }
                });
            }
            
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    return formCtrl.buildEvenement(initial != null ? initial.getId() : 0);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getName();
                if (e.getCause() != null) {
                    errorMsg += " - Cause: " + e.getCause().getMessage();
                }
            }
            
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible de charger le formulaire");
            error.setContentText("Erreur : " + errorMsg + "\n\nVérifiez la console pour plus de détails.");
            if (window != null) {
                error.initOwner(window);
            }
            error.showAndWait();
            return null;
        }
        return dialog;
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ---------- Tâches ----------
    @FXML
    private void onAjouterTache() {
        Dialog<Tache> d = creerDialogTache("Nouvelle tâche", null);
        if (d == null) return;
        d.showAndWait().filter(t -> t != null).ifPresent(t -> {
            try {
                tacheService.add(t);
                chargerTaches();
            } catch (Exception e) {
                e.printStackTrace();
                showAlertErreur(getDialogOwner(), e);
            }
        });
    }

    @FXML
    private void onModifierTache() {
        ObservableList<Tache> sel = tacheTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner une tâche à modifier."); return; }
        if (sel.size() > 1) { showInfo("Veuillez sélectionner une seule tâche."); return; }
        Tache t = sel.get(0);
        Dialog<Tache> d = creerDialogTache("Modifier la tâche", t);
        if (d == null) return;
        d.showAndWait().filter(x -> x != null).ifPresent(x -> {
            x.setId(t.getId());
            tacheService.update(x);
            chargerTaches();
        });
    }

    @FXML
    private void onSupprimerTache() {
        ObservableList<Tache> sel = tacheTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner une ou plusieurs tâches à supprimer."); return; }
        int n = sel.size();
        String msg = n == 1 ? "Supprimer la tâche \"" + sel.get(0).getNom() + "\" ?" : "Supprimer les " + n + " tâches sélectionnées ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            sel.forEach(t -> tacheService.delete(t.getId()));
            chargerTaches();
        }
    }

    @FXML
    private void onConsulterTache() {
        ObservableList<Tache> sel = tacheTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner une tâche à consulter."); return; }
        if (sel.size() > 1) { showInfo("Veuillez sélectionner une seule tâche."); return; }
        Tache t = sel.get(0);
        String detail = String.format("Nom : %s\nDeadline : %s\nNotes : %s\nStatut : %s",
                nullToEmpty(t.getNom()), formatDate(t.getDeadline()), nullToEmpty(t.getNotes()),
                t.getStatut() != null ? t.getStatut().name() : "");
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détail de la tâche");
        info.setHeaderText(t.getNom());
        info.setContentText(detail);
        info.showAndWait();
    }

    private Dialog<Tache> creerDialogTache(String titre, Tache initial) {
        Dialog<Tache> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window window = getDialogOwner();
        if (window != null) dialog.initOwner(window);
        dialog.getDialogPane().setMinWidth(520);
        dialog.getDialogPane().setMinHeight(420);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/harmony/tache-form.fxml"));
            Parent form = loader.load();
            TacheFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/com/example/harmony/styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.addEventFilter(ActionEvent.ACTION, e -> { if (!ctrl.validate()) e.consume(); });
            dialog.setResultConverter(btn -> btn == ButtonType.OK ? ctrl.buildTache(initial != null ? initial.getId() : 0) : null);
        } catch (Exception e) {
            e.printStackTrace();
            showAlertErreur(window, e);
            return null;
        }
        return dialog;
    }

    private void showAlertErreur(Window window, Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) msg = e.getClass().getName();
        if (e.getCause() != null) msg += " - " + e.getCause().getMessage();
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle("Erreur");
        err.setHeaderText("Impossible de charger le formulaire");
        err.setContentText("Erreur : " + msg);
        if (window != null) err.initOwner(window);
        err.showAndWait();
    }
}
