package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import models.NuitSommeil;

public class JournalSommeilController {

    @FXML private CheckBox checkBruit;
    @FXML private CheckBox checkCafeine;
    @FXML private CheckBox checkStress;

    @FXML private TableView<NuitSommeil> sommeilTable;
    @FXML private TableColumn<NuitSommeil, String> colCoucher;
    @FXML private TableColumn<NuitSommeil, String> colReveil;
    @FXML private TableColumn<NuitSommeil, String> colQualite;
    @FXML private TableColumn<NuitSommeil, String> colFacteurs;
    @FXML private TableColumn<NuitSommeil, Void> colActions;

    @FXML private DatePicker dateCoucherPicker;
    @FXML private DatePicker dateReveilPicker;
    @FXML private TextField heureCoucherField;
    @FXML private TextField heureReveilField;

    @FXML private Label erreurLabel;
    @FXML private ComboBox<String> qualiteCombo;
    @FXML private Button btnEnregistrer;

    private ObservableList<NuitSommeil> listeNuits = FXCollections.observableArrayList();

    // Variable pour savoir si on est en train de modifier une ligne existante
    private NuitSommeil nuitEnCoursDeModification = null;

    @FXML
    public void initialize() {
        qualiteCombo.getItems().addAll("Excellente", "Bonne", "Moyenne", "Mauvaise", "Très mauvaise");

        // 1. Lier les données aux colonnes avec les nouvelles méthodes du modèle
        colCoucher.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCoucherAffichage()));
        colReveil.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReveilAffichage()));
        colQualite.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getQualite()));
        colFacteurs.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFacteursAffichage()));

        // 2. Générer les boutons Modifier/Supprimer dans la colonne Actions
        setupActionsColumn();

        sommeilTable.setItems(listeNuits);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<NuitSommeil, Void>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("✖");
            private final HBox pane = new HBox(15, btnEdit, btnDelete);

            {
                // Design des boutons
                btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #007bff; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 16px;");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4c4c; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 16px;");
                pane.setAlignment(Pos.CENTER);

                // Action : Supprimer
                btnDelete.setOnAction(event -> {
                    NuitSommeil nuitSelectionnee = getTableView().getItems().get(getIndex());
                    listeNuits.remove(nuitSelectionnee);
                    erreurLabel.setText("Nuit supprimée !");
                    erreurLabel.setStyle("-fx-text-fill: #ff4c4c;");

                    if (nuitEnCoursDeModification == nuitSelectionnee) {
                        reinitialiserFormulaire();
                    }
                });

                // Action : Modifier
                btnEdit.setOnAction(event -> {
                    NuitSommeil nuitSelectionnee = getTableView().getItems().get(getIndex());
                    chargerNuitDansFormulaire(nuitSelectionnee);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void chargerNuitDansFormulaire(NuitSommeil nuit) {
        nuitEnCoursDeModification = nuit;

        dateCoucherPicker.setValue(nuit.getDateCoucher());
        heureCoucherField.setText(nuit.getHeureCoucher());
        dateReveilPicker.setValue(nuit.getDateReveil());
        heureReveilField.setText(nuit.getHeureReveil());
        qualiteCombo.setValue(nuit.getQualite());
        checkStress.setSelected(nuit.isStress());
        checkCafeine.setSelected(nuit.isCafeine());
        checkBruit.setSelected(nuit.isBruit());

        btnEnregistrer.setText("Mettre à jour cette nuit");
        btnEnregistrer.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 25; -fx-cursor: hand;");
        erreurLabel.setText("Modification en cours...");
        erreurLabel.setStyle("-fx-text-fill: #007bff;");
    }

    // Navigation (Assure-toi que les chemins de tes FXML sont corrects ici)
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    @FXML
    void enregistrerNuit(ActionEvent event) {
        if (dateCoucherPicker.getValue() == null || heureCoucherField.getText().isEmpty() ||
                dateReveilPicker.getValue() == null || heureReveilField.getText().isEmpty() ||
                qualiteCombo.getValue() == null) {
            erreurLabel.setText("Veuillez remplir tous les champs obligatoires.");
            erreurLabel.setStyle("-fx-text-fill: #ff4c4c;");
            return;
        }

        if (nuitEnCoursDeModification == null) {
            // MODE AJOUT (Création avec les 8 arguments)
            NuitSommeil nouvelleNuit = new NuitSommeil(
                    dateCoucherPicker.getValue(), heureCoucherField.getText(),
                    dateReveilPicker.getValue(), heureReveilField.getText(),
                    qualiteCombo.getValue(), checkStress.isSelected(),
                    checkCafeine.isSelected(), checkBruit.isSelected()
            );
            listeNuits.add(nouvelleNuit);
            erreurLabel.setText("Nuit ajoutée avec succès !");
            erreurLabel.setStyle("-fx-text-fill: #28a745;");
        } else {
            // MODE MODIFICATION
            nuitEnCoursDeModification.setDateCoucher(dateCoucherPicker.getValue());
            nuitEnCoursDeModification.setHeureCoucher(heureCoucherField.getText());
            nuitEnCoursDeModification.setDateReveil(dateReveilPicker.getValue());
            nuitEnCoursDeModification.setHeureReveil(heureReveilField.getText());
            nuitEnCoursDeModification.setQualite(qualiteCombo.getValue());
            nuitEnCoursDeModification.setStress(checkStress.isSelected());
            nuitEnCoursDeModification.setCafeine(checkCafeine.isSelected());
            nuitEnCoursDeModification.setBruit(checkBruit.isSelected());

            sommeilTable.refresh();
            erreurLabel.setText("Nuit modifiée avec succès !");
            erreurLabel.setStyle("-fx-text-fill: #28a745;");
        }

        reinitialiserFormulaire();
    }

    private void reinitialiserFormulaire() {
        nuitEnCoursDeModification = null;
        dateCoucherPicker.setValue(null);
        heureCoucherField.clear();
        dateReveilPicker.setValue(null);
        heureReveilField.clear();
        qualiteCombo.setValue(null);
        checkStress.setSelected(false);
        checkCafeine.setSelected(false);
        checkBruit.setSelected(false);

        btnEnregistrer.setText("Sauvegarder cette nuit");
        btnEnregistrer.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 25; -fx-cursor: hand;");
    }
}