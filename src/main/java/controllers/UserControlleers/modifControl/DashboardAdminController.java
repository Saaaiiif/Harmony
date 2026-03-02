package controllers.UserControlleers.modifControl;

import controllers.ForumControllers.BackOffice.ForumBackDashboardController;
import controllers.UserControlleers.AdminProfilePopupController;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.layout.HBox;
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
 * Controller racine de l'espace admin.
 *
 * Gère :
 *  - Sidebar collapsible (72 px ↔ 260 px, animation 180ms)
 *  - Navigation par chargement de contenu dans le StackPane contentArea (fade 250ms)
 *  - Avatar profil avec tooltip au survol et popup au double-clic
 *  - Session check + logout
 *  - Gestion Sport (GestionSport.fxml) et Gestion Nutrition (GestionNutrition.fxml)
 */
public class DashboardAdminController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox      sidebar;
    @FXML private StackPane contentArea;

    @FXML private Button    btnDashboard;
    @FXML private Button    btnGestionUsers;
    @FXML private Button    btnGestionSport;
    @FXML private Button    btnGestionNutrition;
    @FXML private Button    btnGestionMeditation;
    @FXML private Button    btnJournalEtudiants;
    @FXML private Button    btnRessources;
    @FXML private Button    btnGestionRessources;
    @FXML private Button    btnLogout;

    @FXML private StackPane profileZone;
    @FXML private HBox      profileRow;
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitial;
    @FXML private Label     profileNameLabel;

    @FXML private VBox      profileTooltip;
    @FXML private Label     tooltipNom;
    @FXML private Label     tooltipEmail;

    // ── Sidebar dimensions ─────────────────────────────────────────────────────
    private static final double SIDEBAR_EXPANDED  = 260.0;
    private static final double SIDEBAR_COLLAPSED =  72.0;
    private Timeline sidebarAnim;

    // ── Page courante ─────────────────────────────────────────────────────────
    private String currentPage = "dashboard";

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        if (!checkSession()) {
            switchToLogin();
            return;
        }

        // Sidebar : démarrer réduite, puis installer le comportement hover
        Platform.runLater(() -> {
            collapseSidebar(true);
            sidebar.setPrefWidth(SIDEBAR_COLLAPSED);
            sidebar.setMinWidth(SIDEBAR_COLLAPSED);
            sidebar.setMaxWidth(SIDEBAR_COLLAPSED);

            // Appliquer le background image sur le StackPane contentArea
            try {
                java.net.URL bgUrl = getClass().getResource("/views/UserViews/backgLIGHT.png");
                if (bgUrl != null) {
                    contentArea.setStyle(
                            "-fx-background-image: url('" + bgUrl.toExternalForm() + "');" +
                                    "-fx-background-size: cover;" +
                                    "-fx-background-position: center center;"
                    );
                }
            } catch (Exception ignored) {}
        });

        sidebar.setOnMouseEntered(e -> {
            collapseSidebar(false);
            animateSidebarTo(SIDEBAR_EXPANDED);
        });
        sidebar.setOnMouseExited(e -> {
            collapseSidebar(true);
            animateSidebarTo(SIDEBAR_COLLAPSED);
        });

        // Profil admin
        Platform.runLater(this::loadAdminProfile);

        // Charger le dashboard par défaut
        Platform.runLater(() -> loadContent("/views/UserViews/modif/DashboardContent.fxml", "dashboard"));
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
        return u != null && u.getType_utilisateur() == Role.ADMIN;
    }

    private void switchToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/UserViews/Login.fxml"));
            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        return (Stage) sidebar.getScene().getWindow();
    }

    // =========================================================================
    //  SIDEBAR — ANIMATION
    // =========================================================================

    private void animateSidebarTo(double targetWidth) {
        if (sidebarAnim != null) sidebarAnim.stop();
        sidebarAnim = new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                        new KeyValue(sidebar.minWidthProperty(),  targetWidth),
                        new KeyValue(sidebar.maxWidthProperty(),  targetWidth)
                )
        );
        sidebarAnim.play();
    }

    private void collapseSidebar(boolean collapsed) {
        if (collapsed) {
            if (!sidebar.getStyleClass().contains("sidebar-collapsed"))
                sidebar.getStyleClass().add("sidebar-collapsed");
        } else {
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }
    }

    // =========================================================================
    //  NAVIGATION — chargement de contenu dans le StackPane
    // =========================================================================

    @FXML
    void handleDashboard() {
        if ("dashboard".equals(currentPage)) return;
        loadContent("/views/UserViews/modif/DashboardContent.fxml", "dashboard");
    }

    @FXML
    void handleGestionUsers() {
        if ("gestionUsers".equals(currentPage)) return;
        loadContent("/views/UserViews/GestionUsersContent.fxml", "gestionUsers");
    }

    /**
     * Charge la page Gestion Sport dans le contentArea.
     */
    @FXML
    void handleGestionSport() {
        if ("gestionSport".equals(currentPage)) return;
        loadContent("/views/ActiviteViews/GestionSport.fxml", "gestionSport");
    }

    /**
     * Charge la page Gestion Nutrition dans le contentArea.
     */
    @FXML
    void handleGestionNutrition() {
        if ("gestionNutrition".equals(currentPage)) return;
        loadContent("/views/ActiviteViews/GestionNutrition.fxml", "gestionNutrition");
    }

    /** Harmonie — Gestion des sessions de méditation (admin) */
    @FXML
    void handleGestionMeditation() {
        if ("gestionMeditation".equals(currentPage)) return;
        loadContent("/views/HarmonieViews/AdminMeditation.fxml", "gestionMeditation");
    }

    /** Harmonie — Journal d'humeur des étudiants (admin) */
    @FXML
    void handleJournalEtudiants() {
        if ("journalEtudiants".equals(currentPage)) return;
        loadContent("/views/HarmonieViews/AdminJournal.fxml", "journalEtudiants");
    }

    /** Gestion Ressources — Backoffice admin (salles, événements, demandes, tâches) */
    @FXML
    void handleGestionRessources() {
        if ("gestionRessources".equals(currentPage)) return;
        loadContent("/views/RessourceViews/admin-backoffice.fxml", "gestionRessources");
    }

    /** Cours signalés — modération des cours reportés */
    @FXML
    void handleCoursesSignales() {
        if ("coursesSignales".equals(currentPage)) return;
        loadContent("/views/LibraryViews/gestion-ressources.fxml", "coursesSignales");
    }

    /**
     * Charge un FXML dans le StackPane contentArea avec un fade transition.
     * Met à jour le bouton actif dans la sidebar.
     */
    private void loadContent(String fxmlPath, String pageKey) {
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
            updateActiveButton(pageKey);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateActiveButton(String pageKey) {
        // Réinitialiser tous les boutons
        btnDashboard.getStyleClass().setAll("sidebar-item");
        btnGestionUsers.getStyleClass().setAll("sidebar-item");
        if (btnGestionSport       != null) btnGestionSport.getStyleClass().setAll("sidebar-item");
        if (btnGestionNutrition   != null) btnGestionNutrition.getStyleClass().setAll("sidebar-item");
        if (btnGestionMeditation  != null) btnGestionMeditation.getStyleClass().setAll("sidebar-item");
        if (btnJournalEtudiants   != null) btnJournalEtudiants.getStyleClass().setAll("sidebar-item");
        if (btnRessources         != null) btnRessources.getStyleClass().setAll("sidebar-item");
        if (btnGestionRessources  != null) btnGestionRessources.getStyleClass().setAll("sidebar-item");

        switch (pageKey) {
            case "dashboard"          -> btnDashboard.getStyleClass().setAll("sidebar-item", "sidebar-item-active");
            case "gestionUsers"       -> btnGestionUsers.getStyleClass().setAll("sidebar-item", "sidebar-item-active");
            case "gestionSport"       -> { if (btnGestionSport      != null) btnGestionSport.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
            case "gestionNutrition"   -> { if (btnGestionNutrition  != null) btnGestionNutrition.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
            case "gestionMeditation"  -> { if (btnGestionMeditation != null) btnGestionMeditation.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
            case "journalEtudiants"   -> { if (btnJournalEtudiants  != null) btnJournalEtudiants.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
            case "gestionRessources" -> { if (btnRessources        != null) btnRessources.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
            case "coursesSignales"   -> { if (btnGestionRessources != null) btnGestionRessources.getStyleClass().setAll("sidebar-item", "sidebar-item-active"); }
        }
    }

    // =========================================================================
    //  LOGOUT
    // =========================================================================

    @FXML
    void handleLogout() {
        Session session = Session.getInstance();
        SessionDAO dao  = new SessionDAO();
        if (session.getToken() != null) dao.deleteSession(session.getToken());
        session.clearSession();
        new File("remember.dat").delete();
        switchToLogin();
    }

    // =========================================================================
    //  FORUM
    // =========================================================================

    @FXML
    private void handleForum() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/ForumViews/ForumBackOffice/ForumBackDashboard.fxml")
            );
            Parent content = loader.load();

            ForumBackDashboardController ctrl = loader.getController();

            if (contentArea.getChildren().isEmpty()) {
                contentArea.getChildren().setAll(content);
            } else {
                FadeTransition fadeOut =
                        new FadeTransition(Duration.millis(180), contentArea.getChildren().get(0));
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(content);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), content);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }
            currentPage = "forum";
            updateActiveButton("forum");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  PROFIL — avatar
    // =========================================================================

    private void loadAdminProfile() {
        user admin = Session.getInstance().getUser();
        if (admin == null) return;

        // Tooltip
        String nomComplet = nvl(admin.getUser_prenom()) + " " + nvl(admin.getUser_nom());
        tooltipNom.setText(nomComplet.trim());
        tooltipEmail.setText(nvl(admin.getUser_email()));
        profileNameLabel.setText(!nvl(admin.getUser_prenom()).isEmpty()
                ? admin.getUser_prenom() : "Admin");

        // Avatar
        String imagePath = admin.getUser_image_path();
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
        // Fallback initiale
        String initial = (!nvl(admin.getUser_prenom()).isEmpty())
                ? admin.getUser_prenom().substring(0, 1).toUpperCase() : "A";
        avatarInitial.setText(initial);
        avatarInitial.setVisible(true);
        avatarImage.setVisible(false);
    }

    // =========================================================================
    //  PROFIL — survol (tooltip)
    // =========================================================================

    @FXML
    void handleProfileHover(MouseEvent event) {
        profileTooltip.setVisible(true);
        profileTooltip.setManaged(true);
        avatarContainer.setScaleX(1.10);
        avatarContainer.setScaleY(1.10);
    }

    @FXML
    void handleProfileHoverEnd(MouseEvent event) {
        profileTooltip.setVisible(false);
        profileTooltip.setManaged(false);
        avatarContainer.setScaleX(1.0);
        avatarContainer.setScaleY(1.0);
    }

    // =========================================================================
    //  PROFIL — double-clic → popup édition
    // =========================================================================

    @FXML
    void handleProfileClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            openProfilePopup();
        }
    }

    private void openProfilePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserViews/AdminProfilePopup.fxml"));
            Parent root = loader.load();

            AdminProfilePopupController ctrl = loader.getController();

            Stage popup = new Stage();
            popup.setTitle("Harmony — Modifier mon profil");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(getCurrentStage());
            popup.setResizable(false);

            ctrl.setup(Session.getInstance().getUser(), popup, this::loadAdminProfile);

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