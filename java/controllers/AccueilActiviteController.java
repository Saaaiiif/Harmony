package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;    // ← CORRECT : GridPane est dans javafx.scene.layout
import javafx.scene.layout.StackPane;   // ← CORRECT : StackPane est dans javafx.scene.layout
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.application.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

// iTextPDF — on n'importe PAS TextField de ce package pour éviter l'ambiguïté
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import services.ServiceActivite;
import services.ServiceAliment;
import services.ServiceConsommation;
import services.ServiceSommeil;
import models.Activite;
import models.Aliment;
import models.Consommation;
import models.Sommeil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// ── Stockage des préférences via fichier .properties (remplace java.util.prefs) ──
import java.util.Properties;

public class AccueilActiviteController {

    // ==========================================================================
    // OBJECTIFS PARTAGÉS
    // ==========================================================================
    public static int OBJECTIF_CALORIES_JOURNALIER = 2150;
    public static int OBJECTIF_EAU_ML              = 2000;

    // ==========================================================================
    // STOCKAGE PRÉFÉRENCES — fichier .properties dans le dossier utilisateur
    // (remplace java.util.prefs.Preferences pour éviter l'erreur de module)
    // ==========================================================================
    private static final String PREFS_FILE =
            System.getProperty("user.home") + File.separator + "harmony_prefs.properties";

    private Properties prefs = new Properties();

    private void chargerFichierPrefs() {
        try (InputStream in = new FileInputStream(PREFS_FILE)) {
            prefs.load(in);
        } catch (Exception e) {
            // Fichier absent au premier lancement → valeurs par défaut
        }
    }

    private void sauvegarderFichierPrefs() {
        try (OutputStream out = new FileOutputStream(PREFS_FILE)) {
            prefs.store(out, "Harmony Preferences");
        } catch (Exception e) {
            System.out.println("Impossible de sauvegarder les préférences : " + e.getMessage());
        }
    }

    private int    getInt(String key, int    def) { return Integer.parseInt(prefs.getProperty(key, String.valueOf(def))); }
    private double getDbl(String key, double def) { return Double.parseDouble(prefs.getProperty(key, String.valueOf(def))); }
    private String getStr(String key, String def) { return prefs.getProperty(key, def); }
    private void   setVal(String key, Object val) { prefs.setProperty(key, String.valueOf(val)); }

    // ==========================================================================
    // 100 CITATIONS SPÉCIALISÉES EN FRANÇAIS
    // ==========================================================================
    private static final String[][] CITATIONS_FR = {
            // ── NUTRITION (25) ──
            {"Que ton aliment soit ta première médecine.", "Hippocrate"},
            {"Manger sainement n'est pas une punition, c'est un cadeau que tu te fais chaque jour.", "Anonyme"},
            {"Tu es ce que tu manges. Alors ne sois pas rapide, bon marché, facile ou faux.", "Anonyme"},
            {"Une bonne nutrition est la base de toute performance physique.", "Anonyme"},
            {"Le corps est un temple. Nourrissez-le avec respect et il vous servira toute une vie.", "Anonyme"},
            {"Les légumes sont le carburant de tes ambitions.", "Anonyme"},
            {"Manger, c'est un acte d'amour envers soi-même.", "Anonyme"},
            {"La nutrition n'est pas une question de restriction, c'est une question d'équilibre.", "Anonyme"},
            {"Un repas bien préparé vaut mille médicaments.", "Proverbe"},
            {"L'alimentation équilibrée est le fondement d'un esprit sain.", "Anonyme"},
            {"Ce que tu mets dans ton assiette détermine ce que tu deviens.", "Anonyme"},
            {"Chaque repas est une opportunité de nourrir ton potentiel.", "Anonyme"},
            {"Les protéines construisent les muscles, la discipline construit le caractère.", "Anonyme"},
            {"Hydratez-vous. L'eau est le moteur de votre machine corporelle.", "Anonyme"},
            {"Une alimentation riche en nutriments est le meilleur investissement de santé.", "Anonyme"},
            {"Cuisiner pour soi, c'est se respecter.", "Anonyme"},
            {"Les calories comptent, mais la qualité de tes aliments compte encore plus.", "Anonyme"},
            {"Mangez des couleurs — chaque couleur dans votre assiette est un nutriment différent.", "Anonyme"},
            {"Le sucre est agréable une seconde, la santé est agréable toute la vie.", "Anonyme"},
            {"Planifier ses repas, c'est planifier sa réussite.", "Anonyme"},
            {"Les vitamines et minéraux sont les engrenages invisibles de votre moteur corporel.", "Anonyme"},
            {"Ne mangez pas moins — mangez mieux.", "Anonyme"},
            {"Votre fourchette est plus puissante que n'importe quel médicament.", "Mark Hyman"},
            {"Un intestin sain est la clé d'un esprit sain.", "Hippocrate"},
            {"Nourrissez votre corps avec intention, pas avec impulsion.", "Anonyme"},
            // ── SPORT (25) ──
            {"Le sport ne construit pas le caractère, il le révèle.", "Heywood Broun"},
            {"Chaque séance d'entraînement est un dépôt dans la banque de votre santé.", "Anonyme"},
            {"La douleur d'aujourd'hui est la force de demain.", "Anonyme"},
            {"L'exercice physique est la meilleure thérapie qui soit.", "Anonyme"},
            {"Votre corps peut tout supporter. C'est votre esprit qu'il faut convaincre.", "Anonyme"},
            {"Un kilomètre de plus aujourd'hui, une vie plus longue demain.", "Anonyme"},
            {"Le mouvement est la médecine de la création d'un changement durable.", "Carol Welch"},
            {"Chaque répétition vous rapproche de la meilleure version de vous-même.", "Anonyme"},
            {"Il n'y a pas de raccourci vers la forme physique.", "Anonyme"},
            {"L'entraînement d'hier est la base d'aujourd'hui.", "Anonyme"},
            {"Bougez chaque jour — votre corps n'est pas fait pour rester immobile.", "Anonyme"},
            {"Le meilleur moment pour s'entraîner était hier. Le second meilleur, c'est maintenant.", "Anonyme"},
            {"La régularité bat l'intensité. Mieux vaut bouger un peu chaque jour.", "Anonyme"},
            {"Le sport, c'est l'art de repousser ses propres limites.", "Anonyme"},
            {"Vos jambes ne sont pas fatiguées — c'est votre tête qui l'est.", "Anonyme"},
            {"Transpirer, c'est graisser les rouages de votre potentiel.", "Anonyme"},
            {"Chaque muscle développé est une victoire sur votre ancienne version.", "Anonyme"},
            {"Le cardio entraîne le cœur. La musculation forge le caractère.", "Anonyme"},
            {"Faites de votre entraînement votre moment sacré de la journée.", "Anonyme"},
            {"Votre corps est la seule machine qui s'améliore avec l'utilisation.", "Anonyme"},
            {"La forme physique n'est pas un état — c'est un mode de vie.", "Anonyme"},
            {"Ceux qui n'ont pas le temps pour l'exercice devront en trouver pour la maladie.", "Edward Stanley"},
            {"Un athlète vit dans son corps. Traitez-le comme un palais.", "Anonyme"},
            {"Commencez doucement. Persévérez longtemps. Les résultats viennent toujours.", "Anonyme"},
            {"Le vrai entraînement commence quand vous voulez arrêter.", "Anonyme"},
            // ── SOMMEIL (25) ──
            {"Le sommeil est la meilleure méditation.", "Dalaï Lama"},
            {"Un bon sommeil est le fondement de tout progrès physique et mental.", "Anonyme"},
            {"Dormir suffisamment n'est pas un luxe — c'est une nécessité biologique.", "Anonyme"},
            {"Le corps se répare la nuit. Ne volez pas ces heures précieuses.", "Anonyme"},
            {"Le sommeil est l'arme secrète des champions.", "Anonyme"},
            {"Une nuit de récupération de qualité vaut autant que deux séances d'entraînement.", "Anonyme"},
            {"Les muscles ne poussent pas pendant l'entraînement — ils poussent pendant le sommeil.", "Anonyme"},
            {"Respectez votre sommeil et votre sommeil respectera vos objectifs.", "Anonyme"},
            {"La fatigue est l'ennemi de la performance. Le sommeil en est le remède.", "Anonyme"},
            {"Chaque heure de sommeil est un investissement dans votre santé de demain.", "Anonyme"},
            {"Dormir, c'est se recharger — votre cerveau a besoin de 100 %.", "Anonyme"},
            {"Sans récupération, il n'y a pas de progression. Le repos est partie du plan.", "Anonyme"},
            {"Le sommeil régule vos hormones, vos émotions et votre appétit.", "Anonyme"},
            {"Un étudiant qui dort bien apprend deux fois mieux.", "Anonyme"},
            {"La qualité du sommeil détermine la qualité de votre éveil.", "Anonyme"},
            {"Aller au lit tôt est l'habitude la plus sous-estimée des gens en bonne santé.", "Anonyme"},
            {"Votre lit est un laboratoire de récupération. Utilisez-le intelligemment.", "Anonyme"},
            {"Le manque de sommeil vole votre énergie, votre concentration et votre joie.", "Anonyme"},
            {"Dormez comme si votre demain en dépend — parce que c'est le cas.", "Anonyme"},
            {"Un sommeil profond est la cure contre le stress, la fatigue et la mauvaise humeur.", "Anonyme"},
            {"La nuit porte conseil, mais seulement si vous dormez suffisamment.", "Anonyme"},
            {"Huit heures de sommeil transforment un guerrier épuisé en un guerrier prêt.", "Anonyme"},
            {"Votre heure de coucher est aussi importante que votre heure d'entraînement.", "Anonyme"},
            {"La récupération n'est pas la faiblesse — c'est la sagesse.", "Anonyme"},
            {"Fermez les yeux sur les distractions. Ouvrez-les sur vos rêves.", "Anonyme"},
            // ── OBJECTIFS (25) ──
            {"Un objectif sans plan n'est qu'un souhait.", "Antoine de Saint-Exupéry"},
            {"La discipline est de choisir entre ce que vous voulez maintenant et ce que vous voulez le plus.", "Abraham Lincoln"},
            {"Fixez vos objectifs en grand. Commencez en petit. Agissez maintenant.", "Anonyme"},
            {"La constance est le superpouvoir des personnes ordinaires qui obtiennent des résultats extraordinaires.", "Anonyme"},
            {"Votre seule compétition, c'est la personne que vous étiez hier.", "Anonyme"},
            {"Chaque petit progrès mérite d'être célébré.", "Anonyme"},
            {"La motivation vous lance. L'habitude vous maintient en mouvement.", "Jim Ryun"},
            {"Ne cherchez pas la perfection — cherchez la progression.", "Anonyme"},
            {"Les objectifs transforment un effort aléatoire en direction ciblée.", "Anonyme"},
            {"Ceux qui ont un 'pourquoi' peuvent supporter n'importe quel 'comment'.", "Nietzsche"},
            {"Chaque journée sans progrès est une journée perdue. Chaque petit pas compte.", "Anonyme"},
            {"Le succès est la somme de petits efforts répétés jour après jour.", "Robert Collier"},
            {"Votre corps entend tout ce que votre esprit dit. Soyez positif.", "Anonyme"},
            {"Tracez votre chemin. Mesurez vos progrès. Ajustez votre cap.", "Anonyme"},
            {"Un objectif bien défini est à moitié atteint.", "Anonyme"},
            {"La volonté est un muscle — plus vous l'exercez, plus elle devient forte.", "Anonyme"},
            {"Commencez là où vous êtes, utilisez ce que vous avez, faites ce que vous pouvez.", "Arthur Ashe"},
            {"Se fixer des limites est le seul moyen de les dépasser.", "Anonyme"},
            {"Les habitudes d'aujourd'hui bâtissent la santé de demain.", "Anonyme"},
            {"Chaque refus d'une mauvaise habitude est une victoire sur vous-même.", "Anonyme"},
            {"Votre journal de bord est votre meilleur entraîneur.", "Anonyme"},
            {"Mesurer ses progrès, c'est maintenir sa motivation.", "Anonyme"},
            {"La persévérance n'est pas un long marathon — c'est beaucoup de courts sprints.", "Walter Elliot"},
            {"Un étudiant en bonne santé est un étudiant qui réussit.", "Anonyme"},
            {"Harmony n'est pas une application — c'est votre partenaire de vie saine.", "Harmony"}
    };

    // ==========================================================================
    // CHAMPS FXML
    // ==========================================================================
    @FXML private Label      lblDateJour;
    @FXML private StackPane  circularCaloriesPane;
    @FXML private Label      lblObjectifBase;
    @FXML private Label      lblAlimentsKcal;
    @FXML private Label      lblExercicesKcal;
    @FXML private Label      lblAlerteCalories;
    @FXML private Label      lblQuoteText;
    @FXML private Label      lblQuoteAuthor;
    @FXML private Label      lblObjCal;
    @FXML private Label      lblObjGluG;
    @FXML private Label      lblObjGluPct;
    @FXML private Label      lblObjLipG;
    @FXML private Label      lblObjLipPct;
    @FXML private Label      lblObjProtG;
    @FXML private Label      lblObjProtPct;
    @FXML private Label      lblObjCalBrulees;
    @FXML private Label      lblObjEntrainements;
    @FXML private Label      lblObjMinutes;
    @FXML private Label      lblObjEau;
    @FXML private StackPane  overlayObjectifs;
    // ── Champs du Centre d'Alertes (remplacent l'ancien SMS) ──
    @FXML private VBox       vboxAlertes;
    @FXML private Label      lblDerniereVerif;

    // ==========================================================================
    // ÉTAT INTERNE
    // ==========================================================================
    private LocalDate dateSelectionnee      = LocalDate.now();
    private int    objectifCalories         = 2150;
    private double objectifGluPct           = 30.0;
    private double objectifLipPct           = 10.0;
    private double objectifProtPct          = 60.0;
    private int    objectifEauMl            = 2000;
    private int    objCalBruleesParSemaine  = 0;
    private int    objEntrainements         = 0;
    private int    objMinutes               = 0;
    private String phoneNumber              = "";
    private int    dernierIndexCitation     = -1;

    private final ServiceConsommation serviceConsommation = new ServiceConsommation();
    private final ServiceActivite     serviceActivite     = new ServiceActivite();
    private final ServiceAliment      serviceAliment      = new ServiceAliment();

    // ==========================================================================
    // NAVIGATION
    // ==========================================================================
    @FXML void goToAccueil(ActionEvent e)   { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent e)  { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent e) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent e)   { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    // ==========================================================================
    // INITIALISATION
    // ==========================================================================
    @FXML
    public void initialize() {
        chargerPreferences();
        afficherObjectifs();
        actualiserDate();
        actualiserCalories();
        chargerCitation();
        // Lance la vérification des alertes automatiquement au chargement
        Platform.runLater(this::verifierAlertes);
    }

    // ==========================================================================
    // NAVIGATION DATE
    // ==========================================================================
    @FXML void jourPrecedent(ActionEvent e) {
        dateSelectionnee = dateSelectionnee.minusDays(1);
        actualiserDate(); actualiserCalories();
    }
    @FXML void jourSuivant(ActionEvent e) {
        if (dateSelectionnee.isBefore(LocalDate.now())) {
            dateSelectionnee = dateSelectionnee.plusDays(1);
            actualiserDate(); actualiserCalories();
        }
    }
    @FXML void allerAujourdhui(ActionEvent e) {
        dateSelectionnee = LocalDate.now();
        actualiserDate(); actualiserCalories();
    }

    private void actualiserDate() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        String texte = dateSelectionnee.equals(LocalDate.now())
                ? "Aujourd'hui \u2014 " + dateSelectionnee.format(fmt)
                : dateSelectionnee.format(fmt);
        lblDateJour.setText(Character.toUpperCase(texte.charAt(0)) + texte.substring(1));
    }

    // ==========================================================================
    // WIDGET CALORIES
    // ==========================================================================
    private void actualiserCalories() {
        Map<Integer, Aliment> alimentMap = new HashMap<>();
        serviceAliment.afficherTout().forEach(a -> alimentMap.put(a.getId_aliment(), a));

        List<Consommation> duJour = serviceConsommation.afficherTout().stream()
                .filter(c -> c.getDate_consommation() != null &&
                        c.getDate_consommation().toLocalDateTime()
                                .toLocalDate().equals(dateSelectionnee))
                .collect(Collectors.toList());

        int caloriesConsommees = duJour.stream().mapToInt(c -> {
            Aliment a = alimentMap.get(c.getId_aliment());
            if (a == null) return 0;
            return (int) Math.round(
                    (a.getCalories_pour_100g() * (double) c.getPoids_grammes()) / 100.0);
        }).sum();

        int caloriesExercice = serviceActivite.afficherTout().stream()
                .filter(a -> a.getDate_activite() != null &&
                        a.getDate_activite().toLocalDateTime()
                                .toLocalDate().equals(dateSelectionnee))
                .mapToInt(Activite::getCalories_brulees)
                .sum();

        int restant = objectifCalories - caloriesConsommees + caloriesExercice;

        lblObjectifBase.setText(String.valueOf(objectifCalories));
        lblAlimentsKcal.setText(String.valueOf(caloriesConsommees));
        lblExercicesKcal.setText(String.valueOf(caloriesExercice));

        dessinerCercleCalories(restant, objectifCalories);

        if (caloriesConsommees > objectifCalories) {
            int depasse = caloriesConsommees - objectifCalories;
            lblAlerteCalories.setText("\u26a0\ufe0f Objectif dépassé de " + depasse + " kcal !");
            // Relancer la vérification des alertes pour mettre à jour le centre d'alertes
            Platform.runLater(this::verifierAlertes);
        } else {
            lblAlerteCalories.setText("");
        }
    }

    private void dessinerCercleCalories(int restant, int objectif) {
        circularCaloriesPane.getChildren().clear();
        double size   = 145;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setStroke(Color.web("#ede7f6"));
        gc.setLineWidth(13);
        gc.strokeArc(13, 13, size - 26, size - 26, 0, 360, ArcType.OPEN);

        double consomme = objectif - restant;
        double progress = Math.min(1.0, Math.max(0.0, consomme / Math.max(objectif, 1)));
        double angleDeg = 360.0 * progress;
        String couleur  = restant < 0 ? "#c62828" : "#6a1b9a";
        gc.setStroke(Color.web(couleur));
        gc.setLineWidth(13);
        gc.strokeArc(13, 13, size - 26, size - 26, 90, -angleDeg, ArcType.OPEN);

        Label lblVal = new Label(String.valueOf(Math.abs(restant)));
        lblVal.setStyle("-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: "
                + (restant < 0 ? "#c62828" : "#4a148c") + ";");
        Label lblTxt = new Label(restant >= 0 ? "Restant" : "Dépassé");
        lblTxt.setStyle("-fx-font-size: 12px; -fx-text-fill: #9e9e9e;");

        VBox center = new VBox(2, lblVal, lblTxt);
        center.setAlignment(Pos.CENTER);
        circularCaloriesPane.getChildren().addAll(canvas, center);
    }

    // ==========================================================================
    // CENTRE D'ALERTES HARMONY (remplace SMS TextBelt)
    // ==========================================================================

    /** Appelé par le bouton "Actualiser les alertes" ET automatiquement au lancement */
    @FXML
    void verifierAlertes(ActionEvent e) { verifierAlertes(); }

    void verifierAlertes() {
        if (vboxAlertes == null) return;
        vboxAlertes.getChildren().clear();

        boolean auMoinsUneAlerte = false;
        LocalDate today = LocalDate.now();

        // ── ALERTE 1 : Calories dépassées ──────────────────────────────────
        Map<Integer, Aliment> alimentMap = new HashMap<>();
        serviceAliment.afficherTout().forEach(a -> alimentMap.put(a.getId_aliment(), a));

        int caloriesToday = serviceConsommation.afficherTout().stream()
                .filter(c -> c.getDate_consommation() != null &&
                        c.getDate_consommation().toLocalDateTime().toLocalDate().equals(today))
                .mapToInt(c -> {
                    Aliment a = alimentMap.get(c.getId_aliment());
                    return (a == null) ? 0 :
                            (int) Math.round(a.getCalories_pour_100g() * (double) c.getPoids_grammes() / 100.0);
                }).sum();

        if (caloriesToday > objectifCalories) {
            int depasse = caloriesToday - objectifCalories;
            ajouterCarteAlerte(vboxAlertes,
                    "🔥 Calories dépassées !",
                    "Vous avez dépassé votre objectif de " + depasse + " kcal aujourd'hui.\n" +
                            "Pensez à faire une séance de sport pour compenser.",
                    "#c62828", "#fff3e0");
            auMoinsUneAlerte = true;
        }

        // ── ALERTE 2 : Aucun repas enregistré aujourd'hui ──────────────────
        long repasAujourdhui = serviceConsommation.afficherTout().stream()
                .filter(c -> c.getDate_consommation() != null &&
                        c.getDate_consommation().toLocalDateTime().toLocalDate().equals(today))
                .count();

        if (repasAujourdhui == 0) {
            ajouterCarteAlerte(vboxAlertes,
                    "🍽️ Journal alimentaire vide !",
                    "Vous n'avez encore rien enregistré dans votre journal nutritionnel aujourd'hui." +
                            "\nN'oubliez pas de tracker vos repas !",
                    "#e65100", "#fff8e1");
            auMoinsUneAlerte = true;
        }

        // ── ALERTE 3 : Aucun entraînement cette semaine ────────────────────
        LocalDate lundiSemaine = today.minusDays(today.getDayOfWeek().getValue() - 1);
        long seancesSemaine = serviceActivite.afficherTout().stream()
                .filter(a -> a.getDate_activite() != null)
                .filter(a -> {
                    LocalDate d = a.getDate_activite().toLocalDateTime().toLocalDate();
                    return !d.isBefore(lundiSemaine) && !d.isAfter(today);
                }).count();

        if (seancesSemaine == 0) {
            ajouterCarteAlerte(vboxAlertes,
                    "💪 Pas d'entraînement cette semaine !",
                    "Vous n'avez enregistré aucune séance depuis lundi.\n" +
                            "Objectif : " + objEntrainements + " entraînement(s)/semaine.",
                    "#1565c0", "#e3f2fd");
            auMoinsUneAlerte = true;
        } else if (objEntrainements > 0 && seancesSemaine < objEntrainements) {
            long restantes = objEntrainements - seancesSemaine;
            ajouterCarteAlerte(vboxAlertes,
                    "📊 Objectif sport en cours",
                    "Cette semaine : " + seancesSemaine + "/" + objEntrainements + " séances réalisées.\n" +
                            restantes + " séance(s) restante(s) pour atteindre votre objectif !",
                    "#1b5e20", "#e8f5e9");
            auMoinsUneAlerte = true;
        }

        // ── PAS D'ALERTE = Message positif ─────────────────────────────────
        if (!auMoinsUneAlerte) {
            ajouterCarteAlerte(vboxAlertes,
                    "✅ Tout est parfait !",
                    "Aucune alerte pour aujourd'hui. Continuez sur cette lancée, vous êtes en bonne voie !",
                    "#2e7d32", "#e8f5e9");
        }

        // Horodatage dernière vérification
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        if (lblDerniereVerif != null)
            lblDerniereVerif.setText("Dernière vérif. : " +
                    java.time.LocalTime.now().format(fmt));
    }

    /** Crée une carte d'alerte colorée et animée dans le vbox */
    private void ajouterCarteAlerte(VBox parent, String titre, String message,
                                    String textColor, String bgColor) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; " +
                "-fx-padding: 10 14; -fx-border-color: " + textColor + "; " +
                "-fx-border-width: 0 0 0 4; -fx-border-radius: 10;");
        card.setOpacity(0);

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + textColor + ";");
        lblTitre.setWrapText(true);

        Label lblMsg = new Label(message);
        lblMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        lblMsg.setWrapText(true);

        card.getChildren().addAll(lblTitre, lblMsg);
        parent.getChildren().add(card);

        // Animation d'apparition
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0.0); ft.setToValue(1.0);
        ft.setDelay(Duration.millis(parent.getChildren().size() * 80L));
        ft.play();
    }

    // ==========================================================================
    // ANCIENS CHAMPS SMS — conservés pour éviter NullPointerException
    // (les fx:id sont absents du FXML, les champs seront null, les méthodes
    //  sont gardées mais ne font rien si les champs sont null)
    // ==========================================================================
    @FXML void sauvegarderTelephone(ActionEvent e) { /* désactivé - remplacé par alertes in-app */ }
    private void setPhoneStatus(String msg, String color) { /* désactivé */ }
    private void envoyerSMSAlerte(int caloriesConsommees) { /* désactivé - logique migrée dans verifierAlertes() */ }

    // ==========================================================================
    // CITATION — 100% FRANÇAIS
    // ==========================================================================
    @FXML
    void changerCitation(ActionEvent e) {
        lblQuoteText.setText("\u2728 Chargement d'une nouvelle citation...");
        lblQuoteAuthor.setText("\u2014");
        chargerCitation();
    }

    private void chargerCitation() {
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(7)).build();

                HttpRequest reqZen = HttpRequest.newBuilder()
                        .uri(URI.create("https://zenquotes.io/api/random"))
                        .timeout(java.time.Duration.ofSeconds(7)).GET().build();

                HttpResponse<String> resZen =
                        client.send(reqZen, HttpResponse.BodyHandlers.ofString());

                if (resZen.statusCode() != 200 || resZen.body() == null
                        || resZen.body().isBlank()) {
                    afficherCitationLocale(); return;
                }

                String quoteEn = extraireStringJSON(resZen.body(), "\"q\":\"");
                String author  = extraireStringJSON(resZen.body(), "\"a\":\"");
                if (quoteEn.isEmpty()) { afficherCitationLocale(); return; }

                String quoteFr = null;
                try {
                    String encoded = URLEncoder.encode(quoteEn, StandardCharsets.UTF_8);
                    HttpRequest reqTrad = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.mymemory.translated.net/get?q="
                                    + encoded + "&langpair=en|fr"))
                            .timeout(java.time.Duration.ofSeconds(7)).GET().build();
                    HttpResponse<String> resTrad =
                            client.send(reqTrad, HttpResponse.BodyHandlers.ofString());
                    if (resTrad.statusCode() == 200 && resTrad.body() != null) {
                        String translated =
                                extraireStringJSON(resTrad.body(), "\"translatedText\":\"");
                        if (!translated.isEmpty()
                                && !translated.contains("MYMEMORY WARNING")
                                && !translated.equalsIgnoreCase(quoteEn)
                                && translated.length() > 5) {
                            quoteFr = translated;
                        }
                    }
                } catch (Exception ignored) {}

                if (quoteFr == null || quoteFr.isBlank()) { afficherCitationLocale(); return; }

                final String fq = nettoyerTexte(quoteFr);
                final String fa = nettoyerTexte(author);
                afficherCitationAvecAnimation(fq, fa);

            } catch (Exception ex) {
                afficherCitationLocale();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void afficherCitationLocale() {
        int idx;
        do { idx = (int) (Math.random() * CITATIONS_FR.length); }
        while (idx == dernierIndexCitation && CITATIONS_FR.length > 1);
        dernierIndexCitation = idx;
        afficherCitationAvecAnimation(CITATIONS_FR[idx][0], CITATIONS_FR[idx][1]);
    }

    private void afficherCitationAvecAnimation(String quote, String author) {
        Platform.runLater(() -> {
            FadeTransition fo = new FadeTransition(Duration.millis(180), lblQuoteText);
            fo.setFromValue(1.0); fo.setToValue(0.0);
            fo.setOnFinished(ev -> {
                String display = quote.startsWith("\u201C")
                        ? quote : "\u201C" + quote + "\u201D";
                lblQuoteText.setText(display);
                lblQuoteAuthor.setText("\u2014 " + (author.isEmpty() ? "Anonyme" : author));
                FadeTransition fi = new FadeTransition(Duration.millis(450), lblQuoteText);
                fi.setFromValue(0.0); fi.setToValue(1.0); fi.play();
                FadeTransition fia = new FadeTransition(Duration.millis(550), lblQuoteAuthor);
                fia.setFromValue(0.0); fia.setToValue(1.0); fia.play();
            });
            fo.play();
        });
    }

    // ==========================================================================
    // PRÉFÉRENCES
    // ==========================================================================
    private void chargerPreferences() {
        chargerFichierPrefs();
        objectifCalories        = getInt("objectifCalories", 2150);
        objectifGluPct          = getDbl("objectifGluPct",   30.0);
        objectifLipPct          = getDbl("objectifLipPct",   10.0);
        objectifProtPct         = getDbl("objectifProtPct",  60.0);
        objectifEauMl           = getInt("objectifEauMl",    2000);
        objCalBruleesParSemaine = getInt("objCalBrulees",       0);
        objEntrainements        = getInt("objEntrainements",    0);
        objMinutes              = getInt("objMinutes",           0);
        phoneNumber             = getStr("phoneNumber",         "");
        OBJECTIF_CALORIES_JOURNALIER = objectifCalories;
        OBJECTIF_EAU_ML              = objectifEauMl;
    }

    private void sauvegarderPreferences() {
        setVal("objectifCalories",  objectifCalories);
        setVal("objectifGluPct",    objectifGluPct);
        setVal("objectifLipPct",    objectifLipPct);
        setVal("objectifProtPct",   objectifProtPct);
        setVal("objectifEauMl",     objectifEauMl);
        setVal("objCalBrulees",     objCalBruleesParSemaine);
        setVal("objEntrainements",  objEntrainements);
        setVal("objMinutes",        objMinutes);
        OBJECTIF_CALORIES_JOURNALIER = objectifCalories;
        OBJECTIF_EAU_ML              = objectifEauMl;
        sauvegarderFichierPrefs();
    }

    private void afficherObjectifs() {
        double gluG  = Math.round((objectifCalories * objectifGluPct  / 100.0) / 4.0);
        double lipG  = Math.round((objectifCalories * objectifLipPct  / 100.0) / 9.0);
        double protG = Math.round((objectifCalories * objectifProtPct / 100.0) / 4.0);
        lblObjCal.setText(String.valueOf(objectifCalories));
        lblObjGluG.setText((int)gluG  + " g");
        lblObjGluPct.setText((int)objectifGluPct  + " %");
        lblObjLipG.setText((int)lipG  + " g");
        lblObjLipPct.setText((int)objectifLipPct  + " %");
        lblObjProtG.setText((int)protG + " g");
        lblObjProtPct.setText((int)objectifProtPct + " %");
        lblObjCalBrulees.setText(objCalBruleesParSemaine + " Calories");
        lblObjEntrainements.setText(objEntrainements
                + " entraînement" + (objEntrainements > 1 ? "s" : ""));
        lblObjMinutes.setText(String.valueOf(objMinutes));
        lblObjEau.setText(objectifEauMl + " ml");
    }

    // ==========================================================================
    // MODAL MODIFICATION OBJECTIFS
    // ==========================================================================
    @FXML
    void ouvrirModificationObjectifs(ActionEvent e) {

        // ── Crée des champs JavaFX TextField (type qualifié complet pour éviter l'ambiguïté)
        TextField tfCal = creerChamp(String.valueOf(objectifCalories),        "Ex: 2150");
        TextField tfGlu = creerChamp(String.valueOf((int) objectifGluPct),    "Ex: 30");
        TextField tfLip = creerChamp(String.valueOf((int) objectifLipPct),    "Ex: 10");
        TextField tfPrt = creerChamp(String.valueOf((int) objectifProtPct),   "Ex: 60");
        TextField tfEau = creerChamp(String.valueOf(objectifEauMl),           "Ex: 2000");
        TextField tfCBr = creerChamp(String.valueOf(objCalBruleesParSemaine), "Ex: 1500");
        TextField tfEnt = creerChamp(String.valueOf(objEntrainements),        "Ex: 3");
        TextField tfMin = creerChamp(String.valueOf(objMinutes),              "Ex: 45");

        Label lblErreur = new Label("");
        lblErreur.setStyle(
                "-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblErreur.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(12); grid.setAlignment(Pos.CENTER);

        String[]    labels = {"Calories/jour :", "Glucides (%) :", "Lipides (%) :",
                "Protéines (%) :", "Eau (ml/jour) :", "Cal brûlées/sem :",
                "Entraînements/sem :", "Min/entraînement :"};
        TextField[] fields = {tfCal, tfGlu, tfLip, tfPrt, tfEau, tfCBr, tfEnt, tfMin};

        for (int i = 0; i < labels.length; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #424242;");
            lbl.setMinWidth(185);
            // add(Node, col, row) — TextField est bien javafx.scene.control.TextField ici
            grid.add(lbl,      0, i);
            grid.add(fields[i], 1, i);
        }

        Button btnSave    = creerBoutonPremium("✅  Enregistrer",
                "linear-gradient(to right, #6a1b9a, #d500f9)");
        Button btnAnnuler = creerBoutonPremium("Annuler", "#9ca3af");

        btnSave.setOnAction(ev -> {
            try {
                int    cal = Integer.parseInt(tfCal.getText().trim());
                double glu = Double.parseDouble(tfGlu.getText().trim().replace(",", "."));
                double lip = Double.parseDouble(tfLip.getText().trim().replace(",", "."));
                double prt = Double.parseDouble(tfPrt.getText().trim().replace(",", "."));
                int    eau = Integer.parseInt(tfEau.getText().trim());
                int    cbr = Integer.parseInt(tfCBr.getText().trim());
                int    ent = Integer.parseInt(tfEnt.getText().trim());
                int    min = Integer.parseInt(tfMin.getText().trim());

                if (cal <= 0 || eau <= 0) {
                    lblErreur.setText("\u274c Calories et eau doivent être supérieures à 0.");
                    return;
                }
                if (cbr < 0 || ent < 0 || min < 0 || glu < 0 || lip < 0 || prt < 0) {
                    lblErreur.setText("\u274c Aucune valeur ne peut être négative.");
                    return;
                }
                double somme = glu + lip + prt;
                if (Math.abs(somme - 100.0) > 0.5) {
                    lblErreur.setText("\u274c Glucides + Lipides + Protéines = "
                            + String.format("%.0f", somme)
                            + " %. Ils doivent totaliser 100 %.");
                    return;
                }
                objectifCalories        = cal;
                objectifGluPct          = glu;
                objectifLipPct          = lip;
                objectifProtPct         = prt;
                objectifEauMl           = eau;
                objCalBruleesParSemaine = cbr;
                objEntrainements        = ent;
                objMinutes              = min;
                sauvegarderPreferences();
                afficherObjectifs();
                actualiserCalories();
                fermerOverlay();

            } catch (NumberFormatException ex) {
                lblErreur.setText("\u274c Veuillez entrer des nombres entiers valides.");
            }
        });

        btnAnnuler.setOnAction(ev -> fermerOverlay());

        HBox btnBox = new HBox(15, btnAnnuler, btnSave);
        btnBox.setAlignment(Pos.CENTER);

        VBox dialog = new VBox(20);
        dialog.setAlignment(Pos.CENTER);
        dialog.setMaxWidth(530);
        dialog.setStyle(
                "-fx-background-color: white; -fx-background-radius: 24px; " +
                        "-fx-padding: 40px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 30, 0, 0, 12);");

        Label titre = new Label("\u270f\ufe0f  Modifier mes objectifs");
        titre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");
        Label sousTitre = new Label(
                "\uD83D\uDCA1 Glucides + Lipides + Protéines doivent totaliser 100 %.");
        sousTitre.setStyle("-fx-font-size: 12px; -fx-text-fill: #9e9e9e;");
        sousTitre.setWrapText(true);

        dialog.getChildren().addAll(titre, sousTitre, grid, lblErreur, btnBox);

        overlayObjectifs.getChildren().setAll(dialog);
        overlayObjectifs.setVisible(true);
        overlayObjectifs.setOnMouseClicked(ev -> {
            if (ev.getTarget() == overlayObjectifs) fermerOverlay();
        });

        FadeTransition ft = new FadeTransition(Duration.millis(250), dialog);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(250), dialog);
        st.setFromX(0.85); st.setFromY(0.85); st.setToX(1.0); st.setToY(1.0); st.play();
    }

    private void fermerOverlay() {
        overlayObjectifs.setVisible(false);
        overlayObjectifs.getChildren().clear();
    }

    /** Retourne un javafx.scene.control.TextField — type sans ambiguïté grâce à l'import unique */
    private TextField creerChamp(String valeur, String placeholder) {
        TextField tf = new TextField(valeur);
        tf.setPromptText(placeholder);
        tf.setPrefWidth(210);
        tf.setStyle(
                "-fx-background-color: #f3e5f5; -fx-border-color: #ce93d8; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-padding: 9; -fx-font-size: 14px;");
        return tf;
    }

    private Button creerBoutonPremium(String texte, String bgStyle) {
        Button btn = new Button(texte);
        btn.setStyle(
                "-fx-background-color: " + bgStyle + "; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 28; " +
                        "-fx-background-radius: 30px; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        btn.setOnMouseEntered(ev -> btn.setTranslateY(-2));
        btn.setOnMouseExited(ev -> btn.setTranslateY(0));
        return btn;
    }

    // ==========================================================================
    // OUTILS JSON
    // ==========================================================================
    private String extraireStringJSON(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            idx += key.length();
            StringBuilder sb = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '"' && json.charAt(idx - 1) != '\\') break;
                sb.append(c);
                idx++;
            }
            return sb.toString().replace("\\\"", "\"").replace("\\/", "/");
        } catch (Exception e) { return ""; }
    }

    private String nettoyerTexte(String t) {
        if (t == null || t.isBlank()) return "";
        return t.replace("\\u00e9", "\u00e9").replace("\\u00e8", "\u00e8")
                .replace("\\u00ea", "\u00ea").replace("\\u00e0", "\u00e0")
                .replace("\\u00e2", "\u00e2").replace("\\u00ee", "\u00ee")
                .replace("\\u00f4", "\u00f4").replace("\\u00fb", "\u00fb")
                .replace("\\u00e7", "\u00e7").replace("\\u2019", "\u2019")
                .replace("\\u0027", "'").replace("\\n", " ").replace("\\r", "").trim();
    }

    // ==========================================================================
    // NAVIGATION BACK-OFFICE (INCHANGÉE)
    // ==========================================================================
    @FXML
    void goToBackOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/root-layout.fxml"));
            Parent root = loader.load();
            controllers.RootLayoutController ctrl = loader.getController();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (ctrl != null) ctrl.setStage(stage);
            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==========================================================================
    // GÉNÉRATION BILAN PDF — ENRICHI AVEC PLUS DE STATISTIQUES
    // ==========================================================================
    @FXML
    void genererBilanPDF(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le Bilan de Santé");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fc.setInitialFileName("Bilan_Harmony_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");

        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fc.showSaveDialog(mainStage);
        if (file == null) return;

        try {
            ServiceActivite     sa = new ServiceActivite();
            ServiceConsommation sc = new ServiceConsommation();
            ServiceSommeil      ss = new ServiceSommeil();

            List<Activite>     activites = sa.afficherTout();
            List<Consommation> repas     = sc.afficherTout();
            List<Sommeil>      nuits     = ss.afficherTout();

            // ── CALCULS STATISTIQUES ────────────────────────────────────────
            int totalSportMin        = activites.stream().mapToInt(Activite::getDuree_minutes).sum();
            int totalCaloriesBrulees = activites.stream().mapToInt(Activite::getCalories_brulees).sum();
            int totalEauMl           = repas.stream().mapToInt(Consommation::getQuantite_eau_ml).sum();

            // Séances des 7 derniers jours
            LocalDate today = LocalDate.now();
            LocalDate il7j  = today.minusDays(7);
            long seances7j = activites.stream()
                    .filter(a -> a.getDate_activite() != null &&
                            !a.getDate_activite().toLocalDateTime().toLocalDate().isBefore(il7j))
                    .count();

            // Calories moyennes par séance
            double calMoyParSeance = activites.isEmpty() ? 0 :
                    (double) totalCaloriesBrulees / activites.size();

            // Sommeil : durée moyenne
            double sommeilMoyH = 0;
            if (!nuits.isEmpty()) {
                double totalMin = nuits.stream().mapToDouble(n -> {
                    long m = java.time.Duration.between(
                            n.getDate_coucher().toLocalDateTime(),
                            n.getDate_reveil().toLocalDateTime()).toMinutes();
                    return m / 60.0;
                }).sum();
                sommeilMoyH = totalMin / nuits.size();
            }

            // Nuits de bonne qualité (Excellent ou BON)
            long bonneNuits = nuits.stream()
                    .filter(n -> "EXCELLENT".equals(n.getQualite_sommeil()) || "BON".equals(n.getQualite_sommeil()))
                    .count();

            // Journées alimentaires distinctes
            long joursAvecRepas = repas.stream()
                    .filter(c -> c.getDate_consommation() != null)
                    .map(c -> c.getDate_consommation().toLocalDateTime().toLocalDate())
                    .distinct().count();

            // Taux de réussite calories (jours où objectif respecté)
            Map<LocalDate, Integer> calParJour = new HashMap<>();
            Map<Integer, Aliment> alimentMap = new HashMap<>();
            serviceAliment.afficherTout().forEach(a -> alimentMap.put(a.getId_aliment(), a));
            repas.forEach(c -> {
                if (c.getDate_consommation() == null) return;
                LocalDate d = c.getDate_consommation().toLocalDateTime().toLocalDate();
                Aliment a = alimentMap.get(c.getId_aliment());
                int cal = (a == null) ? 0 : (int) Math.round(
                        a.getCalories_pour_100g() * (double) c.getPoids_grammes() / 100.0);
                calParJour.merge(d, cal, Integer::sum);
            });
            long joursObjectifRespect = calParJour.values().stream()
                    .filter(cal -> cal <= objectifCalories).count();
            int tauxReussite = calParJour.isEmpty() ? 0 :
                    (int) Math.round(100.0 * joursObjectifRespect / calParJour.size());

            // Calories brûlées cette semaine
            LocalDate lundiSem = today.minusDays(today.getDayOfWeek().getValue() - 1);
            int calBruleesSemaine = activites.stream()
                    .filter(a -> a.getDate_activite() != null &&
                            !a.getDate_activite().toLocalDateTime().toLocalDate().isBefore(lundiSem))
                    .mapToInt(Activite::getCalories_brulees).sum();

            // ── DOCUMENT PDF ──────────────────────────────────────────────
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            BaseColor mauve    = new BaseColor(106, 27, 154);
            BaseColor vert     = new BaseColor(27, 94, 32);
            BaseColor bleu     = new BaseColor(21, 101, 192);
            BaseColor orange   = new BaseColor(230, 81, 0);
            BaseColor gris     = new BaseColor(80, 80, 80);
            Font fontTitre     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, mauve);
            Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, mauve);
            Font fontSection   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, bleu);
            Font fontTexte     = FontFactory.getFont(FontFactory.HELVETICA, 12, gris);
            Font fontHdr       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            Font fontVert      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, vert);
            Font fontOrange    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, orange);

            // ── PAGE 1 : EN-TÊTE + RÉSUMÉ GLOBAL ─────────────────────────
            Paragraph titre = new Paragraph("BILAN GLOBAL DE SANTÉ — HARMONY", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            doc.add(titre);

            Paragraph dateGen = new Paragraph(
                    "Généré le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, BaseColor.GRAY));
            dateGen.setAlignment(Element.ALIGN_CENTER);
            dateGen.setSpacingAfter(20f);
            doc.add(dateGen);

            // Tableau résumé global (4 colonnes)
            Paragraph tResume = new Paragraph("TABLEAU DE BORD GLOBAL", fontSousTitre);
            tResume.setSpacingBefore(10f); tResume.setSpacingAfter(10f);
            doc.add(tResume);

            PdfPTable tableResume = new PdfPTable(4);
            tableResume.setWidthPercentage(100); tableResume.setSpacingAfter(20f);
            String[] resumeLabels = {"Séances sport", "Calories brûlées", "Jours trackés", "Nuits enregistrées"};
            String[] resumeValues = {
                    activites.size() + " séances",
                    totalCaloriesBrulees + " kcal",
                    joursAvecRepas + " jours",
                    nuits.size() + " nuits"
            };
            BaseColor[] resumeColors = {mauve, new BaseColor(198, 40, 40), new BaseColor(21, 101, 192), new BaseColor(0, 131, 143)};
            for (int i = 0; i < 4; i++) {
                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(resumeColors[i]);
                cell.setPadding(12f); cell.setBorder(PdfPCell.NO_BORDER);
                Paragraph p = new Paragraph(resumeLabels[i] + "\n",
                        FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.WHITE));
                p.add(new Phrase(resumeValues[i],
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.WHITE)));
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p); tableResume.addCell(cell);
            }
            doc.add(tableResume);

            // ── SECTION SPORT ─────────────────────────────────────────────
            Paragraph tSport = new Paragraph("SYNTHÈSE SPORTIVE", fontSousTitre);
            tSport.setSpacingAfter(10f); doc.add(tSport);

            PdfPTable tableSport = pdfTable("Indicateur", "Performances", fontHdr);
            pdfRow(tableSport, "Séances réalisées (total)",     activites.size() + " séances",                              fontTexte, true);
            pdfRow(tableSport, "Séances cette semaine",          seances7j + " séances (7 derniers jours)",                  fontTexte, false);
            pdfRow(tableSport, "Temps total d'effort",           (totalSportMin/60)+"h "+(totalSportMin%60)+"m",             fontTexte, true);
            pdfRow(tableSport, "Temps moyen par séance",         activites.isEmpty() ? "N/A" :
                    (totalSportMin/activites.size()) + " min/séance",                                                         fontTexte, false);
            pdfRow(tableSport, "Calories brûlées (total)",       totalCaloriesBrulees + " kcal",                             fontTexte, true);
            pdfRow(tableSport, "Calories brûlées cette semaine", calBruleesSemaine + " kcal",                                fontTexte, false);
            pdfRow(tableSport, "Moyenne cal/séance",             String.format("%.0f kcal", calMoyParSeance),                fontTexte, true);
            pdfRow(tableSport, "Objectif entraînements/semaine", objEntrainements + " séances/semaine",                      fontTexte, false);
            doc.add(tableSport);

            // Graphique calories brûlées par séance — CORRIGÉ
            if (!activites.isEmpty()) {
                doc.add(new Paragraph("Évolution des calories brûlées par séance :", fontSection));
                doc.add(chartToPdfImage(creerBarChart(activites), "bar"));
            }

            // ── PAGE 2 : NUTRITION ────────────────────────────────────────
            doc.newPage();
            Paragraph tNutri = new Paragraph("NUTRITION & HYDRATATION", fontSousTitre);
            tNutri.setSpacingAfter(10f); doc.add(tNutri);

            PdfPTable tableNutri = pdfTable("Indicateur", "Valeur", fontHdr);
            pdfRow(tableNutri, "Jours avec repas enregistrés",  joursAvecRepas + " jours",                                  fontTexte, true);
            pdfRow(tableNutri, "Total entrées nutritionnelles",  repas.size() + " entrées",                                  fontTexte, false);
            pdfRow(tableNutri, "Volume d'eau bu (total)",        String.format("%.1f L", totalEauMl/1000.0),                 fontTexte, true);
            pdfRow(tableNutri, "Objectif calorique journalier",  objectifCalories + " kcal/jour",                            fontTexte, false);
            pdfRow(tableNutri, "Taux de réussite objectif cal.", tauxReussite + "% des jours dans l'objectif",               fontTexte, true);
            pdfRow(tableNutri, "Répartition macros cibles",
                    "G:" + (int)objectifGluPct + "% | L:" + (int)objectifLipPct + "% | P:" + (int)objectifProtPct + "%",    fontTexte, false);
            doc.add(tableNutri);

            // Graphique hydratation — CORRIGÉ
            if (!repas.isEmpty()) {
                doc.add(new Paragraph("\nSuivi de l'hydratation quotidienne :", fontSection));
                doc.add(chartToPdfImage(creerLineChartEau(repas), "line"));
            }

            // ── PAGE 3 : SOMMEIL ─────────────────────────────────────────
            doc.newPage();
            Paragraph tSommeil = new Paragraph("RÉCUPÉRATION & SOMMEIL", fontSousTitre);
            tSommeil.setSpacingAfter(10f); doc.add(tSommeil);

            PdfPTable tableSommeil = pdfTable("Indicateur", "Donnée", fontHdr);
            pdfRow(tableSommeil, "Nuits enregistrées",         nuits.size() + " nuits",                                      fontTexte, true);
            pdfRow(tableSommeil, "Durée moyenne de sommeil",   String.format("%.1f h/nuit", sommeilMoyH),                    fontTexte, false);
            pdfRow(tableSommeil, "Nuits de bonne qualité",     bonneNuits + " nuits (Excellent ou Bon)",                     fontTexte, true);
            pdfRow(tableSommeil, "Taux de bonne qualité",      nuits.isEmpty() ? "N/A" :
                    String.format("%.0f%%", 100.0 * bonneNuits / nuits.size()),                                               fontTexte, false);
            pdfRow(tableSommeil, "Objectif sommeil recommandé", "7 à 9 heures par nuit (OMS)",                               fontTexte, true);
            doc.add(tableSommeil);

            // Graphique sommeil — CORRIGÉ
            if (!nuits.isEmpty()) {
                doc.add(new Paragraph("\nÉvolution de la durée de sommeil :", fontSection));
                doc.add(chartToPdfImage(creerLineChartSommeil(nuits), "sommeil"));
            }

            // ── CONCLUSION ────────────────────────────────────────────────
            Paragraph concl = new Paragraph(
                    "\n\nBravo pour votre suivi ! Ce rapport Harmony résume votre parcours de santé.\n" +
                            "Continuez à tracker, progresser et prendre soin de vous chaque jour. 💪",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, mauve));
            concl.setAlignment(Element.ALIGN_CENTER);
            doc.add(concl);

            doc.close();
            afficherFenetreSucces(mainStage);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers Graphiques pour PDF (méthodes séparées pour clarté) ──

    private BarChart<String, Number> creerBarChart(List<Activite> activites) {
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        xA.setAnimated(false); yA.setAnimated(false);
        xA.setLabel("Séances"); yA.setLabel("Calories brûlées");
        BarChart<String, Number> chart = new BarChart<>(xA, yA);
        chart.setAnimated(false); chart.setTitle("Calories brûlées par séance");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        int i = 1;
        for (Activite a : activites)
            s.getData().add(new XYChart.Data<>("S" + i++, a.getCalories_brulees()));
        chart.getData().add(s);
        return chart;
    }

    private LineChart<String, Number> creerLineChartEau(List<Consommation> repas) {
        CategoryAxis xW = new CategoryAxis(); NumberAxis yW = new NumberAxis();
        xW.setAnimated(false); yW.setAnimated(false);
        xW.setLabel("Entrées"); yW.setLabel("Eau (ml)");
        LineChart<String, Number> chart = new LineChart<>(xW, yW);
        chart.setAnimated(false); chart.setTitle("Suivi hydratation (ml)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> sE = new XYChart.Series<>();
        int j = 1;
        for (Consommation c : repas)
            sE.getData().add(new XYChart.Data<>("R" + j++, c.getQuantite_eau_ml()));
        chart.getData().add(sE);
        return chart;
    }

    private LineChart<String, Number> creerLineChartSommeil(List<Sommeil> nuits) {
        CategoryAxis xS = new CategoryAxis(); NumberAxis yS = new NumberAxis();
        xS.setAnimated(false); yS.setAnimated(false);
        xS.setLabel("Nuits"); yS.setLabel("Heures de sommeil");
        LineChart<String, Number> chart = new LineChart<>(xS, yS);
        chart.setAnimated(false); chart.setTitle("Durée de sommeil par nuit");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> sS = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (Sommeil n : nuits) {
            String label = n.getDate_coucher().toLocalDateTime().format(fmt);
            double h = java.time.Duration.between(
                    n.getDate_coucher().toLocalDateTime(),
                    n.getDate_reveil().toLocalDateTime()).toMinutes() / 60.0;
            sS.getData().add(new XYChart.Data<>(label, h));
        }
        chart.getData().add(sS);
        return chart;
    }

    // ── Helpers PDF ──
    private PdfPTable pdfTable(String c1, String c2, Font fh) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(95); t.setSpacingBefore(10f); t.setSpacingAfter(15f);
        BaseColor bg = new BaseColor(106, 27, 154);
        PdfPCell cc1 = new PdfPCell(new Phrase(c1, fh));
        cc1.setBackgroundColor(bg); cc1.setPadding(12f); cc1.setBorder(PdfPCell.NO_BORDER);
        PdfPCell cc2 = new PdfPCell(new Phrase(c2, fh));
        cc2.setBackgroundColor(bg); cc2.setPadding(12f); cc2.setBorder(PdfPCell.NO_BORDER);
        t.addCell(cc1); t.addCell(cc2);
        return t;
    }

    private void pdfRow(PdfPTable t, String v1, String v2, Font f, boolean light) {
        BaseColor bg     = light ? new BaseColor(248, 240, 252) : BaseColor.WHITE;
        BaseColor border = new BaseColor(230, 230, 230);
        PdfPCell c1 = new PdfPCell(new Phrase(v1, f));
        c1.setBackgroundColor(bg); c1.setPadding(10f); c1.setBorderColor(border);
        c1.setBorderWidthTop(0); c1.setBorderWidthRight(0); c1.setBorderWidthLeft(0);
        PdfPCell c2 = new PdfPCell(new Phrase(v2, f));
        c2.setBackgroundColor(bg); c2.setPadding(10f); c2.setBorderColor(border);
        c2.setBorderWidthTop(0); c2.setBorderWidthRight(0); c2.setBorderWidthLeft(0);
        t.addCell(c1); t.addCell(c2);
    }

    private Image chartToPdfImage(Chart chart, String type) throws Exception {
        // CORRECTION : on force le rendu CSS dans une scène temporaire non affichée
        chart.setPrefSize(650, 320);
        chart.setMinSize(650, 320);
        chart.setMaxSize(650, 320);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: white;");

        // Crée une scène temporaire (non affichée) pour forcer le CSS
        Group group = new Group(chart);
        Scene offscreenScene = new Scene(group, 650, 320);

        // Applique les CSS et force le layout
        offscreenScene.getRoot().applyCss();
        offscreenScene.getRoot().layout();
        chart.applyCss();
        chart.layout();

        // Personnalisation visuelle des graphiques
        Node pb = chart.lookup(".chart-plot-background");
        if (pb != null) pb.setStyle("-fx-background-color: #fbf6fc;");

        if ("bar".equals(type)) {
            for (Node n : chart.lookupAll(".default-color0.chart-bar"))
                n.setStyle("-fx-bar-fill: #6a1b9a;");
        } else {
            Node ln = chart.lookup(".chart-series-line");
            if (ln != null) ln.setStyle("-fx-stroke: #6a1b9a; -fx-stroke-width: 3px;");
            for (Node sym : chart.lookupAll(".chart-line-symbol"))
                sym.setStyle("-fx-background-color: #d500f9, white;");
        }

        // Snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage fxImage = chart.snapshot(params, null);
        BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", baos);
        Image img = Image.getInstance(baos.toByteArray());
        img.setAlignment(Element.ALIGN_CENTER);
        img.scalePercent(80);
        img.setSpacingBefore(8f);
        img.setSpacingAfter(12f);
        return img;
    }

    private void afficherFenetreSucces(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #6a1b9a, #d500f9); " +
                        "-fx-background-radius: 25; -fx-padding: 40; " +
                        "-fx-border-color: white; -fx-border-width: 3; -fx-border-radius: 25;");

        Label icon  = new Label("\u2728 \uD83D\uDCC4 \u2728");
        icon.setStyle("-fx-font-size: 50px;");
        Label title = new Label("Génération Réussie !");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label msg   = new Label("Votre bilan a été exporté avec succès !");
        msg.setStyle("-fx-font-size: 15px; -fx-text-fill: white; -fx-text-alignment: center;");
        msg.setAlignment(Pos.CENTER);

        Button btn = new Button("Génial !");
        btn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #6a1b9a; -fx-font-weight: bold; " +
                        "-fx-font-size: 16px; -fx-background-radius: 30; " +
                        "-fx-padding: 10 40; -fx-cursor: hand;");
        btn.setOnAction(ev -> dialog.close());

        root.getChildren().addAll(icon, title, msg, btn);
        Scene scene = new Scene(root, 420, 280);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(500), root);
        st.setFromX(0.75); st.setFromY(0.75); st.setToX(1.0); st.setToY(1.0); st.play();

        dialog.showAndWait();
    }
}