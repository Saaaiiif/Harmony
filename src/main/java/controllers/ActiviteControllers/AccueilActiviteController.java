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
import models.ActiviteModels.Sommeil;
import models.UserModels.Session;
import services.ActiviteServices.ServiceActivite;
import services.ActiviteServices.ServiceAliment;
import services.ActiviteServices.ServiceConsommation;
import services.ActiviteServices.ServiceSommeil;

// ── iText 5 — vrai PDF ──────────────────────────────────────────────────────
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class AccueilActiviteController {

    // ── Référence au contrôleur parent (front office) ────────────────────────
    private controllers.UserControlleers.modifControl.AccueilController accueilController;

    public void setAccueilController(
            controllers.UserControlleers.modifControl.AccueilController controller) {
        this.accueilController = controller;
    }

    // ── Chemins FXML ─────────────────────────────────────────────────────────
    private static final String FXML_ACCUEIL   = "/views/ActiviteViews/AccueilActivite.fxml";
    private static final String FXML_ALIMENTS  = "/views/ActiviteViews/JournalAlimentaire.fxml";
    private static final String FXML_EXERCICES = "/views/ActiviteViews/JournalExercices.fxml";
    private static final String FXML_SOMMEIL   = "/views/ActiviteViews/JournalSommeil.fxml";

    // ── Objectifs par défaut ─────────────────────────────────────────────────
    private static final int OBJ_CAL_DEFAULT = 2150;
    private static final int OBJ_GLU_PCT     = 30;
    private static final int OBJ_LIP_PCT     = 10;
    private static final int OBJ_PROT_PCT    = 60;
    private static final int OBJ_EAU_ML      = 2000;
    private int    dernierIndexCitation     = -1;

    // ── Services ─────────────────────────────────────────────────────────────
    private final ServiceConsommation serviceConsommation = new ServiceConsommation();
    private final ServiceActivite     serviceActivite     = new ServiceActivite();
    private final ServiceAliment      serviceAliment      = new ServiceAliment();
    private final ServiceSommeil      serviceSommeil      = new ServiceSommeil();

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
    //  EXPORT BILAN PDF — iText 5 (vrai format PDF)
    // =========================================================================

    @FXML
    void genererBilanPDF(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le bilan Harmony");
        fc.setInitialFileName("bilan_harmony_" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);
        if (fichier == null) return;

        try {
            genererVraiPDF(fichier);
            afficherInfo("✅ Bilan généré avec succès !\n" + fichier.getAbsolutePath());
        } catch (Exception e) {
            afficherInfo("❌ Erreur lors de la génération PDF :\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Génération du vrai PDF avec iText 5
    // ──────────────────────────────────────────────────────────────────────────

    /** Couleurs charte graphique Harmony */
    private static final BaseColor VIOLET_HARMONY    = new BaseColor(106, 27, 154);
    private static final BaseColor VIOLET_LIGHT      = new BaseColor(156, 39, 176);
    private static final BaseColor VIOLET_VERY_LIGHT = new BaseColor(243, 229, 255);
    private static final BaseColor GRIS_HEADER       = new BaseColor(245, 245, 250);
    private static final BaseColor GRIS_SEPARATEUR   = new BaseColor(224, 224, 224);
    private static final BaseColor VERT_OK           = new BaseColor(46, 125, 50);
    private static final BaseColor ROUGE_ALERTE      = new BaseColor(198, 40, 40);
    private static final BaseColor BLEU_DONNEE       = new BaseColor(21, 101, 192);

    private void genererVraiPDF(File fichier) throws IOException, DocumentException {
        // ── 1. Récupérer les données ──────────────────────────────────────────
        int userId = 0;
        String nomUtilisateur = "Utilisateur";
        if (Session.getInstance() != null && Session.getInstance().getUser() != null) {
            userId = Session.getInstance().getUser().getUser_id();
            String prenom = Session.getInstance().getUser().getUser_prenom();
            String nom    = Session.getInstance().getUser().getUser_nom();
            if (prenom != null || nom != null)
                nomUtilisateur = (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
        }

        // Sport
        List<Activite> activites = userId > 0
                ? serviceActivite.afficherParUtilisateur(userId)
                : serviceActivite.afficherTout();
        if (activites == null) activites = new ArrayList<>();

        // Nutrition
        if (cacheAliments == null) cacheAliments = serviceAliment.afficherTout();
        List<Consommation> consos = userId > 0
                ? serviceConsommation.afficherParUtilisateur(userId)
                : serviceConsommation.afficherTout();
        if (consos == null) consos = new ArrayList<>();

        // Sommeil
        List<Sommeil> nuits = userId > 0
                ? serviceSommeil.afficherParUtilisateur(userId)
                : serviceSommeil.afficherTout();
        if (nuits == null) nuits = new ArrayList<>();

        // ── 2. Calculs sport ─────────────────────────────────────────────────
        int nbSeances = activites.size();
        int calBruleesTotal = activites.stream().mapToInt(Activite::getCalories_brulees).sum();
        int dureeTotal = activites.stream().mapToInt(Activite::getDuree_minutes).sum();
        int dureeMin = nbSeances > 0 ? dureeTotal / nbSeances : 0;
        int calMoySeance = nbSeances > 0 ? calBruleesTotal / nbSeances : 0;

        LocalDate maintenant = LocalDate.now();
        LocalDate il7Jours = maintenant.minusDays(7);
        long nbSeancesSemaine = activites.stream()
                .filter(a -> a.getDate_activite() != null
                        && a.getDate_activite().toLocalDateTime().toLocalDate().isAfter(il7Jours))
                .count();
        int calBruleesSemaine = activites.stream()
                .filter(a -> a.getDate_activite() != null
                        && a.getDate_activite().toLocalDateTime().toLocalDate().isAfter(il7Jours))
                .mapToInt(Activite::getCalories_brulees).sum();

        long joursTrackes = activites.stream()
                .filter(a -> a.getDate_activite() != null)
                .map(a -> a.getDate_activite().toLocalDateTime().toLocalDate())
                .distinct().count();

        // ── 3. Calculs nutrition ─────────────────────────────────────────────
        long joursMealsTrackes = consos.stream()
                .filter(c -> c.getDate_consommation() != null)
                .map(c -> c.getDate_consommation().toLocalDateTime().toLocalDate())
                .distinct().count();
        int totalEntrees = consos.size();
        int eauTotalMl = consos.stream().mapToInt(Consommation::getQuantite_eau_ml).sum();
        double eauTotalL = eauTotalMl / 1000.0;

        // Calcul taux réussite objectif calorique (jours dans l'objectif)
        Map<LocalDate, Integer> calParJour = new HashMap<>();
        for (Consommation c : consos) {
            if (c.getDate_consommation() == null) continue;
            LocalDate d = c.getDate_consommation().toLocalDateTime().toLocalDate();
            int cal = 0;
            Aliment al = trouverAlimentParId(c.getId_aliment());
            if (al != null && c.getPoids_grammes() > 0)
                cal = (int) (al.getCalories_pour_100g() * c.getPoids_grammes() / 100.0);
            calParJour.merge(d, cal, Integer::sum);
        }
        long joursOk = calParJour.values().stream().filter(cal -> cal <= objCalories).count();
        int tauxOk = joursMealsTrackes > 0 ? (int) (joursOk * 100 / joursMealsTrackes) : 0;

        // ── 4. Calculs sommeil ───────────────────────────────────────────────
        int nbNuits = nuits.size();
        double dureeMovSommeil = 0;
        long nbBonnesNuits = 0;
        for (Sommeil s : nuits) {
            if (s.getDate_coucher() != null && s.getDate_reveil() != null) {
                double heures = ChronoUnit.MINUTES.between(
                        s.getDate_coucher().toLocalDateTime(),
                        s.getDate_reveil().toLocalDateTime()) / 60.0;
                dureeMovSommeil += heures;
            }
            if ("Excellent".equalsIgnoreCase(s.getQualite_sommeil())
                    || "Bon".equalsIgnoreCase(s.getQualite_sommeil()))
                nbBonnesNuits++;
        }
        double dureeMovh = nbNuits > 0 ? dureeMovSommeil / nbNuits : 0;
        int tauxBonnesNuits = nbNuits > 0 ? (int) (nbBonnesNuits * 100 / nbNuits) : 0;

        // ── 5. Création du document PDF ──────────────────────────────────────
        Document doc = new Document(PageSize.A4, 45, 45, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fichier));
        doc.open();

        // Fonts (CP1252 pour les caractères français)
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        com.itextpdf.text.Font fontTitrePrincipal = new com.itextpdf.text.Font(bfBold, 22, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);
        com.itextpdf.text.Font fontSousTitre      = new com.itextpdf.text.Font(bf, 11, com.itextpdf.text.Font.NORMAL, new BaseColor(200, 170, 230));
        com.itextpdf.text.Font fontSection        = new com.itextpdf.text.Font(bfBold, 15, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY);
        com.itextpdf.text.Font fontTableHeader    = new com.itextpdf.text.Font(bfBold, 11, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);
        com.itextpdf.text.Font fontTableBody      = new com.itextpdf.text.Font(bf, 10, com.itextpdf.text.Font.NORMAL, new BaseColor(60, 60, 60));
        com.itextpdf.text.Font fontKpiValue       = new com.itextpdf.text.Font(bfBold, 18, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY);
        com.itextpdf.text.Font fontKpiLabel       = new com.itextpdf.text.Font(bf, 9, com.itextpdf.text.Font.NORMAL, new BaseColor(120, 120, 120));
        com.itextpdf.text.Font fontClosing        = new com.itextpdf.text.Font(bf, 11, com.itextpdf.text.Font.ITALIC, VIOLET_LIGHT);

        String dateGen = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // ════════════════════════════════════════════════════════════════════
        //  PAGE 1 : EN-TÊTE + SPORT
        // ════════════════════════════════════════════════════════════════════

        // ── Bandeau d'en-tête violet ──────────────────────────────────────
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        cb.setColorFill(VIOLET_HARMONY);
        cb.roundRectangle(45, 760, 505, 62, 8);
        cb.fill();
        // Dégradé simulé : bande plus claire sur le côté droit
        cb.setColorFill(VIOLET_LIGHT);
        cb.roundRectangle(370, 760, 180, 62, 8);
        cb.fill();
        cb.restoreState();

        Paragraph titreHeader = new Paragraph();
        titreHeader.add(new Phrase("BILAN GLOBAL DE SANTE - HARMONY\n", fontTitrePrincipal));
        titreHeader.add(new Phrase("Genere le : " + dateGen + "   |   " + nomUtilisateur.trim(), fontSousTitre));
        titreHeader.setAlignment(Element.ALIGN_CENTER);
        titreHeader.setSpacingBefore(5);
        titreHeader.setSpacingAfter(18);
        doc.add(titreHeader);

        // ── TABLEAU DE BORD GLOBAL ─────────────────────────────────────
        Paragraph titreDashboard = new Paragraph("TABLEAU DE BORD GLOBAL", fontSection);
        titreDashboard.setSpacingAfter(4);
        doc.add(titreDashboard);
        doc.add(traitSeparateur(cb, writer, doc));
        doc.add(new Paragraph(" "));

        // ── SECTION SPORT ─────────────────────────────────────────────────
        Paragraph titreSection1 = new Paragraph("SYNTHESE SPORTIVE", fontSection);
        titreSection1.setSpacingAfter(10);
        doc.add(titreSection1);

        // 4 KPI boxes sur une ligne
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(14);
        kpiTable.addCell(creerCelluleKpi(bfBold, bf, "Seances sport", nbSeances + " seances"));
        kpiTable.addCell(creerCelluleKpi(bfBold, bf, "Calories brulees", calBruleesTotal + " kcal"));
        kpiTable.addCell(creerCelluleKpi(bfBold, bf, "Jours trackes", joursTrackes + " jours"));
        kpiTable.addCell(creerCelluleKpi(bfBold, bf, "Nuits enregistrees", nbNuits + " nuits"));
        doc.add(kpiTable);

        // ── Graphique calories brûlées par séance ─────────────────────────
        Paragraph titrGraph1 = new Paragraph("Evolution des calories brulees par seance :", new com.itextpdf.text.Font(bfBold, 11, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY));
        titrGraph1.setSpacingBefore(4);
        titrGraph1.setSpacingAfter(6);
        doc.add(titrGraph1);

        // Préparer les données du graphique
        List<Activite> actSortees = activites.stream()
                .filter(a -> a.getDate_activite() != null)
                .sorted(Comparator.comparing(a -> a.getDate_activite().toLocalDateTime()))
                .collect(Collectors.toList());

        dessinerGraphiqueLigne(cb, writer, doc,
                actSortees.stream().mapToDouble(Activite::getCalories_brulees).toArray(),
                actSortees.stream().map(a -> "S" + (actSortees.indexOf(a) + 1)).toArray(String[]::new),
                "Seances", "Calories brulees", VIOLET_HARMONY);

        doc.add(new Paragraph(" "));

        // ── Table indicateurs de performance sport ────────────────────────
        PdfPTable tableSport = creerTableauSection(
                new String[]{"Indicateur", "Performances"},
                new String[][]{
                        {"Seances realisees (total)", nbSeances + " seances"},
                        {"Seances cette semaine", nbSeancesSemaine + " seances (7 derniers jours)"},
                        {"Temps total d'effort", (dureeTotal / 60) + "h " + (dureeTotal % 60) + "m"},
                        {"Temps moyen par seance", dureeMin + " min/seance"},
                        {"Calories brulees (total)", calBruleesTotal + " kcal"},
                        {"Calories brulees cette semaine", calBruleesSemaine + " kcal"},
                        {"Moyenne cal/seance", calMoySeance + " kcal"},
                        {"Objectif entrainements/semaine", objEntrainements + " seances/semaine"}
                },
                bfBold, bf
        );
        doc.add(tableSport);

        // ════════════════════════════════════════════════════════════════════
        //  PAGE 2 : NUTRITION & HYDRATATION
        // ════════════════════════════════════════════════════════════════════
        doc.newPage();
        dessinerBandeauSection(cb, writer, "NUTRITION & HYDRATATION", bfBold, 770);

        doc.add(new Paragraph("\n"));

        // Table nutrition
        PdfPTable tableNutrition = creerTableauSection(
                new String[]{"Indicateur", "Valeur"},
                new String[][]{
                        {"Jours avec repas enregistres", joursMealsTrackes + " jours"},
                        {"Total entrees nutritionnelles", totalEntrees + " entrees"},
                        {"Volume d'eau bu (total)", String.format("%.1f L", eauTotalL)},
                        {"Objectif calorique journalier", objCalories + " kcal/jour"},
                        {"Taux de reussite objectif cal.", tauxOk + "% des jours dans l'objectif"},
                        {"Repartition macros cibles", "G:" + objGluPct + "% | L:" + objLipPct + "% | P:" + objProtPct + "%"}
                },
                bfBold, bf
        );
        doc.add(tableNutrition);
        doc.add(new Paragraph(" "));

        // ── Graphique hydratation ──────────────────────────────────────────
        Paragraph titrGraph2 = new Paragraph("Suivi de l'hydratation quotidienne :", new com.itextpdf.text.Font(bfBold, 11, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY));
        titrGraph2.setSpacingBefore(8);
        titrGraph2.setSpacingAfter(6);
        doc.add(titrGraph2);

        // Données eau par entrée
        List<Consommation> consosTriees = consos.stream()
                .filter(c -> c.getDate_consommation() != null)
                .sorted(Comparator.comparing(c -> c.getDate_consommation().toLocalDateTime()))
                .collect(Collectors.toList());
        double[] eauData = consosTriees.stream().mapToDouble(Consommation::getQuantite_eau_ml).toArray();
        String[] eauLabels = new String[consosTriees.size()];
        for (int i = 0; i < consosTriees.size(); i++) eauLabels[i] = "R" + (i + 1);

        if (eauData.length > 0) {
            dessinerGraphiqueLigne(cb, writer, doc, eauData, eauLabels, "Entrees", "Eau (ml)", new BaseColor(0, 100, 220));
        } else {
            doc.add(new Paragraph("Aucune donnee d'hydratation enregistree.",
                    new com.itextpdf.text.Font(bf, 10, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY)));
        }

        // ════════════════════════════════════════════════════════════════════
        //  PAGE 3 : SOMMEIL
        // ════════════════════════════════════════════════════════════════════
        doc.newPage();
        dessinerBandeauSection(cb, writer, "RECUPERATION & SOMMEIL", bfBold, 770);

        doc.add(new Paragraph("\n"));

        // Table sommeil
        PdfPTable tableSommeil = creerTableauSection(
                new String[]{"Indicateur", "Donnee"},
                new String[][]{
                        {"Nuits enregistrees", nbNuits + " nuits"},
                        {"Duree moyenne de sommeil", String.format("%.1f h/nuit", dureeMovh)},
                        {"Nuits de bonne qualite", nbBonnesNuits + " nuits (Excellent ou Bon)"},
                        {"Taux de bonne qualite", tauxBonnesNuits + "%"},
                        {"Objectif sommeil recommande", "7 a 9 heures par nuit (OMS)"}
                },
                bfBold, bf
        );
        doc.add(tableSommeil);
        doc.add(new Paragraph(" "));

        // ── Graphique durée de sommeil ──────────────────────────────────────
        Paragraph titrGraph3 = new Paragraph("Evolution de la duree de sommeil :", new com.itextpdf.text.Font(bfBold, 11, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY));
        titrGraph3.setSpacingBefore(8);
        titrGraph3.setSpacingAfter(6);
        doc.add(titrGraph3);

        List<Sommeil> nuitsTriees = nuits.stream()
                .filter(s -> s.getDate_coucher() != null && s.getDate_reveil() != null)
                .sorted(Comparator.comparing(s -> s.getDate_coucher().toLocalDateTime()))
                .collect(Collectors.toList());

        if (!nuitsTriees.isEmpty()) {
            double[] sommeilData = nuitsTriees.stream().mapToDouble(s ->
                    ChronoUnit.MINUTES.between(
                            s.getDate_coucher().toLocalDateTime(),
                            s.getDate_reveil().toLocalDateTime()) / 60.0).toArray();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
            String[] sommeilLabels = nuitsTriees.stream()
                    .map(s -> s.getDate_coucher().toLocalDateTime().format(dtf))
                    .toArray(String[]::new);
            dessinerGraphiqueLigne(cb, writer, doc, sommeilData, sommeilLabels, "Nuits", "Heures de sommeil", new BaseColor(170, 0, 220));
        } else {
            doc.add(new Paragraph("Aucune donnee de sommeil enregistree.",
                    new com.itextpdf.text.Font(bf, 10, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY)));
        }

        // ── Message de clôture ─────────────────────────────────────────────
        doc.add(new Paragraph("\n"));
        Paragraph closing = new Paragraph(
                "Bravo pour votre suivi ! Ce rapport Harmony resume votre parcours de sante.\n" +
                        "Continuez a tracker, progresser et prendre soin de vous chaque jour.", fontClosing);
        closing.setAlignment(Element.ALIGN_CENTER);
        closing.setSpacingBefore(20);
        doc.add(closing);

        doc.close();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers PDF
    // ──────────────────────────────────────────────────────────────────────────

    /** Cellule KPI (valeur en gros + label en petit) */
    private PdfPCell creerCelluleKpi(BaseFont bfBold, BaseFont bf, String label, String valeur) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(GRIS_SEPARATEUR);
        cell.setBorderWidth(1f);
        cell.setBackgroundColor(VIOLET_VERY_LIGHT);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p = new Paragraph();
        p.add(new Phrase(valeur + "\n", new com.itextpdf.text.Font(bfBold, 16, com.itextpdf.text.Font.NORMAL, VIOLET_HARMONY)));
        p.add(new Phrase(label, new com.itextpdf.text.Font(bf, 9, com.itextpdf.text.Font.NORMAL, new BaseColor(120, 120, 120))));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    /** Tableau de données (2 colonnes : indicateur / valeur) avec en-têtes violets */
    private PdfPTable creerTableauSection(String[] headers, String[][] lignes,
                                          BaseFont bfBold, BaseFont bf) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2.5f});
        table.setSpacingAfter(12);

        // En-têtes
        for (String h : headers) {
            PdfPCell header = new PdfPCell(new Phrase(h,
                    new com.itextpdf.text.Font(bfBold, 11, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE)));
            header.setBackgroundColor(VIOLET_HARMONY);
            header.setPadding(10);
            header.setBorder(Rectangle.NO_BORDER);
            table.addCell(header);
        }

        // Lignes alternées
        for (int i = 0; i < lignes.length; i++) {
            BaseColor bg = (i % 2 == 0) ? new BaseColor(250, 248, 255) : BaseColor.WHITE;
            for (String val : lignes[i]) {
                PdfPCell cell = new PdfPCell(new Phrase(val,
                        new com.itextpdf.text.Font(bf, 10, com.itextpdf.text.Font.NORMAL, new BaseColor(50, 50, 50))));
                cell.setBackgroundColor(bg);
                cell.setPadding(9);
                cell.setBorderColor(GRIS_SEPARATEUR);
                table.addCell(cell);
            }
        }
        return table;
    }

    /** Dessine un trait séparateur coloré */
    private Paragraph traitSeparateur(PdfContentByte cb, PdfWriter writer, Document doc) {
        Paragraph p = new Paragraph();
        p.add(new Phrase("_________________________________________" +
                "__________________________________________________",
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 1,
                        com.itextpdf.text.Font.NORMAL, VIOLET_LIGHT)));
        p.setSpacingAfter(4);
        return p;
    }

    /** Dessine un bandeau de section violet en haut de page */
    private void dessinerBandeauSection(PdfContentByte cb, PdfWriter writer,
                                        String texte, BaseFont bfBold, float yPos) throws DocumentException {
        cb.saveState();
        cb.setColorFill(VIOLET_HARMONY);
        cb.roundRectangle(45, yPos, 505, 44, 6);
        cb.fill();
        cb.setColorFill(BaseColor.WHITE);
        cb.beginText();
        cb.setFontAndSize(bfBold, 16);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, texte, 297, yPos + 14, 0);
        cb.endText();
        cb.restoreState();
    }

    /**
     * Dessine un graphique en ligne avec iText 5 PdfContentByte.
     * Utilise doc.add(new Paragraph(" ")) pour réserver l'espace vertical.
     */
    private void dessinerGraphiqueLigne(PdfContentByte cb, PdfWriter writer, Document doc,
                                        double[] data, String[] labels,
                                        String axeX, String axeY,
                                        BaseColor couleurLigne) throws DocumentException {
        // Position du graphique sur la page
        float chartX  = 80;
        float chartW  = 420;
        float chartH  = 130;

        // On saute l'espace dans le flux iText pour que la suite du texte ne chevauchant pas
        doc.add(new Paragraph(" "));

        // Position Y courante approximative (le writer connaît la position verticale du curseur)
        float yBase = writer.getVerticalPosition(false) - chartH - 20;
        if (yBase < 60) {
            doc.newPage();
            yBase = PageSize.A4.getHeight() - 100;
        }

        float chartY = yBase;

        // Fond blanc avec bordure
        cb.saveState();
        cb.setColorFill(BaseColor.WHITE);
        cb.setColorStroke(GRIS_SEPARATEUR);
        cb.setLineWidth(0.8f);
        cb.roundRectangle(chartX - 10, chartY - 10, chartW + 20, chartH + 30, 6);
        cb.fillStroke();

        // Grille horizontale en gris clair
        int nbGrilles = 5;
        cb.setColorStroke(new BaseColor(230, 230, 230));
        cb.setLineWidth(0.4f);
        for (int g = 0; g <= nbGrilles; g++) {
            float y = chartY + (chartH * g / nbGrilles);
            cb.moveTo(chartX, y);
            cb.lineTo(chartX + chartW, y);
            cb.stroke();
        }

        // Axe X et Y
        cb.setColorStroke(new BaseColor(180, 180, 180));
        cb.setLineWidth(0.8f);
        cb.moveTo(chartX, chartY);
        cb.lineTo(chartX, chartY + chartH);
        cb.stroke();
        cb.moveTo(chartX, chartY);
        cb.lineTo(chartX + chartW, chartY);
        cb.stroke();

        if (data == null || data.length == 0) {
            cb.restoreState();
            doc.add(new Paragraph(" \n"));
            return;
        }

        // Calcul min/max pour l'échelle
        double maxVal = Arrays.stream(data).max().orElse(1);
        double minVal = Arrays.stream(data).min().orElse(0);
        if (maxVal <= 0) maxVal = 1;
        double range = maxVal - minVal;
        if (range == 0) range = maxVal;

        // Tracer la ligne de données
        cb.setColorStroke(couleurLigne);
        cb.setLineWidth(1.8f);
        float stepX = data.length > 1 ? chartW / (data.length - 1) : chartW / 2;

        for (int i = 0; i < data.length; i++) {
            float px = chartX + i * stepX;
            float py = chartY + (float) ((data[i] - minVal) / range * chartH * 0.85);
            if (i == 0) cb.moveTo(px, py);
            else        cb.lineTo(px, py);
        }
        cb.stroke();

        // Points de données
        cb.setColorFill(couleurLigne);
        for (int i = 0; i < data.length; i++) {
            float px = chartX + i * stepX;
            float py = chartY + (float) ((data[i] - minVal) / range * chartH * 0.85);
            cb.circle(px, py, 3);
            cb.fill();
        }

        // Labels axe X (limités à 10 max pour lisibilité)
        cb.setColorFill(new BaseColor(100, 100, 100));
        try {
            BaseFont labelFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.setFontAndSize(labelFont, 7);
            int step = Math.max(1, data.length / 10);
            for (int i = 0; i < data.length; i += step) {
                float px = chartX + i * stepX;
                cb.beginText();
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER,
                        i < labels.length ? labels[i] : ("" + (i + 1)), px, chartY - 12, 0);
                cb.endText();
            }
            // Label axe X
            cb.beginText();
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, axeX, chartX + chartW / 2, chartY - 22, 0);
            cb.endText();
            // Label axe Y (valeur max)
            cb.setFontAndSize(labelFont, 7);
            cb.beginText();
            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, String.format("%.0f", maxVal), chartX - 5, chartY + chartH - 5, 0);
            cb.endText();
            cb.beginText();
            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, "0", chartX - 5, chartY, 0);
            cb.endText();
        } catch (Exception ignored) {}

        cb.restoreState();

        // Réserver l'espace vertical pour le graphique dans le flux iText
        for (int sp = 0; sp < 8; sp++) doc.add(new Paragraph(" "));
    }

    private void afficherInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Harmony");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }}