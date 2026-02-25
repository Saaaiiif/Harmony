package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Role;
import models.Session;
import models.user;
import services.SessionDAO;

import java.io.File;
import java.io.IOException;

/**
 * Controller de la page d'accueil Étudiant (Accueil.fxml).
 *
 * CORRECTIONS :
 *  - fx:id root est maintenant le StackPane racine (pas BorderPane)
 *  - Tooltip profil géré depuis l'overlay StackPane (plus de clipping)
 *  - Bouton déconnexion avec même logique que côté admin
 */
public class AccueilController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    // root = le StackPane racine du FXML (pas le BorderPane)
    @FXML private StackPane contentArea;

    // Boutons navbar
    @FXML private Button btnActivites;
    @FXML private Button btnForum;
    @FXML private Button btnRessources;
    @FXML private Button btnEvents;
    @FXML private Button btnMentale;
    @FXML private Button btnNutrition;
    @FXML private Button btnLogout;

    // Zone profil
    @FXML private StackPane profileZone;
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitial;
    @FXML private Label     editBadge;

    // Tooltip profil (overlay dans le StackPane racine)
    @FXML private VBox  profileTooltip;
    @FXML private Label tooltipNom;
    @FXML private Label tooltipEmail;

    // ── Pages (chemins FXML des collègues — compléter lors de l'intégration) ──
    private static final String PAGE_ACTIVITES  = "/views/AccueilActivite.fxml";
    private static final String PAGE_FORUM      = null;  // ← à remplir
    private static final String PAGE_RESSOURCES = null;  // ← à remplir
    private static final String PAGE_EVENTS     = null;  // ← à remplir
    private static final String PAGE_MENTALE    = null;  // ← à remplir
    private static final String PAGE_NUTRITION  = null;  // ← à remplir

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

        Platform.runLater(() -> {
            loadUserProfile();
            loadContent(PAGE_ACTIVITES, "activites", btnActivites);
        });
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
            // Nettoyer la session
            Session session = Session.getInstance();
            SessionDAO dao  = new SessionDAO();
            if (session.getToken() != null) dao.deleteSession(session.getToken());
            session.clearSession();
            new java.io.File("remember.dat").delete();

            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
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
    //  NAVIGATION — boutons navbar
    // =========================================================================

    @FXML void handleActivites()  { if (!"activites".equals(currentPage))  loadContent(PAGE_ACTIVITES,  "activites",  btnActivites);  }
    @FXML void handleForum()      { if (!"forum".equals(currentPage))      loadContent(PAGE_FORUM,      "forum",      btnForum);      }
    @FXML void handleRessources() { if (!"ressources".equals(currentPage)) loadContent(PAGE_RESSOURCES, "ressources", btnRessources); }
    @FXML void handleEvents()     { if (!"events".equals(currentPage))     loadContent(PAGE_EVENTS,     "events",     btnEvents);     }
    @FXML void handleMentale()    { if (!"mentale".equals(currentPage))    loadContent(PAGE_MENTALE,    "mentale",    btnMentale);    }
    @FXML void handleNutrition()  { if (!"nutrition".equals(currentPage))  loadContent(PAGE_NUTRITION,  "nutrition",  btnNutrition);  }

    /**
     * Charge un FXML dans contentArea avec fade transition.
     * Si fxmlPath est null → page non encore intégrée, on active juste le bouton.
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
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void updateActiveButton(Button active) {
        Button[] allBtns = {btnActivites, btnForum, btnRessources, btnEvents, btnMentale, btnNutrition};
        for (Button b : allBtns) {
            b.getStyleClass().remove("accueil-nav-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("accueil-nav-btn-active")) {
            active.getStyleClass().add("accueil-nav-btn-active");
        }
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
        profileTooltip.setManaged(false); // managed=false → ne perturbe pas le layout
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
                    getClass().getResource("/views/EtudiantProfilePopup.fxml"));
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
