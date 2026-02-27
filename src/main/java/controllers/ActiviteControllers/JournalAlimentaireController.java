package controllers.ActiviteControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.ActiviteModels.Aliment;
import models.ActiviteModels.Consommation;
import services.ActiviteServices.ServiceAliment;
import services.ActiviteServices.ServiceConsommation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JournalAlimentaireController {

    // ── Référence au contrôleur parent (front office) ────────────────────────
    private controllers.UserControlleers.modifControl.AccueilController accueilController;

    public void setAccueilController(
            controllers.UserControlleers.modifControl.AccueilController controller) {
        this.accueilController = controller;
    }

    // =========================================================================
    // CODES REPAS
    // =========================================================================
    public static final String CODE_PD    = "PETIT_DEJ";
    public static final String CODE_DEJ   = "DEJEUNER";
    public static final String CODE_DIN   = "DINER";
    public static final String CODE_SNACK = "SNACK";

    public static String    repasSelectionne = "";
    public static LocalDate dateSelectionnee = LocalDate.now();
    public static int       totalEauMl       = 0;

    // =========================================================================
    // CORRECTION ERREURS 207, 329, 644 :
    // AccueilActiviteController.OBJECTIF_CALORIES_JOURNALIER et
    // AccueilActiviteController.OBJECTIF_EAU_ML n'existent pas dans
    // AccueilActiviteController (constantes statiques publiques manquantes).
    //
    // SOLUTION : On définit ici des constantes locales avec les mêmes valeurs
    // par défaut. Ainsi le code compile sans dépendre d'une constante externe
    // inexistante. Si l'utilisateur modifie son objectif dans AccueilActivite,
    // ces valeurs restent des fallbacks raisonnables.
    // =========================================================================
    private static final int OBJECTIF_CALORIES_JOURNALIER_DEFAULT = 2150;
    private static final int OBJECTIF_EAU_ML_DEFAULT              = 2000;

    // =========================================================================
    // CHAMPS FXML — EXISTANTS
    // =========================================================================
    @FXML private DatePicker dpDateJournal;
    @FXML private VBox       vboxPetitDej, vboxDejeuner, vboxDiner, vboxSnacks;
    @FXML private Label      lblTotPD, lblTotDej, lblTotDin, lblTotSnack;
    @FXML private Label      lblDayCal, lblDayProt, lblDayGlu, lblDayLip;
    @FXML private ProgressBar waterBar;
    @FXML private Label      labelEau;
    @FXML private FlowPane   flowPaneHistoriqueAliment;

    // =========================================================================
    // CHAMPS FXML — BARRE DE PROGRESSION CALORIES
    // =========================================================================
    @FXML private ProgressBar progressCalories;
    @FXML private Label       lblCaloriesProgress;
    @FXML private Label       lblCaloriesRestantes;

    // =========================================================================
    // SERVICES & CACHE
    // =========================================================================
    private final ServiceConsommation serviceConsommation = new ServiceConsommation();
    private final ServiceAliment      serviceAliment      = new ServiceAliment();
    private final Map<Integer, Aliment> cacheAliments     = new HashMap<>();

    // SVG Icones
    private static final String SVG_EDIT =
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z" +
                    "M20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34" +
                    "c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private static final String SVG_DELETE =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z" +
                    "M19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @FXML
    public void initialize() {
        // Charger le cache des aliments
        for (Aliment a : serviceAliment.afficherTout()) {
            cacheAliments.put(a.getId_aliment(), a);
        }

        // DatePicker
        if (dpDateJournal != null) {
            dpDateJournal.setValue(dateSelectionnee);
            dpDateJournal.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    dateSelectionnee = newVal;
                    chargerRepasDuJour();
                }
            });
        }

        chargerRepasDuJour();
        chargerHistorique();
    }

    // =========================================================================
    // CHARGEMENT DES REPAS DU JOUR
    // =========================================================================
    private void chargerRepasDuJour() {
        if (vboxPetitDej != null) vboxPetitDej.getChildren().clear();
        if (vboxDejeuner  != null) vboxDejeuner.getChildren().clear();
        if (vboxDiner     != null) vboxDiner.getChildren().clear();
        if (vboxSnacks    != null) vboxSnacks.getChildren().clear();

        totalEauMl = 0;

        // Totaux journaliers
        double dayProt = 0, dayGlu = 0, dayLip = 0;
        int    dayCal  = 0;

        // Totaux par repas
        double pdProt  = 0, pdGlu  = 0, pdLip  = 0;  int pdCal  = 0;
        double dejProt = 0, dejGlu = 0, dejLip = 0;  int dejCal = 0;
        double dinProt = 0, dinGlu = 0, dinLip = 0;  int dinCal = 0;
        double snkProt = 0, snkGlu = 0, snkLip = 0;  int snkCal = 0;

        List<Consommation> toutes = serviceConsommation.afficherTout();

        for (Consommation c : toutes) {
            if (c.getDate_consommation() == null) continue;
            if (!c.getDate_consommation().toLocalDateTime()
                    .toLocalDate().equals(dateSelectionnee)) continue;

            totalEauMl += c.getQuantite_eau_ml();

            Aliment al = cacheAliments.get(c.getId_aliment());
            if (al == null) continue;

            double ratio = c.getPoids_grammes() / 100.0;
            int    cals  = (int) (al.getCalories_pour_100g() * ratio);
            double prot  = al.getProteines() * ratio;
            double glu   = al.getGlucides()  * ratio;
            double lip   = al.getLipides()   * ratio;

            dayCal  += cals;  dayProt += prot;
            dayGlu  += glu;   dayLip  += lip;

            HBox ligne = creerLigneAliment(c, al, cals, prot, glu, lip);
            String type = c.getType_repas();

            if (CODE_PD.equals(type)) {
                if (vboxPetitDej != null) vboxPetitDej.getChildren().add(ligne);
                pdCal  += cals;  pdProt  += prot;
                pdGlu  += glu;   pdLip   += lip;
            } else if (CODE_DEJ.equals(type)) {
                if (vboxDejeuner != null) vboxDejeuner.getChildren().add(ligne);
                dejCal += cals;  dejProt += prot;
                dejGlu += glu;   dejLip  += lip;
            } else if (CODE_DIN.equals(type)) {
                if (vboxDiner != null) vboxDiner.getChildren().add(ligne);
                dinCal += cals;  dinProt += prot;
                dinGlu += glu;   dinLip  += lip;
            } else if ("SNACK".equals(type) || "SNACKS".equals(type)) {
                if (vboxSnacks != null) vboxSnacks.getChildren().add(ligne);
                snkCal += cals;  snkProt += prot;
                snkGlu += glu;   snkLip  += lip;
            }
        }

        // Étiquettes par repas
        if (lblTotPD    != null) lblTotPD.setText(   formatMacros(pdCal,  pdProt,  pdGlu,  pdLip));
        if (lblTotDej   != null) lblTotDej.setText(  formatMacros(dejCal, dejProt, dejGlu, dejLip));
        if (lblTotDin   != null) lblTotDin.setText(  formatMacros(dinCal, dinProt, dinGlu, dinLip));
        if (lblTotSnack != null) lblTotSnack.setText(formatMacros(snkCal, snkProt, snkGlu, snkLip));

        // Bilan journalier
        if (lblDayCal  != null) lblDayCal.setText(String.valueOf(dayCal));
        if (lblDayProt != null) lblDayProt.setText(String.format(Locale.US, "%.1f", dayProt) + "g");
        if (lblDayGlu  != null) lblDayGlu.setText(String.format(Locale.US,  "%.1f", dayGlu)  + "g");
        if (lblDayLip  != null) lblDayLip.setText(String.format(Locale.US,  "%.1f", dayLip)  + "g");

        // Eau
        majAffichageEau();

        // Barre de progression calories
        mettreAJourProgressionCalories(dayCal);
    }

    private String formatMacros(int cal, double prot, double glu, double lip) {
        return String.format(Locale.US,
                "%d kcal | P: %.1fg | G: %.1fg | L: %.1fg",
                cal, prot, glu, lip);
    }

    // =========================================================================
    // BARRE DE PROGRESSION CALORIES
    // =========================================================================

    /**
     * Met à jour la barre de progression des calories.
     *
     * CORRECTION ERREUR 207 :
     *   AccueilActiviteController.OBJECTIF_CALORIES_JOURNALIER n'existe pas.
     *   Remplacé par la constante locale OBJECTIF_CALORIES_JOURNALIER_DEFAULT.
     */
    private void mettreAJourProgressionCalories(int caloriesTotal) {
        if (progressCalories    == null
                || lblCaloriesProgress  == null
                || lblCaloriesRestantes == null) return;

        // ✅ CORRIGÉ : utilise la constante locale au lieu de
        //    AccueilActiviteController.OBJECTIF_CALORIES_JOURNALIER (inexistant)
        int objectif = OBJECTIF_CALORIES_JOURNALIER_DEFAULT;
        if (objectif <= 0) objectif = 2150;

        double progress = Math.min(1.0, (double) caloriesTotal / objectif);
        progressCalories.setProgress(progress);
        lblCaloriesProgress.setText(caloriesTotal + " / " + objectif + " kcal");

        int restant = objectif - caloriesTotal;
        if (restant >= 0) {
            lblCaloriesRestantes.setText(restant + " kcal restantes");
            lblCaloriesRestantes.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            progressCalories.setStyle(
                    "-fx-accent: #6a1b9a; -fx-pref-height: 12px;");
        } else {
            lblCaloriesRestantes.setText(Math.abs(restant) + " kcal dépassées !");
            lblCaloriesRestantes.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c62828;");
            progressCalories.setStyle(
                    "-fx-accent: #c62828; -fx-pref-height: 12px;");
        }
    }

    // =========================================================================
    // LIGNE ALIMENT
    // =========================================================================
    private HBox creerLigneAliment(Consommation c, Aliment al,
                                   int cals, double prot, double glu, double lip) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("food-row-modern");

        Label icon = new Label("🍽");
        icon.setStyle("-fx-font-size: 24px; -fx-text-fill: #6a1b9a;");

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblNom = new Label(al.getNom_aliment() + " (" + c.getPoids_grammes() + "g)");
        lblNom.setStyle("-fx-text-fill: #3a0a52; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label lblStats = new Label(String.format(Locale.US,
                "%d kcal   |   P: %.1fg   |   G: %.1fg   |   L: %.1fg",
                cals, prot, glu, lip));
        lblStats.setStyle(
                "-fx-text-fill: #8e24aa; -fx-font-size: 12px; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(lblNom, lblStats);

        StackPane btnEdit   = creerBoutonIcone(SVG_EDIT,   "icon-btn-edit",   "icon-svg-edit");
        btnEdit.setOnMouseClicked(e -> modifierConsommation(c, al));

        StackPane btnDelete = creerBoutonIcone(SVG_DELETE, "icon-btn-delete", "icon-svg-delete");
        btnDelete.setOnMouseClicked(e -> supprimerConsommation(c));

        row.getChildren().addAll(icon, infoBox, btnEdit, btnDelete);
        return row;
    }

    private StackPane creerBoutonIcone(String svgContent,
                                       String containerClass, String svgClass) {
        SVGPath path = new SVGPath();
        path.setContent(svgContent);
        path.getStyleClass().add(svgClass);
        path.setScaleX(1.1);
        path.setScaleY(1.1);

        StackPane container = new StackPane(path);
        container.getStyleClass().addAll("icon-btn-modern", containerClass);
        return container;
    }

    // =========================================================================
    // HISTORIQUE
    // =========================================================================
    private void chargerHistorique() {
        if (flowPaneHistoriqueAliment == null) return;
        flowPaneHistoriqueAliment.getChildren().clear();

        Map<LocalDate, List<Consommation>> parJour = new TreeMap<>(Collections.reverseOrder());
        for (Consommation c : serviceConsommation.afficherTout()) {
            if (c.getDate_consommation() == null) continue;
            LocalDate d = c.getDate_consommation().toLocalDateTime().toLocalDate();
            parJour.computeIfAbsent(d, k -> new ArrayList<>()).add(c);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);

        for (Map.Entry<LocalDate, List<Consommation>> entry : parJour.entrySet()) {
            LocalDate          date        = entry.getKey();
            List<Consommation> repasDuJour = entry.getValue();

            int    totalCals = 0, totalEau = 0;
            double tP = 0, tG = 0, tL = 0;

            for (Consommation c : repasDuJour) {
                Aliment al = cacheAliments.get(c.getId_aliment());
                if (al != null) {
                    double ratio = c.getPoids_grammes() / 100.0;
                    totalCals += (int)(al.getCalories_pour_100g() * ratio);
                    tP        += al.getProteines() * ratio;
                    tG        += al.getGlucides()  * ratio;
                    tL        += al.getLipides()   * ratio;
                }
                if (c.getQuantite_eau_ml() > totalEau) totalEau = c.getQuantite_eau_ml();
            }

            // ── Carte historique ──
            VBox card = new VBox(10);
            card.getStyleClass().add("history-card");
            card.setPrefWidth(320);
            card.setMaxHeight(Region.USE_PREF_SIZE);

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox infosHeader = new VBox(3);
            HBox.setHgrow(infosHeader, Priority.ALWAYS);

            Label dateLbl = new Label("📅 " + date.format(dtf));
            dateLbl.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: #6a1b9a; -fx-font-size: 15px;");

            // ✅ CORRIGÉ ERREUR 329 : utilise la constante locale
            //    au lieu de AccueilActiviteController.OBJECTIF_CALORIES_JOURNALIER (inexistant)
            int objCal = OBJECTIF_CALORIES_JOURNALIER_DEFAULT;
            if (objCal <= 0) objCal = 2150;
            String statutCal = totalCals > objCal
                    ? (totalCals - objCal) + " kcal dépassées"
                    : (objCal - totalCals) + " kcal restantes";

            Label statsLbl = new Label(String.format(
                    "%d kcal | %d ml  —  %s", totalCals, totalEau, statutCal));
            statsLbl.setStyle(
                    "-fx-text-fill: #555555; -fx-font-size: 12px; -fx-font-weight: bold;");
            statsLbl.setWrapText(true);

            Label macrosLbl = new Label(String.format(Locale.US,
                    "P: %.1fg | G: %.1fg | L: %.1fg", tP, tG, tL));
            macrosLbl.setStyle("-fx-text-fill: #8e24aa; -fx-font-size: 11px;");

            infosHeader.getChildren().addAll(dateLbl, statsLbl, macrosLbl);

            Button btnDeleteDay = new Button("🗑️");
            btnDeleteDay.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #ff1744; " +
                            "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 5;");
            btnDeleteDay.setOnAction(e -> supprimerJourneeEntiere(date, repasDuJour));

            Button btnToggle = new Button("▼");
            btnToggle.getStyleClass().add("history-toggle-btn");

            HBox rightBtns = new HBox(5, btnDeleteDay, btnToggle);
            header.getChildren().addAll(infosHeader, rightBtns);

            VBox detailsBox = new VBox(5);
            detailsBox.setVisible(false);
            detailsBox.setManaged(false);

            for (Consommation c : repasDuJour) {
                Aliment al = cacheAliments.get(c.getId_aliment());
                if (al == null) continue;
                Label lblDet = new Label(
                        "• " + al.getNom_aliment() + " (" + c.getPoids_grammes() + "g)");
                lblDet.setStyle("-fx-text-fill: #3a0a52; -fx-font-size: 13px;");
                HBox detailRow = new HBox(lblDet);
                detailRow.getStyleClass().add("history-detail-row");
                detailsBox.getChildren().add(detailRow);
            }

            btnToggle.setOnAction(e -> {
                boolean vis = !detailsBox.isVisible();
                detailsBox.setVisible(vis);
                detailsBox.setManaged(vis);
                btnToggle.setText(vis ? "▲" : "▼");
            });

            card.getChildren().addAll(header, detailsBox);
            flowPaneHistoriqueAliment.getChildren().add(card);
        }
    }

    // =========================================================================
    // DIALOGUES SUPPRESSION / MODIFICATION
    // =========================================================================

    private void supprimerJourneeEntiere(LocalDate date, List<Consommation> repasDuJour) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #300018, #61042d); " +
                        "-fx-background-radius: 20; -fx-border-color: #ff1744; " +
                        "-fx-border-radius: 20; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,23,68,0.5), 30, 0, 0, 10);");

        Label iconLabel = new Label("🔥");
        iconLabel.setStyle("-fx-font-size: 50px;");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        Label titleLabel = new Label("Purger le " + date.format(dtf) + " ?");
        titleLabel.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLabel = new Label(
                "Toute la journée sera définitivement effacée de la base de données.");
        descLabel.setWrapText(true);
        descLabel.setStyle(
                "-fx-text-fill: #ffb3c6; -fx-font-size: 15px; -fx-text-alignment: center;");
        descLabel.setPrefWidth(300);

        Button btnSupprimer = new Button("Tout purger");
        btnSupprimer.setStyle(
                "-fx-background-color: linear-gradient(to right, #d50000, #ff1744); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> {
            for (Consommation c : repasDuJour) {
                serviceConsommation.supprimer(c.getId_consommation());
            }
            chargerRepasDuJour();
            chargerHistorique();
            stage.close();
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ffb3c6; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 30; " +
                        "-fx-padding: 10 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> stage.close());

        HBox boutonsBox = new HBox(15, btnAnnuler, btnSupprimer);
        boutonsBox.setAlignment(Pos.CENTER);
        content.getChildren().addAll(iconLabel, titleLabel, descLabel, boutonsBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void modifierConsommation(Consommation c, Aliment al) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4a148c, #8e24aa); " +
                        "-fx-background-radius: 20; -fx-border-color: #d500f9; " +
                        "-fx-border-radius: 20; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(142,36,170,0.5), 30, 0, 0, 10);");

        Label titleLabel = new Label("Modifier : " + al.getNom_aliment());
        titleLabel.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label l1 = new Label("Nouvelle Quantité (grammes) :");
        l1.setStyle("-fx-text-fill: #f3e5f5; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField txtQte = new TextField(String.valueOf(c.getPoids_grammes()));
        txtQte.setStyle(
                "-fx-background-color: white; -fx-text-fill: black; " +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8;");

        Button btnEnregistrer = new Button("Enregistrer");
        btnEnregistrer.setStyle(
                "-fx-background-color: white; -fx-text-fill: #6a1b9a; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
        btnEnregistrer.setOnAction(e -> {
            try {
                int nouvelleQte = Integer.parseInt(txtQte.getText().trim());
                if (nouvelleQte > 0) {
                    c.setPoids_grammes(nouvelleQte);
                    serviceConsommation.modifier(c);
                    chargerRepasDuJour();
                    chargerHistorique();
                    stage.close();
                }
            } catch (Exception ignored) {}
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #f3e5f5; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 30; " +
                        "-fx-padding: 10 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> stage.close());

        HBox boutonsBox = new HBox(15, btnAnnuler, btnEnregistrer);
        boutonsBox.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().addAll(titleLabel, l1, txtQte, boutonsBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void supprimerConsommation(Consommation c) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #300018, #61042d); " +
                        "-fx-background-radius: 20; -fx-border-color: #ff1744; " +
                        "-fx-border-radius: 20; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,23,68,0.5), 30, 0, 0, 10);");

        Label iconLabel  = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 50px;");
        Label titleLabel = new Label("Supprimer l'aliment ?");
        titleLabel.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setStyle(
                "-fx-background-color: linear-gradient(to right, #d50000, #ff1744); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> {
            serviceConsommation.supprimer(c.getId_consommation());
            chargerRepasDuJour();
            chargerHistorique();
            stage.close();
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ffb3c6; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 30; " +
                        "-fx-padding: 10 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> stage.close());

        HBox boutonsBox = new HBox(15, btnAnnuler, btnSupprimer);
        boutonsBox.setAlignment(Pos.CENTER);
        content.getChildren().addAll(iconLabel, titleLabel, boutonsBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void showCustomAlert(boolean isSuccess, String title, String message) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        String bgColor1    = isSuccess ? "#004d40" : "#4a0000";
        String bgColor2    = isSuccess ? "#1de9b6" : "#d50000";
        String borderColor = isSuccess ? "#64ffda" : "#ff5252";
        String shadowColor = isSuccess ? "rgba(29,233,182,0.5)" : "rgba(213,0,0,0.5)";

        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, "
                        + bgColor1 + ", " + bgColor2 + "); " +
                        "-fx-background-radius: 20; -fx-border-radius: 20; " +
                        "-fx-border-color: " + borderColor + "; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 30, 0, 0, 10);");

        Label iconLabel  = new Label(isSuccess ? "🎉" : "⚠️");
        iconLabel.setStyle("-fx-font-size: 50px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label msgLabel   = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 15px; -fx-text-alignment: center;");
        msgLabel.setPrefWidth(300);

        Button btnOk = new Button(isSuccess ? "Génial !" : "Compris");
        btnOk.setStyle(
                "-fx-background-color: white; -fx-text-fill: black; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-background-radius: 30; -fx-padding: 10 30; -fx-cursor: hand;");
        btnOk.setOnAction(e -> stage.close());

        content.getChildren().addAll(iconLabel, titleLabel, msgLabel, btnOk);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    @FXML
    void enregistrerJournee(ActionEvent event) {
        List<Consommation> toutes = serviceConsommation.afficherTout();
        Consommation premierDuJour = null;
        for (Consommation c : toutes) {
            if (c.getDate_consommation() == null) continue;
            if (c.getDate_consommation().toLocalDateTime()
                    .toLocalDate().equals(dateSelectionnee)) {
                premierDuJour = c;
                break;
            }
        }
        if (premierDuJour == null) {
            showCustomAlert(false, "Action Impossible",
                    "Vous devez ajouter au moins un aliment avant de valider la journée.");
            return;
        }
        premierDuJour.setQuantite_eau_ml(totalEauMl);
        serviceConsommation.modifier(premierDuJour);
        showCustomAlert(true, "Archivage Réussi !",
                "Votre journée a été ajoutée avec succès à l'historique.");
        chargerHistorique();
    }

    @FXML void addEau250(ActionEvent event) { totalEauMl += 250; majAffichageEau(); }
    @FXML void addEau500(ActionEvent event) { totalEauMl += 500; majAffichageEau(); }
    @FXML void resetEau(ActionEvent event)  { totalEauMl  = 0;   majAffichageEau(); }

    private void majAffichageEau() {
        if (labelEau != null) labelEau.setText(totalEauMl + " ml");
        if (waterBar != null) {
            // ✅ CORRIGÉ ERREUR 644 : utilise la constante locale
            //    au lieu de AccueilActiviteController.OBJECTIF_EAU_ML (inexistant)
            int objEau = OBJECTIF_EAU_ML_DEFAULT;
            if (objEau <= 0) objEau = 2000;
            waterBar.setProgress(Math.min((double) totalEauMl / objEau, 1.0));
        }
    }

    @FXML void addPetitDej(ActionEvent event) {
        repasSelectionne = CODE_PD;
        if (accueilController != null)
            accueilController.loadActivityPage("/views/AjouterAlimentFront.fxml");
    }

    @FXML void addDejeuner(ActionEvent event) {
        repasSelectionne = CODE_DEJ;
        if (accueilController != null)
            accueilController.loadActivityPage("/views/AjouterAlimentFront.fxml");
    }

    @FXML void addDiner(ActionEvent event) {
        repasSelectionne = CODE_DIN;
        if (accueilController != null)
            accueilController.loadActivityPage("/views/AjouterAlimentFront.fxml");
    }

    @FXML void addSnacks(ActionEvent event) {
        repasSelectionne = CODE_SNACK;
        if (accueilController != null)
            accueilController.loadActivityPage("/views/AjouterAlimentFront.fxml");
    }

    // ── Navigation ──
    @FXML void goToAccueil(ActionEvent event) {
        if (accueilController != null)
            accueilController.loadActivityPage("/views/AccueilActivite.fxml");
    }

    @FXML void goToAliments(ActionEvent event) {
        // Déjà sur cette page — pas de rechargement
    }

    @FXML void goToExercices(ActionEvent event) {
        if (accueilController != null)
            accueilController.loadActivityPage("/views/ActiviteViews/JournalExercices.fxml");
    }

    @FXML void goToSommeil(ActionEvent event) {
        if (accueilController != null)
            accueilController.loadActivityPage("/views/ActiviteViews/JournalSommeil.fxml");
    }
}