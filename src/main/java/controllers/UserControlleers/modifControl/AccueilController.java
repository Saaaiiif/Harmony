package controllers.UserControlleers.modifControl;

import controllers.UserControlleers.EtudiantProfilePopupController;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.UserModels.Role;
import models.UserModels.Session;
import models.UserModels.user;
import services.UserServices.SessionDAO;

import java.io.File;
import java.io.IOException;

/**
 * Controller de la page d'accueil Étudiant (Accueil.fxml).
 *
 * CORRECTIONS :
 *  - btnActivites remplacé par menuActivites (MenuButton) — correspond au FXML
 *  - Le MenuButton "Activités" contient 4 items : Accueil, Aliments, Exercices, Sommeil
 *  - Chaque item charge la sous-page correspondante dans contentArea via loadActivityPage()
 *  - Injection de AccueilController dans tous les contrôleurs d'activité
 */
public class AccueilController {

    // ── Chemins FXML sous-pages activités ────────────────────────────────────
    private static final String PAGE_ACTIVITES  = "/views/ActiviteViews/AccueilActivite.fxml";
    private static final String PAGE_ALIMENTS   = "/views/ActiviteViews/JournalAlimentaire.fxml";
    private static final String PAGE_EXERCICES  = "/views/ActiviteViews/JournalExercices.fxml";
    private static final String PAGE_SOMMEIL    = "/views/ActiviteViews/JournalSommeil.fxml";
    private static final String PAGE_FORUM      = "/views/ForumViews/ForumHome.fxml";
    private static final String PAGE_RESSOURCES = null;  // à remplir lors de l'intégration
    private static final String PAGE_EVENTS     = null;  // à remplir lors de l'intégration
    private static final String PAGE_MENTALE    = null;  // à remplir lors de l'intégration
    private static final String PAGE_NUTRITION  = null;  // à remplir lors de l'intégration
    // Harmonie
    private static final String PAGE_MEDITATION        = "/views/HarmonieViews/EtudiantMeditation.fxml";
    private static final String PAGE_JOURNAL           = "/views/HarmonieViews/EtudiantJournal.fxml";
    private static final String PAGE_MEDITATION_DETAIL = "/views/HarmonieViews/EtudiantMeditationDetail.fxml";

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // MODIFIÉ : MenuButton au lieu de Button pour le bouton Activités
    @FXML private MenuButton menuActivites;

    // Les 5 autres boutons de navigation (inchangés)
    @FXML private Button btnForum;
    @FXML private Button btnRessources;
    @FXML private Button btnEvents;
    @FXML private Button btnMentale;
    @FXML private Button btnNutrition;
    @FXML private Button btnMeditation;
    @FXML private Button btnJournal;
    @FXML private Button btnLogout;

    // Zone profil
    @FXML private StackPane profileZone;
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitial;
    @FXML private Label     editBadge;

    // Tooltip profil
    @FXML private VBox  profileTooltip;
    @FXML private Label tooltipNom;
    @FXML private Label tooltipEmail;

    // ── Page courante ─────────────────────────────────────────────────────────
    private String currentPage = "";

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        if (!checkSession()) {
            Platform.runLater(this::switchToLogin);
            return;
        }

        // Construire les items du MenuButton Activités
        setupActivityMenuButton();

        Platform.runLater(() -> {
            loadUserProfile();
            loadContent(PAGE_ACTIVITES, "activites", menuActivites);
        });
    }

    /**
     * Crée les 4 MenuItems du bouton déroulant "Activités" et leur associe une action.
     */
    private void setupActivityMenuButton() {
        MenuItem itemAccueil   = new MenuItem("🏠  Accueil Activités");
        MenuItem itemAliments  = new MenuItem("🍴  Aliments");
        MenuItem itemExercices = new MenuItem("🏋  Exercices");
        MenuItem itemSommeil   = new MenuItem("😴  Sommeil");

        itemAccueil.setOnAction(e   -> loadActivityPage(PAGE_ACTIVITES));
        itemAliments.setOnAction(e  -> loadActivityPage(PAGE_ALIMENTS));
        itemExercices.setOnAction(e -> loadActivityPage(PAGE_EXERCICES));
        itemSommeil.setOnAction(e   -> loadActivityPage(PAGE_SOMMEIL));

        menuActivites.getItems().setAll(itemAccueil, itemAliments, itemExercices, itemSommeil);
    }

    // =========================================================================
    //  MÉTHODES PUBLIQUES (utilisées par les contrôleurs enfants)
    // =========================================================================

    /**
     * Charge une sous-page d'activité dans contentArea.
     * Appelée par AccueilActiviteController et ses sous-contrôleurs.
     */
    public void loadActivityPage(String fxmlPath) {
        updateActiveMenuButton();
        currentPage = "activites";

        if (fxmlPath == null || fxmlPath.isEmpty()) {
            System.err.println("❌ Chemin FXML invalide");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object ctrl = loader.getController();
            injecterAccueilController(ctrl);
            afficherContenu(newContent);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page activité : " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Charge n'importe quel contenu dans contentArea (utilisé par ForumHomeController, etc.)
     */
    public void setContent(Parent content) {
        afficherContenu(content);
    }

    /**
     * Retourne à la page d'accueil activités (utilisé par ForumHomeController).
     */
    public void goHome() {
        loadActivityPage(PAGE_ACTIVITES);
    }

    // =========================================================================
    //  SESSION
    // =========================================================================

    private boolean checkSession() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn()) return false;
        SessionDAO dao = new SessionDAO();
        if (!dao.isTokenValid(session.getToken())) return false;
        user u = session.getUser();
        return u != null && u.getType_utilisateur() == Role.ETUDIANT;
    }

    private void switchToLogin() {
        try {
            Session session = Session.getInstance();
            SessionDAO dao  = new SessionDAO();
            if (session.getToken() != null) dao.deleteSession(session.getToken());
            session.clearSession();
            new File("remember.dat").delete();

            Parent root = FXMLLoader.load(getClass().getResource("/views/UserViews/Login.fxml"));
            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle("Harmony - Connexion");
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        if (contentArea != null && contentArea.getScene() != null)
            return (Stage) contentArea.getScene().getWindow();
        if (profileZone != null && profileZone.getScene() != null)
            return (Stage) profileZone.getScene().getWindow();
        return null;
    }

    // =========================================================================
    //  DÉCONNEXION
    // =========================================================================

    @FXML
    void handleLogout() {
        switchToLogin();
    }

    // =========================================================================
    //  NAVIGATION — boutons navbar (Forum, Ressources, etc.)
    // =========================================================================

    @FXML void handleForum()      { if (!"forum".equals(currentPage))      loadContent(PAGE_FORUM,      "forum",      btnForum);      }
    @FXML void handleRessources() { if (!"ressources".equals(currentPage)) loadContent(PAGE_RESSOURCES, "ressources", btnRessources); }
    @FXML void handleEvents()     { if (!"events".equals(currentPage))     loadContent(PAGE_EVENTS,     "events",     btnEvents);     }
    @FXML void handleMentale()    { if (!"mentale".equals(currentPage))    loadContent(PAGE_MENTALE,    "mentale",    btnMentale);    }
    @FXML void handleNutrition()  { if (!"nutrition".equals(currentPage))  loadContent(PAGE_NUTRITION,  "nutrition",  btnNutrition);  }
    @FXML void handleMeditation() { if (!"meditation".equals(currentPage)) loadContent(PAGE_MEDITATION, "meditation", btnMeditation); }
    @FXML void handleJournal()    { if (!"journal".equals(currentPage))    loadContent(PAGE_JOURNAL,    "journal",    btnJournal);    }

    /**
     * Charge un FXML dans contentArea (version avec activation d'un bouton Button).
     */
    private void loadContent(String fxmlPath, String pageKey, Button activeBtn) {
        updateActiveButton(activeBtn);
        currentPage = pageKey;

        if (fxmlPath == null) {
            System.out.println("⚠ Page '" + pageKey + "' en cours d'intégration...");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object ctrl = loader.getController();
            injecterAccueilController(ctrl);
            afficherContenu(newContent);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Charge un FXML dans contentArea (version avec activation du MenuButton Activités).
     */
    private void loadContent(String fxmlPath, String pageKey, MenuButton menuBtn) {
        updateActiveMenuButton();
        currentPage = pageKey;

        if (fxmlPath == null) {
            System.out.println("⚠ Page '" + pageKey + "' en cours d'intégration...");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object ctrl = loader.getController();
            injecterAccueilController(ctrl);
            afficherContenu(newContent);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  INJECTION DU CONTRÔLEUR DANS LES ENFANTS
    // =========================================================================

    private void injecterAccueilController(Object ctrl) {
        if (ctrl instanceof controllers.ForumControllers.ForumHomeController) {
            ((controllers.ForumControllers.ForumHomeController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.ActiviteControllers.AccueilActiviteController) {
            ((controllers.ActiviteControllers.AccueilActiviteController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.ActiviteControllers.JournalAlimentaireController) {
            ((controllers.ActiviteControllers.JournalAlimentaireController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.ActiviteControllers.JournalExercicesController) {
            ((controllers.ActiviteControllers.JournalExercicesController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.ActiviteControllers.JournalSommeilController) {
            ((controllers.ActiviteControllers.JournalSommeilController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.ActiviteControllers.AjouterAlimentFrontController) {
            ((controllers.ActiviteControllers.AjouterAlimentFrontController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.MeditationControllers.EtudiantMeditationController) {
            ((controllers.MeditationControllers.EtudiantMeditationController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.MeditationControllers.EtudiantMeditationDetailController) {
            ((controllers.MeditationControllers.EtudiantMeditationDetailController) ctrl).setAccueilController(this);
        } else if (ctrl instanceof controllers.MeditationControllers.EtudiantJournalController) {
            ((controllers.MeditationControllers.EtudiantJournalController) ctrl).setAccueilController(this);
        }
    }

    // =========================================================================
    //  AFFICHAGE DU CONTENU (fade transition)
    // =========================================================================

    private void afficherContenu(Parent newContent) {
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().add(newContent);
        } else {
            Parent current = (Parent) contentArea.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), current);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().setAll(newContent);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), newContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    // =========================================================================
    //  GESTION BOUTONS ACTIFS
    // =========================================================================

    private void updateActiveButton(Button active) {
        // Désactiver tous les boutons standards
        Button[] allBtns = {btnForum, btnRessources, btnEvents, btnMentale, btnNutrition, btnMeditation, btnJournal};
        for (Button b : allBtns) {
            if (b != null) b.getStyleClass().remove("accueil-nav-btn-active");
        }
        // Désactiver le MenuButton
        if (menuActivites != null) menuActivites.getStyleClass().remove("accueil-nav-btn-active");

        if (active != null && !active.getStyleClass().contains("accueil-nav-btn-active")) {
            active.getStyleClass().add("accueil-nav-btn-active");
        }
    }

    private void updateActiveMenuButton() {
        // Désactiver tous les boutons standards
        Button[] allBtns = {btnForum, btnRessources, btnEvents, btnMentale, btnNutrition, btnMeditation, btnJournal};
        for (Button b : allBtns) {
            if (b != null) b.getStyleClass().remove("accueil-nav-btn-active");
        }
        // Activer le MenuButton Activités
        if (menuActivites != null && !menuActivites.getStyleClass().contains("accueil-nav-btn-active")) {
            menuActivites.getStyleClass().add("accueil-nav-btn-active");
        }
    }

    // =========================================================================
    //  HARMONIE — navigation publique (utilisée par les contrôleurs enfants)
    // =========================================================================

    /**
     * Charge la vue détail d'une session de méditation.
     * Appelé par EtudiantMeditationController.viewDetails().
     */
    public void showMeditationDetail(int sessionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PAGE_MEDITATION_DETAIL));
            Parent newContent = loader.load();
            controllers.MeditationControllers.EtudiantMeditationDetailController ctrl = loader.getController();
            ctrl.setAccueilController(this);
            ctrl.setSessionId(sessionId);
            afficherContenu(newContent);
            currentPage = "meditationDetail";
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement détail méditation");
            e.printStackTrace();
        }
    }

    /**
     * Recharge la liste des sessions de méditation.
     * Appelé par EtudiantMeditationDetailController.goBack().
     */
    public void loadMeditationPage() {
        loadContent(PAGE_MEDITATION, "meditation", btnMeditation);
    }

    // =========================================================================
    //  PROFIL — chargement avatar depuis la session
    // =========================================================================

    private void loadUserProfile() {
        user etudiant = Session.getInstance().getUser();
        if (etudiant == null) return;

        String nomComplet = nvl(etudiant.getUser_prenom()) + " " + nvl(etudiant.getUser_nom());
        tooltipNom.setText(nomComplet.trim().isEmpty() ? "Étudiant" : nomComplet.trim());
        tooltipEmail.setText(nvl(etudiant.getUser_email()));

        String imagePath = etudiant.getUser_image_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            File f = new File(imagePath);
            if (f.exists()) {
                try {
                    Image img = new Image(f.toURI().toString(), 44, 44, false, true);
                    avatarImage.setImage(img);
                    avatarImage.setVisible(true);
                    avatarInitial.setVisible(false);
                    return;
                } catch (Exception ignored) {}
            }
        }
        String initial = (!nvl(etudiant.getUser_prenom()).isEmpty())
                ? etudiant.getUser_prenom().substring(0, 1).toUpperCase() : "E";
        avatarInitial.setText(initial);
        avatarInitial.setVisible(true);
        avatarImage.setVisible(false);
    }

    // =========================================================================
    //  PROFIL — hover / double-clic
    // =========================================================================

    @FXML
    void handleProfileHover(MouseEvent event) {
        profileTooltip.setVisible(true);
        profileTooltip.setManaged(false);
        editBadge.setVisible(true);
        avatarContainer.setScaleX(1.10);
        avatarContainer.setScaleY(1.10);
    }

    @FXML
    void handleProfileHoverEnd(MouseEvent event) {
        profileTooltip.setVisible(false);
        editBadge.setVisible(false);
        avatarContainer.setScaleX(1.0);
        avatarContainer.setScaleY(1.0);
    }

    @FXML
    void handleProfileClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            openProfilePopup();
        }
    }

    private void openProfilePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/EtudiantProfilePopup.fxml"));
            Parent root = loader.load();

            EtudiantProfilePopupController ctrl = loader.getController();

            Stage popup = new Stage();
            popup.setTitle("Harmony — Modifier mon profil");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(getCurrentStage());
            popup.setResizable(false);

            ctrl.setup(Session.getInstance().getUser(), popup, this::loadUserProfile);

            popup.setScene(new Scene(root));
            popup.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  UTILITAIRE
    // =========================================================================

    private String nvl(String s) { return s != null ? s : ""; }
}