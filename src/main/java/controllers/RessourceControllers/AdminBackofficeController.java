package controllers.RessourceControllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Window;
import models.RessourceModels.Evenement;
import models.RessourceModels.Salle;
import models.RessourceModels.StatutDemandeSalle;
import models.RessourceModels.Tache;
import models.RessourceModels.StatutTache;
import services.RessourceServices.AdminNotificationApiService;
import services.RessourceServices.PublicStatsApiService;
import services.RessourceServices.EvenementService;
import services.RessourceServices.SalleService;
import services.RessourceServices.TacheService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.ResourceBundle;

public class AdminBackofficeController implements Initializable {

    @FXML private TabPane adminTabPane;
    @FXML private TextField searchField;
    @FXML private TableView<Salle> salleTable;
    @FXML private TableColumn<Salle, String> colSalleNom;
    @FXML private TableColumn<Salle, Number> colSalleCapacite;
    @FXML private TableColumn<Salle, String> colSalleEquipements;
    @FXML private TableColumn<Salle, String> colSalleDisponible;
    @FXML private TableColumn<Salle, String> colSalleDescription;

    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, String> colEventTitre;
    @FXML private TableColumn<Evenement, String> colEventDateDebut;
    @FXML private TableColumn<Evenement, String> colEventDateFin;
    @FXML private TableColumn<Evenement, String> colEventLieu;
    @FXML private TableColumn<Evenement, String> colEventSalle;
    @FXML private TableColumn<Evenement, String> colEventStatut;
    @FXML private TableColumn<Evenement, String> colEventType;

    @FXML private TableView<Tache> tachesTable;
    @FXML private TableColumn<Tache, String> colTacheNom;
    @FXML private TableColumn<Tache, String> colTacheDeadline;
    @FXML private TableColumn<Tache, String> colTacheStatut;
    @FXML private Button btnAjouterTache;
    @FXML private Button btnModifierTache;
    @FXML private Button btnSupprimerTache;

    @FXML private TableView<Evenement> demandesTable;
    @FXML private TableColumn<Evenement, String> colDemandeTitre;
    @FXML private TableColumn<Evenement, String> colDemandeDateDebut;
    @FXML private TableColumn<Evenement, String> colDemandeDateFin;
    @FXML private TableColumn<Evenement, String> colDemandeSalle;
    @FXML private TableColumn<Evenement, String> colDemandeType;
    @FXML private Label demandeNotificationLabel;
    @FXML private Label labelExternalStats;
    @FXML private Button btnModifierSalle;
    @FXML private Button btnSupprimerSalle;
    @FXML private Button btnModifierEvent;
    @FXML private Button btnSupprimerEvent;
    @FXML private Button btnAccepterDemande;
    @FXML private Button btnRefuserDemande;

    private final SalleService salleService = new SalleService();
    private final EvenementService evenementService = new EvenementService();
    private final TacheService tacheService = new TacheService();
    private final AdminNotificationApiService adminNotificationApiService = new AdminNotificationApiService();
    private final PublicStatsApiService publicStatsApiService = new PublicStatsApiService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private ObservableList<Salle> sallesData = FXCollections.observableArrayList();
    private FilteredList<Salle> sallesFiltrees;
    private ObservableList<Evenement> eventsData = FXCollections.observableArrayList();
    private FilteredList<Evenement> eventsFiltrees;
    private ObservableList<Tache> tachesData = FXCollections.observableArrayList();
    private FilteredList<Tache> tachesFiltrees;
    private ObservableList<Evenement> demandesData = FXCollections.observableArrayList();
    private FilteredList<Evenement> demandesFiltrees;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnesSalle();
        configurerColonnesEvent();
        configurerColonnesDemandes();
        configurerColonnesTache();
        refreshAllTabs();
        sallesFiltrees = new FilteredList<>(sallesData, p -> true);
        SortedList<Salle> sallesSorted = new SortedList<>(sallesFiltrees);
        sallesSorted.comparatorProperty().bind(salleTable.comparatorProperty());
        salleTable.setItems(sallesSorted);
        eventsFiltrees = new FilteredList<>(eventsData, p -> true);
        SortedList<Evenement> eventsSorted = new SortedList<>(eventsFiltrees);
        eventsSorted.comparatorProperty().bind(eventsTable.comparatorProperty());
        eventsTable.setItems(eventsSorted);
        tachesFiltrees = new FilteredList<>(tachesData, p -> true);
        SortedList<Tache> tachesSorted = new SortedList<>(tachesFiltrees);
        tachesSorted.comparatorProperty().bind(tachesTable.comparatorProperty());
        tachesTable.setItems(tachesSorted);
        demandesFiltrees = new FilteredList<>(demandesData, p -> true);
        SortedList<Evenement> demandesSorted = new SortedList<>(demandesFiltrees);
        demandesSorted.comparatorProperty().bind(demandesTable.comparatorProperty());
        demandesTable.setItems(demandesSorted);

        Label emptySalles = new Label("Aucun résultat trouvé");
        emptySalles.setStyle("-fx-font-size:14px; -fx-text-fill:#9CA3AF; -fx-padding:40px;");
        Label emptyTaches = new Label("Aucun résultat trouvé");
        emptyTaches.setStyle("-fx-font-size:14px; -fx-text-fill:#9CA3AF; -fx-padding:40px;");
        Label emptyEvents = new Label("Aucun résultat trouvé");
        emptyEvents.setStyle("-fx-font-size:14px; -fx-text-fill:#9CA3AF; -fx-padding:40px;");
        Label emptyDemandes = new Label("Aucun résultat trouvé");
        emptyDemandes.setStyle("-fx-font-size:14px; -fx-text-fill:#9CA3AF; -fx-padding:40px;");
        if (salleTable != null) {
            salleTable.setPlaceholder(emptySalles);
            salleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (tachesTable != null) {
            tachesTable.setPlaceholder(emptyTaches);
            tachesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (eventsTable != null) {
            eventsTable.setPlaceholder(emptyEvents);
            eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (demandesTable != null) {
            demandesTable.setPlaceholder(emptyDemandes);
            demandesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        bindSelectionToButtons(salleTable, btnModifierSalle, btnSupprimerSalle);
        bindSelectionToButtons(eventsTable, btnModifierEvent, btnSupprimerEvent);
        bindDemandesSelectionToButtons();
        setupTachesTableModify();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String lower = (newVal == null ? "" : newVal.trim().toLowerCase());
                int idx = adminTabPane != null ? adminTabPane.getSelectionModel().getSelectedIndex() : 0;
                switch (idx) {
                    case 0 -> sallesFiltrees.setPredicate(s -> {
                        if (lower.isEmpty()) return true;
                        return nullToEmpty(s.getNom()).toLowerCase().contains(lower)
                            || nullToEmpty(s.getDescription()).toLowerCase().contains(lower)
                            || nullToEmpty(s.getEquipements()).toLowerCase().contains(lower)
                            || String.valueOf(s.getCapacite()).contains(lower);
                    });
                    case 1 -> tachesFiltrees.setPredicate(t -> {
                        if (lower.isEmpty()) return true;
                        return nullToEmpty(t.getNom()).toLowerCase().contains(lower)
                            || nullToEmpty(t.getNotes()).toLowerCase().contains(lower)
                            || (t.getStatut() != null && t.getStatut().name().toLowerCase().contains(lower));
                    });
                    case 2 -> eventsFiltrees.setPredicate(e -> {
                        if (lower.isEmpty()) return true;
                        return nullToEmpty(e.getTitre()).toLowerCase().contains(lower)
                            || nullToEmpty(e.getDescription()).toLowerCase().contains(lower)
                            || nullToEmpty(e.getLieu()).toLowerCase().contains(lower)
                            || (e.getType() != null && e.getType().name().toLowerCase().contains(lower));
                    });
                    case 3 -> demandesFiltrees.setPredicate(d -> {
                        if (lower.isEmpty()) return true;
                        return nullToEmpty(d.getTitre()).toLowerCase().contains(lower)
                            || (d.getStatutDemandeSalle() != null && d.getStatutDemandeSalle().name().toLowerCase().contains(lower));
                    });
                }
            });
        }
        if (adminTabPane != null) {
            adminTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
                if (searchField != null) searchField.clear();
            });
            adminTabPane.getSelectionModel().selectedItemProperty().addListener((o, oldTab, newTab) -> {
                if (newTab == null) return;
                String text = newTab.getText();
                if (text != null) {
                    if (text.contains("Événements")) chargerEvents();
                    else if (text.contains("Demandes")) chargerDemandes();
                    else if (text.contains("Tâches")) chargerTaches();
                    else if (text.contains("Salles")) chargerSalles();
                }
            });
        }
        loadExternalStats();
    }

    private void bindSelectionToButtons(TableView<?> table, Button btnModifier, Button btnSupprimer) {
        if (table == null) return;
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (btnModifier != null) btnModifier.setDisable(n == null);
            if (btnSupprimer != null) btnSupprimer.setDisable(n == null);
        });
        if (btnModifier != null) btnModifier.setDisable(true);
        if (btnSupprimer != null) btnSupprimer.setDisable(true);
    }

    private void bindDemandesSelectionToButtons() {
        if (demandesTable == null) return;
        demandesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (btnAccepterDemande != null) btnAccepterDemande.setDisable(n == null);
            if (btnRefuserDemande != null) btnRefuserDemande.setDisable(n == null);
        });
        if (btnAccepterDemande != null) btnAccepterDemande.setDisable(true);
        if (btnRefuserDemande != null) btnRefuserDemande.setDisable(true);
    }

    private void setupTachesTableModify() {
        if (tachesTable == null) return;
        tachesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tachesTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Tache selected = tachesTable.getSelectionModel().getSelectedItem();
                if (selected != null) showTacheDialog(selected);
            }
        });
    }

    private void refreshAllTabs() {
        sallesData.setAll(salleService.getAll());
        eventsData.setAll(evenementService.getAll());
        tachesData.setAll(tacheService.getAll());
        demandesData.setAll(evenementService.getDemandesEnAttente());
        majNotificationDemandes();
        if (searchField != null) searchField.clear();
    }

    private void loadExternalStats() {
        if (labelExternalStats == null) return;
        publicStatsApiService.getExternalInfo().thenAccept(info -> {
            Platform.runLater(() -> {
                if (labelExternalStats != null) labelExternalStats.setText("Info externe : " + info);
            });
        });
    }

    private void configurerColonnesSalle() {
        colSalleNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colSalleCapacite.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacite()));
        colSalleEquipements.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEquipements() != null ? cell.getValue().getEquipements() : ""));
        colSalleDisponible.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isDisponible() ? "Oui" : "Non"));
        colSalleDisponible.setCellFactory(col -> new TableCell<Salle, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean dispo = item.equalsIgnoreCase("true") || item.equalsIgnoreCase("oui") || "1".equals(item);
                Label badge = new Label(dispo ? "✓  Disponible" : "✕  Indisponible");
                badge.setPadding(new Insets(4, 12, 4, 12));
                badge.setStyle("-fx-background-radius: 999px; -fx-font-size: 11px; -fx-font-weight: bold;"
                    + (dispo ? "-fx-background-color:rgba(16,185,129,0.12); -fx-text-fill:#10B981;"
                              : "-fx-background-color:rgba(239,68,68,0.1); -fx-text-fill:#EF4444;"));
                setGraphic(badge);
                setText(null);
            }
        });
        colSalleDescription.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription() != null ? cell.getValue().getDescription() : ""));
    }

    private void configurerColonnesEvent() {
        if (eventsTable == null) return;
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

    private static LocalDate dateToLocalDate(java.util.Date date) {
        if (date == null) return null;
        if (date instanceof java.sql.Date)
            return ((java.sql.Date) date).toLocalDate();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void configurerColonnesDemandes() {
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

    private void configurerColonnesTache() {
        if (tachesTable == null) return;
        if (colTacheNom != null) colTacheNom.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getNom())));
        if (colTacheDeadline != null) colTacheDeadline.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeadline() != null ? DATE_FORMAT.format(cell.getValue().getDeadline()) : ""));
        if (colTacheStatut != null) {
            colTacheStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatut() != null ? cell.getValue().getStatut().name() : ""));
            colTacheStatut.setCellFactory(col -> new TableCell<Tache, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    String val = item;
                    Label badge = new Label();
                    badge.setPadding(new Insets(4, 12, 4, 12));
                    badge.setStyle("-fx-background-radius: 999px; -fx-font-size: 11px; -fx-font-weight: bold;");
                    switch (val) {
                        case "A_FAIRE" -> {
                            badge.setText("À faire");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(245,158,11,0.12); -fx-text-fill: #F59E0B;");
                        }
                        case "EN_COURS" -> {
                            badge.setText("En cours");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(124,58,237,0.12); -fx-text-fill: #7C3AED;");
                        }
                        case "TERMINEE" -> {
                            badge.setText("Terminée");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(16,185,129,0.12); -fx-text-fill: #10B981;");
                        }
                        default -> badge.setText(val);
                    }
                    setGraphic(badge);
                    setText(null);
                }
            });
        }
    }

    private void chargerSalles() {
        sallesData.setAll(salleService.getAll());
    }

    private void chargerEvents() {
        if (eventsData != null) eventsData.setAll(evenementService.getAll());
    }

    private void chargerDemandes() {
        demandesData.setAll(evenementService.getDemandesEnAttente());
        majNotificationDemandes();
    }

    private void chargerTaches() {
        tachesData.setAll(tacheService.getAll());
    }

    @FXML
    private void handleAjouterTache() {
        showTacheDialog(null);
    }

    @FXML
    private void handleModifierTache() {
        if (tachesTable == null) return;
        Tache selected = tachesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Veuillez sélectionner une tâche à modifier.");
            return;
        }
        showTacheDialog(selected);
    }

    @FXML
    private void handleSupprimerTache() {
        Tache selected = tachesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Veuillez sélectionner une tâche à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer la tâche ?");
        confirm.setContentText("« " + (selected.getNom() != null ? selected.getNom() : "") + " »\nCette action est irréversible.");
        if (getDialogOwner() != null) confirm.initOwner(getDialogOwner());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tacheService.delete(selected.getId());
                    refreshAllTabs();
                    showInfo("Tâche supprimée avec succès.");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    private void showWarning(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        if (getDialogOwner() != null) a.initOwner(getDialogOwner());
        a.showAndWait();
    }

    private void showTacheDialog(Tache tache) {
        boolean isEdit = (tache != null);
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setStyle("-fx-padding: 20px;");
        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints(120);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        col1.setFillWidth(true);
        grid.getColumnConstraints().addAll(col0, col1);
        Label lblNom = new Label("Nom *");
        lblNom.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-weight:bold;");
        TextField fieldNom = new TextField();
        fieldNom.setPromptText("Nom de la tâche");
        fieldNom.setStyle("-fx-background-radius:10px; -fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10px; -fx-padding:10px 14px;");
        fieldNom.setMaxWidth(Double.MAX_VALUE);
        Label lblDeadline = new Label("Deadline");
        lblDeadline.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-weight:bold;");
        javafx.scene.control.DatePicker fieldDeadline = new javafx.scene.control.DatePicker();
        fieldDeadline.setStyle("-fx-background-radius:10px; -fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10px;");
        fieldDeadline.setMaxWidth(Double.MAX_VALUE);
        Label lblStatut = new Label("Statut *");
        lblStatut.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-weight:bold;");
        javafx.scene.control.ComboBox<String> fieldStatut = new javafx.scene.control.ComboBox<>();
        fieldStatut.getItems().addAll("A_FAIRE", "EN_COURS", "TERMINEE");
        fieldStatut.setMaxWidth(Double.MAX_VALUE);
        fieldStatut.setStyle("-fx-background-radius:10px; -fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10px;");
        Label lblNotes = new Label("Notes");
        lblNotes.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-weight:bold;");
        javafx.scene.control.TextArea fieldNotes = new javafx.scene.control.TextArea();
        fieldNotes.setPromptText("Notes (optionnel)");
        fieldNotes.setPrefRowCount(3);
        fieldNotes.setWrapText(true);
        fieldNotes.setStyle("-fx-background-radius:10px; -fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10px;");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:#EF4444; -fx-font-size:12px;");
        errorLabel.setVisible(false);
        grid.add(lblNom, 0, 0); grid.add(fieldNom, 1, 0);
        grid.add(lblDeadline, 0, 1); grid.add(fieldDeadline, 1, 1);
        grid.add(lblStatut, 0, 2); grid.add(fieldStatut, 1, 2);
        grid.add(lblNotes, 0, 3); grid.add(fieldNotes, 1, 3);
        grid.add(errorLabel, 1, 4);
        if (isEdit) {
            fieldNom.setText(tache.getNom() != null ? tache.getNom() : "");
            if (tache.getDeadline() != null)
                fieldDeadline.setValue(dateToLocalDate(tache.getDeadline()));
            fieldStatut.setValue(tache.getStatut() != null ? tache.getStatut().name() : "A_FAIRE");
            fieldNotes.setText(tache.getNotes() != null ? tache.getNotes() : "");
        } else {
            fieldStatut.setValue("A_FAIRE");
        }
        Dialog<Tache> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Modifier la tâche" : "Nouvelle tâche");
        dialog.setHeaderText(isEdit ? "Modifier « " + (tache.getNom() != null ? tache.getNom() : "") + " »" : "Créer une nouvelle tâche");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getStyleClass().add("backoffice-dialog");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        ButtonType saveBtn = new ButtonType(isEdit ? "✎  Enregistrer" : "＋  Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        if (saveButton != null) {
            saveButton.setStyle("-fx-background-color: linear-gradient(to bottom right,#7C3AED,#6D28D9); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 22px;");
            saveButton.setDisable(true);
            fieldNom.textProperty().addListener((obs, o, n) -> saveButton.setDisable(n == null || n.trim().isEmpty()));
            saveButton.addEventFilter(ActionEvent.ACTION, e -> {
                errorLabel.setVisible(false);
                if (fieldNom.getText() == null || fieldNom.getText().trim().isEmpty()) {
                    errorLabel.setText("Le nom est obligatoire.");
                    errorLabel.setVisible(true);
                    e.consume();
                    return;
                }
                if (fieldStatut.getValue() == null) {
                    errorLabel.setText("Le statut est obligatoire.");
                    errorLabel.setVisible(true);
                    e.consume();
                    return;
                }
            });
        }
        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            Tache t = isEdit ? tache : new Tache();
            if (isEdit) t.setId(tache.getId());
            t.setNom(fieldNom.getText().trim());
            t.setDeadline(fieldDeadline.getValue() != null ? java.util.Date.from(fieldDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null);
            t.setStatut(StatutTache.valueOf(fieldStatut.getValue()));
            String notes = fieldNotes.getText().trim();
            t.setNotes(notes.isEmpty() ? "" : notes);
            return t;
        });
        if (getDialogOwner() != null) dialog.initOwner(getDialogOwner());
        dialog.showAndWait().ifPresent(result -> {
            try {
                if (isEdit) {
                    tacheService.update(result);
                    showInfo("Tâche modifiée avec succès.");
                } else {
                    tacheService.add(result);
                    showInfo("Tâche ajoutée avec succès.");
                }
                refreshAllTabs();
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        });
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RessourceViews/event-form.fxml"));
            Parent form = loader.load();
            EventFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/views/RessourceViews/ressource-styles.css");
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
                adminNotificationApiService.notifyDemandProcessed(e.getTitre(), salle.getNom(), true);
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
                    Salle salle = e.getSalleId() != null ? salleService.getById(e.getSalleId()) : null;
                    e.setStatutDemandeSalle(StatutDemandeSalle.REFUSE);
                    evenementService.update(e);
                    adminNotificationApiService.notifyDemandProcessed(e.getTitre(), salle != null ? salle.getNom() : "?", false);
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
                showInfo("Salle ajoutée.");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RessourceViews/salle-form.fxml"));
            Parent form = loader.load();
            SalleFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/views/RessourceViews/ressource-styles.css");
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
