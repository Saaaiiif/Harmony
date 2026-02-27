package controllers.ActiviteControllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import models.ActiviteModels.Sommeil;
import services.ActiviteServices.ServiceSommeil;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class JournalSommeilController {

    // ── MODIFICATION: Propriété AccueilController pour navigation ──
    private controllers.UserControlleers.modifControl.AccueilController accueilController;

    public void setAccueilController(controllers.UserControlleers.modifControl.AccueilController controller) {
        this.accueilController = controller;
    }


    // --- FORMULAIRE ---
    @FXML private DatePicker dateCoucherPicker;
    @FXML private Spinner<Integer> spinnerCoucherH;
    @FXML private Spinner<Integer> spinnerCoucherM;

    @FXML private DatePicker dateReveilPicker;
    @FXML private Spinner<Integer> spinnerReveilH;
    @FXML private Spinner<Integer> spinnerReveilM;

    @FXML private ComboBox<String> qualiteCombo;
    @FXML private CheckBox checkStress, checkCafeine, checkBruit;
    @FXML private Label erreurLabel;

    @FXML private Button btnEnregistrer;
    @FXML private Button btnSupprimer;

    // --- GRAPHIQUE NÉON ---
    @FXML private AreaChart<String, Number> sommeilChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private ServiceSommeil serviceSommeil = new ServiceSommeil();
    private Sommeil nuitEnCoursDeModification = null;

    @FXML
    public void initialize() {
        // 1. Initialiser le ComboBox Qualité
        qualiteCombo.setItems(FXCollections.observableArrayList(
                "Excellent", "Bon", "Moyen", "Mauvais", "Très mauvais"
        ));

        // 2. Initialiser les Spinners (Flèches pour les heures et minutes)
        spinnerCoucherH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 22));
        spinnerCoucherM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spinnerReveilH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 7));
        spinnerReveilM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Cacher le bouton supprimer par défaut
        btnSupprimer.setVisible(false);

        // 3. Charger la courbe
        actualiserGraphique();
    }

    private void actualiserGraphique() {
        sommeilChart.getData().clear(); // On vide le graphique
        List<Sommeil> liste = serviceSommeil.afficherTout();

        if (liste == null || liste.isEmpty()) return;

        // Trier chronologiquement pour que la courbe ait du sens de gauche à droite
        liste.sort(Comparator.comparing(Sommeil::getDate_coucher));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Durée de sommeil (Heures)");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (Sommeil s : liste) {
            LocalDateTime coucher = s.getDate_coucher().toLocalDateTime();
            LocalDateTime reveil = s.getDate_reveil().toLocalDateTime();

            // Calcul de la durée exacte
            double minutes = java.time.Duration.between(coucher, reveil).toMinutes();
            double heures = minutes / 60.0;

            // Étiquette de l'axe X (ex: 14/05 -> 15/05)
            String dateStr = coucher.format(formatter) + " -> " + reveil.format(formatter);

            XYChart.Data<String, Number> data = new XYChart.Data<>(dateStr, heures);

            // Rendre chaque point interactif (clic = modification)
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-cursor: hand;");
                    Tooltip.install(newNode, new Tooltip("Dormi : " + String.format("%.1f", heures) + "h\nCliquez pour modifier !"));
                    newNode.setOnMouseClicked(e -> chargerPourModification(s));
                }
            });

            series.getData().add(data);
        }

        sommeilChart.getData().add(series);
    }

    @FXML
    void enregistrerNuit() {
        LocalDate dateC = dateCoucherPicker.getValue();
        LocalDate dateR = dateReveilPicker.getValue();

        if (dateC == null || dateR == null || qualiteCombo.getValue() == null) {
            erreurLabel.setText("Veuillez remplir les dates et la qualité.");
            return;
        }

        // Récupérer les valeurs des Spinners
        LocalTime timeCoucher = LocalTime.of(spinnerCoucherH.getValue(), spinnerCoucherM.getValue());
        LocalTime timeReveil = LocalTime.of(spinnerReveilH.getValue(), spinnerReveilM.getValue());

        LocalDateTime ldtCoucher = LocalDateTime.of(dateC, timeCoucher);
        LocalDateTime ldtReveil = LocalDateTime.of(dateR, timeReveil);

        // CONTROLE DE SAISIE : Le réveil doit être APRÈS le coucher (min 1 minute)
        if (!ldtReveil.isAfter(ldtCoucher)) {
            erreurLabel.setText("Erreur : Le réveil doit être strictement après le coucher.");
            return;
        }

        Sommeil s = new Sommeil();
        if (nuitEnCoursDeModification != null) {
            s.setId_sommeil(nuitEnCoursDeModification.getId_sommeil());
        }

        s.setDate_coucher(Timestamp.valueOf(ldtCoucher));
        s.setDate_reveil(Timestamp.valueOf(ldtReveil));
        s.setQualite_sommeil(formatQualiteForDB(qualiteCombo.getValue()));
        s.setStress(checkStress.isSelected());
        s.setCafeine(checkCafeine.isSelected());
        s.setBruit(checkBruit.isSelected());

        if (nuitEnCoursDeModification == null) {
            boolean ok = serviceSommeil.ajouter(s);
            if (!ok) erreurLabel.setText("Erreur lors de l'ajout en base.");
        } else {
            boolean ok = serviceSommeil.modifier(s);
            if (!ok) erreurLabel.setText("Erreur lors de la modification.");
        }

        reinitialiserFormulaire();
        actualiserGraphique(); // Met à jour la courbe
    }

    private void chargerPourModification(Sommeil s) {
        nuitEnCoursDeModification = s;

        LocalDateTime c = s.getDate_coucher().toLocalDateTime();
        dateCoucherPicker.setValue(c.toLocalDate());
        spinnerCoucherH.getValueFactory().setValue(c.getHour());
        spinnerCoucherM.getValueFactory().setValue(c.getMinute());

        LocalDateTime r = s.getDate_reveil().toLocalDateTime();
        dateReveilPicker.setValue(r.toLocalDate());
        spinnerReveilH.getValueFactory().setValue(r.getHour());
        spinnerReveilM.getValueFactory().setValue(r.getMinute());

        qualiteCombo.setValue(formatQualiteForUI(s.getQualite_sommeil()));
        checkStress.setSelected(s.isStress());
        checkCafeine.setSelected(s.isCafeine());
        checkBruit.setSelected(s.isBruit());

        btnEnregistrer.setText("Mettre à jour");
        btnSupprimer.setVisible(true); // Fait apparaître le bouton supprimer
        erreurLabel.setText("");
    }

    @FXML
    void supprimerNuit() {
        if (nuitEnCoursDeModification != null) {
            serviceSommeil.supprimer(nuitEnCoursDeModification.getId_sommeil());
            reinitialiserFormulaire();
            actualiserGraphique();
        }
    }

    @FXML
    void reinitialiserFormulaire() {
        nuitEnCoursDeModification = null;
        dateCoucherPicker.setValue(null);
        spinnerCoucherH.getValueFactory().setValue(22);
        spinnerCoucherM.getValueFactory().setValue(0);

        dateReveilPicker.setValue(null);
        spinnerReveilH.getValueFactory().setValue(7);
        spinnerReveilM.getValueFactory().setValue(0);

        qualiteCombo.setValue(null);
        checkStress.setSelected(false);
        checkCafeine.setSelected(false);
        checkBruit.setSelected(false);

        btnEnregistrer.setText("Enregistrer Sommeil");
        btnSupprimer.setVisible(false);
        erreurLabel.setText("");
    }

    private String formatQualiteForDB(String uiValue) {
        if (uiValue == null) return "MOYEN";
        switch (uiValue) {
            case "Excellent": return "EXCELLENT";
            case "Bon": return "BON";
            case "Mauvais": return "MAUVAIS";
            case "Très mauvais": return "TRES_MAUVAIS";
            default: return "MOYEN";
        }
    }

    private String formatQualiteForUI(String dbValue) {
        if (dbValue == null) return "Moyen";
        switch (dbValue) {
            case "EXCELLENT": return "Excellent";
            case "BON": return "Bon";
            case "MAUVAIS": return "Mauvais";
            case "TRES_MAUVAIS": return "Très mauvais";
            default: return "Moyen";
        }
    }

    // --- NAVIGATION UNIFIÉE (LA CORRECTION EST ICI) ---
    @FXML void goToAccueil(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { /* On est déjà sur la page Sommeil, on ne fait rien pour éviter le rechargement infini */ }
}