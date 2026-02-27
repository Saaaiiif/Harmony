package controllers.ActiviteControllers;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.ActiviteModels.Activite;
import models.ActiviteModels.Aliment;
import models.ActiviteModels.Consommation;
import models.UserModels.Session;
import services.ActiviteServices.ServiceActivite;
import services.ActiviteServices.ServiceAliment;
import services.ActiviteServices.ServiceConsommation;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

/**
 * Controller de la page d'accueil Activités (AccueilActivite.fxml).
 *
 * TOUTES LES ERREURS CORRIGÉES :
 *  1. getConsommationsDuJour()     → afficherParUtilisateur() + filtre par date
 *  2. getById()                    → trouverAlimentParId() (recherche dans cache)
 *  3. getAliment_id()              → getId_aliment()         (nom réel Consommation.java)
 *  4. getQuantite_g()              → getPoids_grammes()       (nom réel Consommation.java)
 *  5. getCalories_100g()           → getCalories_pour_100g()  (nom réel Aliment.java)
 *  6. getCaloriesBruleesDuJour()   → calcul manuel via afficherParUtilisateur() + filtre date
 *  7. getUser_objectif_calorique() → supprimé (n'existe pas dans user.java)
 *  8. try(Properties props = ...) → corrigé : Properties n'implémente pas AutoCloseable
 *  9. Imports inutilisés           → supprimés (ServiceExercice, ServiceSommeil, HttpClient...)
 */
public class AccueilActiviteController {

    // ── Référence au contrôleur parent (front office) ────────────────────────
    private controllers.UserControlleers.modifControl.AccueilController accueilController;

    public void setAccueilController(
            controllers.UserControlleers.modifControl.AccueilController controller) {
        this.accueilController = controller;
    }

    // ── Chemins FXML ─────────────────────────────────────────────────────────
    private static final String FXML_ACCUEIL   = "/views/AccueilActivite.fxml";
    private static final String FXML_ALIMENTS  = "/views/ActiviteViews/JournalAlimentaire.fxml";
    private static final String FXML_EXERCICES = "/views/ActiviteViews/JournalExercices.fxml";
    private static final String FXML_SOMMEIL   = "/views/ActiviteViews/JournalSommeil.fxml";

    // ── Objectifs par défaut ─────────────────────────────────────────────────
    private static final int OBJ_CAL_DEFAULT = 2150;
    private static final int OBJ_GLU_PCT     = 30;
    private static final int OBJ_LIP_PCT     = 10;
    private static final int OBJ_PROT_PCT    = 60;
    private static final int OBJ_EAU_ML      = 2000;

    // ── Services ─────────────────────────────────────────────────────────────
    private final ServiceConsommation serviceConsommation = new ServiceConsommation();
    private final ServiceActivite     serviceActivite     = new ServiceActivite();
    private final ServiceAliment      serviceAliment      = new ServiceAliment();

    // ── Cache aliments (évite de recharger la liste à chaque calcul) ─────────
    private List<Aliment> cacheAliments = null;

    // ── Date courante ─────────────────────────────────────────────────────────
    private LocalDate dateJour = LocalDate.now();
    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    // ── Objectifs utilisateur ─────────────────────────────────────────────────
    private int objCalories      = OBJ_CAL_DEFAULT;
    private int objGluPct        = OBJ_GLU_PCT;
    private int objLipPct        = OBJ_LIP_PCT;
    private int objProtPct       = OBJ_PROT_PCT;
    private int objEauMl         = OBJ_EAU_ML;
    private int objCalBrulees    = 0;
    private int objEntrainements = 0;
    private int objMinutes       = 0;

    // ── Citations motivation ─────────────────────────────────────────────────
    private static final String[] CITATIONS = {
            "Le succès n'est pas final, l'échec n'est pas fatal : c'est le courage de continuer qui compte.",
            "Chaque effort compte, chaque pas vous rapproche de votre objectif.",
            "La discipline est le pont entre les objectifs et les réalisations.",
            "Prenez soin de votre corps, c'est le seul endroit où vous devez vivre.",
            "Le corps accomplit ce que l'esprit croit possible.",
            "Commencez par faire ce qui est nécessaire, puis ce qui est possible.",
            "Les limites, comme les peurs, ne sont souvent que des illusions.",
            "La santé est la vraie richesse, pas les pièces d'or et l'argent.",
            "Le succès est la somme de petits efforts répétés chaque jour.",
            "Votre seule limite, c'est vous-même."
    };
    private static final String[] AUTHORS = {
            "Winston Churchill", "Harmony", "Jim Rohn", "Jim Rohn", "Harmony",
            "Saint François d'Assise", "Richard Bach", "Mahatma Gandhi",
            "Robert Collier", "Harmony"
    };
    private int currentQuoteIndex = new Random().nextInt(CITATIONS.length);

    // ── Nodes FXML ───────────────────────────────────────────────────────────
    @FXML private Label     lblDateJour;
    @FXML private Button    btnPrevDay;
    @FXML private Button    btnNextDay;

    @FXML private StackPane circularCaloriesPane;
    @FXML private Label     lblObjectifBase;
    @FXML private Label     lblAlimentsKcal;
    @FXML private Label     lblExercicesKcal;
    @FXML private Label     lblResteKcal;
    @FXML private Label     lblAlerteCalories;

    @FXML private VBox      vboxAlertes;
    @FXML private Label     lblDerniereVerif;

    @FXML private Label     lblQuoteText;
    @FXML private Label     lblQuoteAuthor;

    // Objectifs fitness
    @FXML private Label lblObjCal;
    @FXML private Label lblObjGluG;
    @FXML private Label lblObjGluPct;
    @FXML private Label lblObjLipG;
    @FXML private Label lblObjLipPct;
    @FXML private Label lblObjProtG;
    @FXML private Label lblObjProtPct;
    @FXML private Label lblObjCalBrulees;
    @FXML private Label lblObjEntrainements;
    @FXML private Label lblObjMinutes;
    @FXML private Label lblObjEau;

    @FXML private StackPane overlayObjectifs;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        afficherDate();
        chargerObjectifs();
        rafraichirDonnees();
        afficherCitation();
        verifierAlertes();
    }

    // =========================================================================
    //  NAVIGATION INTERNE — barre de navigation d'AccueilActivite.fxml
    // =========================================================================

    @FXML
    void goToAccueil(ActionEvent event) {
        naviguerVers(FXML_ACCUEIL);
    }

    @FXML
    void goToAliments(ActionEvent event) {
        naviguerVers(FXML_ALIMENTS);
    }

    @FXML
    void goToExercices(ActionEvent event) {
        naviguerVers(FXML_EXERCICES);
    }

    @FXML
    void goToSommeil(ActionEvent event) {
        naviguerVers(FXML_SOMMEIL);
    }

    /**
     * Navigation vers le Back Office Admin.
     * Charge DashboardAdmin.fxml dans la fenêtre principale.
     */
    @FXML
    void goToBackOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/modif/DashboardAdmin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Harmony — Administration");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur chargement DashboardAdmin.fxml : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Délègue la navigation au AccueilController parent.
     */
    private void naviguerVers(String fxmlPath) {
        if (accueilController != null) {
            accueilController.loadActivityPage(fxmlPath);
        } else {
            System.err.println("AccueilController non injecté — navigation impossible vers : " + fxmlPath);
        }
    }

    // =========================================================================
    //  NAVIGATION DATE
    // =========================================================================

    @FXML
    void jourPrecedent(ActionEvent event) {
        dateJour = dateJour.minusDays(1);
        afficherDate();
        rafraichirDonnees();
    }

    @FXML
    void jourSuivant(ActionEvent event) {
        dateJour = dateJour.plusDays(1);
        afficherDate();
        rafraichirDonnees();
    }

    @FXML
    void allerAujourdhui(ActionEvent event) {
        dateJour = LocalDate.now();
        afficherDate();
        rafraichirDonnees();
    }

    private void afficherDate() {
        if (lblDateJour == null) return;
        String dateStr = dateJour.equals(LocalDate.now())
                ? "Aujourd'hui — " + dateJour.format(FMT_DATE)
                : dateJour.format(FMT_DATE);
        lblDateJour.setText(dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1));
    }

    // =========================================================================
    //  DONNÉES CALORIES / ACTIVITÉ
    // =========================================================================

    private void rafraichirDonnees() {
        try {
            int calAliments  = calculerCaloriesAliments(dateJour);
            int calExercices = calculerCaloriesExercices(dateJour);
            int calReste     = objCalories - calAliments + calExercices;

            if (lblObjectifBase  != null) lblObjectifBase.setText(String.valueOf(objCalories));
            if (lblAlimentsKcal  != null) lblAlimentsKcal.setText(String.valueOf(calAliments));
            if (lblExercicesKcal != null) lblExercicesKcal.setText(String.valueOf(calExercices));
            if (lblResteKcal     != null) lblResteKcal.setText(String.valueOf(calReste));

            if (lblAlerteCalories != null) {
                if (calAliments > objCalories) {
                    lblAlerteCalories.setText("Objectif calorique dépassé de "
                            + (calAliments - objCalories) + " kcal !");
                } else {
                    lblAlerteCalories.setText("");
                }
            }

            dessinerIndicateurCirculaire(calAliments, calExercices);

        } catch (Exception e) {
            System.err.println("Erreur rafraîchissement données : " + e.getMessage());
        }
    }

    /**
     * Calcule les calories consommées pour une date donnée.
     *
     * CORRECTIONS :
     *  - Utilise afficherParUtilisateur() au lieu de getConsommationsDuJour() (inexistant)
     *  - Utilise getId_aliment()         au lieu de getAliment_id()           (inexistant)
     *  - Utilise getPoids_grammes()      au lieu de getQuantite_g()           (inexistant)
     *  - Utilise getCalories_pour_100g() au lieu de getCalories_100g()        (inexistant)
     *  - Utilise trouverAlimentParId()   au lieu de serviceAliment.getById()  (inexistant)
     */
    private int calculerCaloriesAliments(LocalDate date) {
        try {
            // Charger le cache des aliments une seule fois
            if (cacheAliments == null) {
                cacheAliments = serviceAliment.afficherTout();
            }

            // Récupérer l'ID utilisateur connecté
            int userId = 0;
            if (Session.getInstance() != null && Session.getInstance().getUser() != null) {
                userId = Session.getInstance().getUser().getUser_id();
            }

            // Récupérer les consommations filtrées par utilisateur
            List<Consommation> toutesConsos;
            if (userId > 0) {
                toutesConsos = serviceConsommation.afficherParUtilisateur(userId);
            } else {
                toutesConsos = serviceConsommation.afficherTout();
            }

            if (toutesConsos == null || toutesConsos.isEmpty()) return 0;

            double total = 0;
            for (Consommation c : toutesConsos) {
                // Filtrer par date
                if (c.getDate_consommation() == null) continue;
                LocalDate dateConso = c.getDate_consommation().toLocalDateTime().toLocalDate();
                if (!dateConso.equals(date)) continue;

                // ✅ CORRIGÉ : getId_aliment() (pas getAliment_id())
                int idAliment = c.getId_aliment();

                // ✅ CORRIGÉ : getPoids_grammes() (pas getQuantite_g())
                int quantiteG = c.getPoids_grammes();

                if (quantiteG <= 0) continue;

                // ✅ CORRIGÉ : recherche locale (pas serviceAliment.getById())
                Aliment aliment = trouverAlimentParId(idAliment);
                if (aliment == null) continue;

                // ✅ CORRIGÉ : getCalories_pour_100g() (pas getCalories_100g())
                total += (aliment.getCalories_pour_100g() * (double) quantiteG) / 100.0;
            }
            return (int) total;

        } catch (Exception e) {
            System.err.println("Erreur calcul calories aliments : " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calcule les calories brûlées pour une date donnée.
     *
     * CORRECTION : getCaloriesBruleesDuJour() n'existe pas dans ServiceActivite.
     * Remplacement par un calcul manuel : afficherParUtilisateur() + filtre date
     * + somme de getCalories_brulees() pour chaque Activite.
     */
    private int calculerCaloriesExercices(LocalDate date) {
        try {
            int userId = 0;
            if (Session.getInstance() != null && Session.getInstance().getUser() != null) {
                userId = Session.getInstance().getUser().getUser_id();
            }

            List<Activite> activites;
            if (userId > 0) {
                activites = serviceActivite.afficherParUtilisateur(userId);
            } else {
                activites = serviceActivite.afficherTout();
            }

            if (activites == null || activites.isEmpty()) return 0;

            int totalCalBrulees = 0;
            for (Activite a : activites) {
                if (a.getDate_activite() == null) continue;
                LocalDate dateActivite = a.getDate_activite().toLocalDateTime().toLocalDate();
                if (!dateActivite.equals(date)) continue;
                // ✅ getCalories_brulees() existe bien dans Activite.java
                totalCalBrulees += a.getCalories_brulees();
            }
            return totalCalBrulees;

        } catch (Exception e) {
            System.err.println("Erreur calcul calories exercices : " + e.getMessage());
            return 0;
        }
    }

    /**
     * Recherche un aliment dans le cache par son ID.
     * Remplace serviceAliment.getById() qui n'existe pas.
     */
    private Aliment trouverAlimentParId(int id) {
        if (cacheAliments == null) return null;
        for (Aliment a : cacheAliments) {
            // ✅ getId_aliment() existe bien dans Aliment.java
            if (a.getId_aliment() == id) return a;
        }
        return null;
    }

    // =========================================================================
    //  INDICATEUR CIRCULAIRE (Canvas)
    // =========================================================================

    private void dessinerIndicateurCirculaire(int calAliments, int calExercices) {
        if (circularCaloriesPane == null) return;
        circularCaloriesPane.getChildren().clear();

        double size   = 148;
        double cx     = size / 2;
        double cy     = size / 2;
        double r      = 55;
        double stroke = 12;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond gris
        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(stroke);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 0, 360, ArcType.OPEN);

        // Arc vert = calories brûlées par exercice
        double pctEx = objCalories > 0 ? Math.min(1.0, (double) calExercices / objCalories) : 0;
        if (pctEx > 0) {
            gc.setStroke(Color.web("#4caf50"));
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -pctEx * 360, ArcType.OPEN);
        }

        // Arc violet/rouge = calories consommées
        double pctAl = objCalories > 0 ? Math.min(1.0, (double) calAliments / objCalories) : 0;
        if (pctAl > 0) {
            gc.setStroke(Color.web(pctAl >= 1.0 ? "#e53935" : "#6a1b9a"));
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                    90 - pctEx * 360, -pctAl * 360, ArcType.OPEN);
        }

        // Texte central : calories restantes
        int reste = objCalories - calAliments + calExercices;
        gc.setFill(Color.web("#1a1a2e"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));
        String resteStr = String.valueOf(Math.max(0, reste));
        double tw = resteStr.length() * 13.0;
        gc.fillText(resteStr, cx - tw / 2, cy + 8);
        gc.setFont(Font.font("System", 11));
        gc.setFill(Color.web("#9e9e9e"));
        gc.fillText("Reste", cx - 17, cy + 26);

        circularCaloriesPane.getChildren().add(canvas);
    }

    // =========================================================================
    //  OBJECTIFS FITNESS
    // =========================================================================

    /**
     * Charge les objectifs depuis le fichier .properties local.
     *
     * CORRECTIONS :
     *  - try(Properties props = new Properties()) corrigé :
     *    Properties n'implémente pas AutoCloseable → on utilise un try normal
     *    et try-with-resources uniquement sur FileInputStream (qui, lui, est AutoCloseable)
     *  - getUser_objectif_calorique() supprimé : n'existe pas dans user.java
     */
    private void chargerObjectifs() {
        File prefFile = new File("harmony_objectifs.properties");
        if (prefFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(prefFile)) {
                props.load(fis);
                objCalories      = Integer.parseInt(props.getProperty("cal",           String.valueOf(OBJ_CAL_DEFAULT)));
                objGluPct        = Integer.parseInt(props.getProperty("glu",           String.valueOf(OBJ_GLU_PCT)));
                objLipPct        = Integer.parseInt(props.getProperty("lip",           String.valueOf(OBJ_LIP_PCT)));
                objProtPct       = Integer.parseInt(props.getProperty("prot",          String.valueOf(OBJ_PROT_PCT)));
                objEauMl         = Integer.parseInt(props.getProperty("eau",           String.valueOf(OBJ_EAU_ML)));
                objCalBrulees    = Integer.parseInt(props.getProperty("brulees",       "0"));
                objEntrainements = Integer.parseInt(props.getProperty("entrainements", "0"));
                objMinutes       = Integer.parseInt(props.getProperty("minutes",       "0"));
            } catch (Exception e) {
                System.err.println("Erreur lecture objectifs (valeurs par défaut utilisées) : " + e.getMessage());
            }
        }
        // Note : getUser_objectif_calorique() n'existe pas dans user.java → supprimé
        afficherObjectifs();
    }

    private void afficherObjectifs() {
        if (lblObjCal           != null) lblObjCal.setText(objCalories + " kcal");
        if (lblObjEau           != null) lblObjEau.setText(objEauMl + " ml");
        if (lblObjCalBrulees    != null) lblObjCalBrulees.setText(objCalBrulees + " Calories");
        if (lblObjEntrainements != null) lblObjEntrainements.setText(objEntrainements + " entraînements");
        if (lblObjMinutes       != null) lblObjMinutes.setText(String.valueOf(objMinutes));

        // Glucides : 1g = 4 kcal
        int gluG = (int) (objCalories * objGluPct / 100.0 / 4);
        if (lblObjGluG   != null) lblObjGluG.setText(gluG + " g");
        if (lblObjGluPct != null) lblObjGluPct.setText(objGluPct + " %");

        // Lipides : 1g = 9 kcal
        int lipG = (int) (objCalories * objLipPct / 100.0 / 9);
        if (lblObjLipG   != null) lblObjLipG.setText(lipG + " g");
        if (lblObjLipPct != null) lblObjLipPct.setText(objLipPct + " %");

        // Protéines : 1g = 4 kcal
        int protG = (int) (objCalories * objProtPct / 100.0 / 4);
        if (lblObjProtG   != null) lblObjProtG.setText(protG + " g");
        if (lblObjProtPct != null) lblObjProtPct.setText(objProtPct + " %");
    }

    @FXML
    void ouvrirModificationObjectifs(ActionEvent event) {
        if (overlayObjectifs == null) return;

        VBox dialog = new VBox(14);
        dialog.setMaxWidth(420);
        dialog.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 30; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(106,27,154,0.3), 20, 0, 0, 0);");
        dialog.setAlignment(Pos.CENTER);

        Label titre = new Label("Modifier vos objectifs");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        TextField tfCal  = creerChampObj("Calories (kcal)",      String.valueOf(objCalories));
        TextField tfGlu  = creerChampObj("Glucides (%)",         String.valueOf(objGluPct));
        TextField tfLip  = creerChampObj("Lipides (%)",          String.valueOf(objLipPct));
        TextField tfProt = creerChampObj("Protéines (%)",        String.valueOf(objProtPct));
        TextField tfEau  = creerChampObj("Eau quotidienne (ml)", String.valueOf(objEauMl));
        TextField tfBrul = creerChampObj("Cal. brûlées/semaine", String.valueOf(objCalBrulees));
        TextField tfEntr = creerChampObj("Entraînements/sem.",   String.valueOf(objEntrainements));
        TextField tfMin  = creerChampObj("Minutes/entraînement", String.valueOf(objMinutes));

        Button btnSave = new Button("Sauvegarder");
        btnSave.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10 25; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            try {
                objCalories      = Integer.parseInt(tfCal.getText().trim());
                objGluPct        = Integer.parseInt(tfGlu.getText().trim());
                objLipPct        = Integer.parseInt(tfLip.getText().trim());
                objProtPct       = Integer.parseInt(tfProt.getText().trim());
                objEauMl         = Integer.parseInt(tfEau.getText().trim());
                objCalBrulees    = Integer.parseInt(tfBrul.getText().trim());
                objEntrainements = Integer.parseInt(tfEntr.getText().trim());
                objMinutes       = Integer.parseInt(tfMin.getText().trim());
                sauvegarderObjectifs();
                afficherObjectifs();
                rafraichirDonnees();
                fermerOverlay();
            } catch (NumberFormatException ex) {
                titre.setText("Valeurs invalides — entrez des nombres entiers");
                titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e53935;");
            }
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; "
                + "-fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10 25; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> fermerOverlay());

        HBox btnBox = new HBox(15, btnAnnuler, btnSave);
        btnBox.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(titre, tfCal, tfGlu, tfLip, tfProt, tfEau, tfBrul, tfEntr, tfMin, btnBox);
        overlayObjectifs.getChildren().setAll(dialog);
        overlayObjectifs.setVisible(true);
        overlayObjectifs.setOnMouseClicked(ev -> {
            if (ev.getTarget() == overlayObjectifs) fermerOverlay();
        });

        FadeTransition ft = new FadeTransition(Duration.millis(200), dialog);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private TextField creerChampObj(String prompt, String valeur) {
        TextField tf = new TextField(valeur);
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 8;");
        return tf;
    }

    private void fermerOverlay() {
        if (overlayObjectifs != null) overlayObjectifs.setVisible(false);
    }

    /**
     * Sauvegarde les objectifs dans un fichier .properties.
     *
     * CORRECTION : Properties n'est pas AutoCloseable, donc on ne peut pas
     * l'utiliser dans try-with-resources. On l'instancie normalement et on
     * utilise try-with-resources uniquement sur FileOutputStream.
     */
    private void sauvegarderObjectifs() {
        Properties props = new Properties();
        props.setProperty("cal",           String.valueOf(objCalories));
        props.setProperty("glu",           String.valueOf(objGluPct));
        props.setProperty("lip",           String.valueOf(objLipPct));
        props.setProperty("prot",          String.valueOf(objProtPct));
        props.setProperty("eau",           String.valueOf(objEauMl));
        props.setProperty("brulees",       String.valueOf(objCalBrulees));
        props.setProperty("entrainements", String.valueOf(objEntrainements));
        props.setProperty("minutes",       String.valueOf(objMinutes));
        try (FileOutputStream fos = new FileOutputStream("harmony_objectifs.properties")) {
            props.store(fos, "Harmony Objectifs");
        } catch (Exception e) {
            System.err.println("Impossible de sauvegarder les objectifs : " + e.getMessage());
        }
    }

    // =========================================================================
    //  ALERTES
    // =========================================================================

    @FXML
    void verifierAlertes(ActionEvent event) {
        verifierAlertes();
    }

    void verifierAlertes() {
        if (vboxAlertes == null) return;
        vboxAlertes.getChildren().clear();

        try {
            int calAuj = calculerCaloriesAliments(LocalDate.now());
            if (calAuj > objCalories) {
                ajouterAlerte("Objectif calorique dépassé de "
                        + (calAuj - objCalories) + " kcal aujourd'hui !", "#ffebee", "#c62828");
            } else if (calAuj > objCalories * 0.9) {
                ajouterAlerte("Vous approchez de votre objectif calorique ("
                        + calAuj + "/" + objCalories + " kcal)", "#fffde7", "#f57f17");
            } else {
                ajouterAlerte("Calories dans les objectifs aujourd'hui ("
                        + calAuj + "/" + objCalories + " kcal)", "#e8f5e9", "#2e7d32");
            }
        } catch (Exception e) {
            ajouterAlerte("Connectez-vous pour voir vos alertes personnalisées",
                    "#e3f2fd", "#1565c0");
        }

        if (lblDerniereVerif != null) {
            lblDerniereVerif.setText("Dernière vérification : "
                    + java.time.LocalTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)));
        }
    }

    private void ajouterAlerte(String message, String bgColor, String textColor) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; "
                + "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 8;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        vboxAlertes.getChildren().add(lbl);
    }

    // =========================================================================
    //  CITATION DU JOUR
    // =========================================================================

    @FXML
    void changerCitation(ActionEvent event) {
        currentQuoteIndex = (currentQuoteIndex + 1) % CITATIONS.length;
        afficherCitation();
    }

    private void afficherCitation() {
        if (lblQuoteText   != null) lblQuoteText.setText(CITATIONS[currentQuoteIndex]);
        if (lblQuoteAuthor != null) lblQuoteAuthor.setText("— " + AUTHORS[currentQuoteIndex]);
    }

    // =========================================================================
    //  EXPORT BILAN (fichier texte .pdf)
    // =========================================================================

    @FXML
    void genererBilanPDF(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le bilan");
        fc.setInitialFileName("bilan_harmony_" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);
        if (fichier == null) return;

        try {
            genererPDFSimple(fichier);
            afficherInfo("Bilan généré avec succès :\n" + fichier.getAbsolutePath());
        } catch (Exception e) {
            afficherInfo("Erreur lors de la génération : " + e.getMessage());
        }
    }

    private void genererPDFSimple(File fichier) throws IOException {
        int calAuj  = calculerCaloriesAliments(dateJour);
        int calBrul = calculerCaloriesExercices(dateJour);
        int reste   = objCalories - calAuj + calBrul;

        String contenu =
                "BILAN HARMONY — "
                        + dateJour.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                        + "========================================\n\n"
                        + "CALORIES\n"
                        + "  Objectif    : " + objCalories + " kcal\n"
                        + "  Consommées  : " + calAuj      + " kcal\n"
                        + "  Brûlées     : " + calBrul     + " kcal\n"
                        + "  Restantes   : " + reste        + " kcal\n\n"
                        + "OBJECTIFS NUTRITIONNELS\n"
                        + "  Glucides    : " + objGluPct  + "%  ("
                        + (int)(objCalories * objGluPct  / 400.0) + " g)\n"
                        + "  Lipides     : " + objLipPct  + "%  ("
                        + (int)(objCalories * objLipPct  / 900.0) + " g)\n"
                        + "  Protéines   : " + objProtPct + "%  ("
                        + (int)(objCalories * objProtPct / 400.0) + " g)\n\n"
                        + "HYDRATATION\n"
                        + "  Objectif eau : " + objEauMl + " ml/jour\n\n"
                        + "FITNESS\n"
                        + "  Calories brûlées/sem. : " + objCalBrulees    + " kcal\n"
                        + "  Entraînements/sem.    : " + objEntrainements  + "\n"
                        + "  Minutes/entraînement  : " + objMinutes        + " min\n\n"
                        + "========================================\n"
                        + "Généré par Harmony — "
                        + java.time.LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH));

        try (PrintWriter pw = new PrintWriter(new FileWriter(fichier))) {
            pw.println(contenu);
        }
    }

    private void afficherInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Harmony");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}