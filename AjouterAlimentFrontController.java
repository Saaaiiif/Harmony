package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Aliment;
import models.Consommation;
import services.ServiceAliment;
import services.ServiceConsommation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

public class AjouterAlimentFrontController {

    @FXML private Label labelTitreRepas;
    @FXML private TextField tfRecherche;
    @FXML private ListView<Aliment> listeResultats;
    @FXML private TextField tfQuantiteGrammes;
    @FXML private Label erreurLabel;

    private ServiceAliment serviceAliment = new ServiceAliment();
    private ServiceConsommation serviceConsommation = new ServiceConsommation();
    private ObservableList<Aliment> tousLesAliments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        erreurLabel.setText("");

        String titreJoli = "Repas";
        switch(JournalAlimentaireController.repasSelectionne) {
            case JournalAlimentaireController.CODE_PD: titreJoli = "🌅 Petit déjeuner"; break;
            case JournalAlimentaireController.CODE_DEJ: titreJoli = "☀️ Déjeuner"; break;
            case JournalAlimentaireController.CODE_DIN: titreJoli = "🌙 Dîner"; break;
            case JournalAlimentaireController.CODE_SNACK: titreJoli = "🍪 Snacks"; break;
        }
        labelTitreRepas.setText("Cible : " + titreJoli);

        List<Aliment> alimentsDB = serviceAliment.afficherTout();
        tousLesAliments.addAll(alimentsDB);

        listeResultats.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Aliment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    setText(null);
                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);
                    Label icon = new Label("🍽");
                    icon.setStyle("-fx-font-size: 22px;");

                    VBox texts = new VBox(3);
                    Label name = new Label(item.getNom_aliment());
                    name.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 15px;");

                    HBox macros = new HBox(10);
                    Label c = new Label("🔥 " + item.getCalories_pour_100g() + " kcal"); c.setStyle("-fx-text-fill: #6a1b9a; -fx-font-weight: bold;");
                    Label p = new Label(String.format(Locale.US, "P: %.1fg", item.getProteines())); p.setStyle("-fx-text-fill: #c0392b;");
                    Label g = new Label(String.format(Locale.US, "G: %.1fg", item.getGlucides())); g.setStyle("-fx-text-fill: #2980b9;");
                    Label l = new Label(String.format(Locale.US, "L: %.1fg", item.getLipides())); l.setStyle("-fx-text-fill: #d35400;");
                    macros.getChildren().addAll(c, p, g, l);

                    texts.getChildren().addAll(name, macros);
                    root.getChildren().addAll(icon, texts);
                    setGraphic(root);
                }
            }
        });

        // Recherche dynamique par les premières lettres
        FilteredList<Aliment> filteredData = new FilteredList<>(tousLesAliments, p -> true);
        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(aliment -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return aliment.getNom_aliment().toLowerCase().startsWith(newValue.toLowerCase());
            });
        });
        listeResultats.setItems(filteredData);
    }

    @FXML
    void validerAjout(ActionEvent event) {
        erreurLabel.setText("");
        Aliment alimentChoisi = listeResultats.getSelectionModel().getSelectedItem();

        if (alimentChoisi == null) {
            erreurLabel.setText("⚠️ Veuillez d'abord sélectionner un aliment dans la liste.");
            return;
        }

        try {
            // ✅ CORRECTION ICI : L'étudiant peut taper "150,5" ou "150.5", ça ne crash pas, et ça s'arrondit à 151g pour la BD.
            double quantiteSaisie = Double.parseDouble(tfQuantiteGrammes.getText().trim().replace(",", "."));
            int grammes = (int) Math.round(quantiteSaisie);

            if (grammes <= 0) {
                erreurLabel.setText("⚠️ La quantité doit être supérieure à 0.");
                return;
            }

            Consommation c = new Consommation();
            c.setId_aliment(alimentChoisi.getId_aliment());
            c.setType_repas(JournalAlimentaireController.repasSelectionne);
            c.setPoids_grammes(grammes);
            c.setQuantite_eau_ml(0);

            // Synchronisation avec la date sélectionnée
            LocalDateTime ldt = JournalAlimentaireController.dateSelectionnee.atTime(LocalTime.now());
            c.setDate_consommation(Timestamp.valueOf(ldt));

            serviceConsommation.ajouter(c);
            goToAliments(null);

        } catch (NumberFormatException e) {
            erreurLabel.setText("❌ Veuillez entrer un nombre valide (ex: 150 ou 150,5).");
        }
    }

    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
}