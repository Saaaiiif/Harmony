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
 * Gère :
 *  - Navbar horizontale avec 6 boutons de navigation (activation visuelle)
 *  - Zone contentArea pour charger les sous-pages des collègues (fade 220ms)
 *  - Bouton profil rond : survol → tooltip détails, double-clic → popup édition
 *  - Session check + fallback login si session invalide
 */
public class AccueilController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // Boutons navbar
    @FXML private Button btnActivites;
    @FXML private Button btnForum;
    @FXML private Button btnRessources;
    @FXML private Button btnEvents;
    @FXML private Button btnMentale;
    @FXML private Button btnNutrition;

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

    // ── Constantes des pages (à adapter selon les FXML de tes collègues) ──────
    // ⚠️ Remplace les chemins null par les vrais chemins FXML de tes collègues
    //    quand tu les auras intégrés.
    private static final String PAGE_ACTIVITES  = "/views/AccueilActivite.fxml";  // déjà présent
    private static final String PAGE_FORUM      = null;  // ← à remplir lors de l'intégration
    private static final String PAGE_RESSOURCES = null;  // ← à remplir lors de l'intégration
    private static final String PAGE_EVENTS     = null;  // ← à remplir lors de l'intégration
    private static final String PAGE_MENTALE    = null;  // ← à remplir lors de l'intégration
    private static final String PAGE_NUTRITION  = null;  // ← à remplir lors de l'intégration

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        if (!checkSession()) {
            Platform.runLater(this::switchToLogin);
            return;
        }

        // Charger le profil dans la navbar
        Platform.runLater(() -> {
            loadUserProfile();
            // Charger la page Activités par défaut
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
        return null;
    }

    // =========================================================================
    //  NAVIGATION — boutons navbar
    // =========================================================================

    @FXML
    void handleActivites() {
        if ("activites".equals(currentPage)) return;
        loadContent(PAGE_ACTIVITES, "activites", btnActivites);
    }

    @FXML
    void handleForum() {
        if ("forum".equals(currentPage)) return;
        loadContent(PAGE_FORUM, "forum", btnForum);
    }

    @FXML
    void handleRessources() {
        if ("ressources".equals(currentPage)) return;
        loadContent(PAGE_RESSOURCES, "ressources", btnRessources);
    }

    @FXML
    void handleEvents() {
        if ("events".equals(currentPage)) return;
        loadContent(PAGE_EVENTS, "events", btnEvents);
    }

    @FXML
    void handleMentale() {
        if ("mentale".equals(currentPage)) return;
        loadContent(PAGE_MENTALE, "mentale", btnMentale);
    }

    @FXML
    void handleNutrition() {
        if ("nutrition".equals(currentPage)) return;
        loadContent(PAGE_NUTRITION, "nutrition", btnNutrition);
    }

    /**
     * Charge un FXML dans la zone contentArea avec une transition fade.
     * Met à jour le bouton actif dans la navbar.
     *
     * @param fxmlPath  Chemin du FXML à charger (null = page en construction)
     * @param pageKey   Clé interne pour éviter les rechargements inutiles
     * @param activeBtn Bouton navbar qui doit être mis en état "actif"
     */
    private void loadContent(String fxmlPath, String pageKey, Button activeBtn) {
        // Page non encore intégrée
        if (fxmlPath == null) {
            System.out.println("⚠ Page '" + pageKey + "' en cours d'intégration...");
            updateActiveButton(activeBtn);
            currentPage = pageKey;
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

            currentPage = pageKey;
            updateActiveButton(activeBtn);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Retire la classe active de tous les boutons navbar,
     * puis l'applique au bouton sélectionné.
     */
    private void updateActiveButton(Button active) {
        Button[] allBtns = {btnActivites, btnForum, btnRessources, btnEvents, btnMentale, btnNutrition};
        for (Button b : allBtns) {
            b.getStyleClass().remove("accueil-nav-btn-active");
            if (!b.getStyleClass().contains("accueil-nav-btn"))
                b.getStyleClass().add("accueil-nav-btn");
        }
        if (active != null && !active.getStyleClass().contains("accueil-nav-btn-active")) {
            active.getStyleClass().add("accueil-nav-btn-active");
        }
    }

    // =========================================================================
    //  PROFIL — chargement de l'avatar depuis la session
    // =========================================================================

    /**
     * Charge le profil de l'étudiant connecté depuis la session
     * et met à jour l'avatar et le tooltip dans la navbar.
     */
    private void loadUserProfile() {
        user etudiant = Session.getInstance().getUser();
        if (etudiant == null) return;

        // Remplir le tooltip
        String nomComplet = nvl(etudiant.getUser_prenom()) + " " + nvl(etudiant.getUser_nom());
        tooltipNom.setText(nomComplet.trim().isEmpty() ? "Étudiant" : nomComplet.trim());
        tooltipEmail.setText(nvl(etudiant.getUser_email()));

        // Charger l'image ou afficher l'initiale
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
        // Fallback : afficher la première lettre du prénom
        String initial = (!nvl(etudiant.getUser_prenom()).isEmpty())
                ? etudiant.getUser_prenom().substring(0, 1).toUpperCase() : "E";
        avatarInitial.setText(initial);
        avatarInitial.setVisible(true);
        avatarImage.setVisible(false);
    }

    // =========================================================================
    //  PROFIL — interactions hover / double-clic
    // =========================================================================

    /**
     * Survol de la zone profil : affiche le tooltip et agrandit légèrement l'avatar.
     */
    @FXML
    void handleProfileHover(MouseEvent event) {
        profileTooltip.setVisible(true);
        profileTooltip.setManaged(true);
        editBadge.setVisible(true);
        avatarContainer.setScaleX(1.10);
        avatarContainer.setScaleY(1.10);
    }

    /**
     * Fin du survol : cache le tooltip et remet l'avatar à sa taille normale.
     */
    @FXML
    void handleProfileHoverEnd(MouseEvent event) {
        profileTooltip.setVisible(false);
        profileTooltip.setManaged(false);
        editBadge.setVisible(false);
        avatarContainer.setScaleX(1.0);
        avatarContainer.setScaleY(1.0);
    }

    /**
     * Double-clic sur la zone profil → ouvre le popup d'édition.
     */
    @FXML
    void handleProfileClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            openProfilePopup();
        }
    }

    /**
     * Ouvre le popup modal de modification du profil étudiant.
     * Même pattern que l'admin (AdminProfilePopupController).
     */
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

            // Le callback rafraîchit l'avatar dans la navbar après sauvegarde
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
