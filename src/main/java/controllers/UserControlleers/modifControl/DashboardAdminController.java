package controllers.UserControlleers.modifControl;

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
 *  - Sidebar collapsible (80 px ↔ 260 px, animation 180ms, même logique que RootLayoutController)
 *  - Navigation par chargement de contenu dans le StackPane contentArea (fade 250ms)
 *  - Avatar profil avec tooltip au survol et popup au double-clic
 *  - Session check + logout
 */
public class DashboardAdminController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox      sidebar;
    @FXML private StackPane contentArea;

    @FXML private Button    btnDashboard;
    @FXML private Button    btnGestionUsers;
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

    // ── Sidebar dimensions (identiques à RootLayoutController) ────────────────
    private static final double SIDEBAR_EXPANDED  = 260.0;
    private static final double SIDEBAR_COLLAPSED =  72.0;
    private Timeline sidebarAnim;

    // ── Styles bouton actif / inactif ─────────────────────────────────────────
    private static final String STYLE_ACTIVE =
            "sidebar-item sidebar-item-active";
    private static final String STYLE_INACTIVE =
            "sidebar-item";

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

            // ✅ Appliquer le background image sur le StackPane contentArea
            // (robuste car utilise le classloader pour résoudre l'URL absolue)
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
    //  SIDEBAR — ANIMATION (même logique que RootLayoutController)
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
        // Réinitialiser
        btnDashboard.getStyleClass().setAll("sidebar-item");
        btnGestionUsers.getStyleClass().setAll("sidebar-item");

        switch (pageKey) {
            case "dashboard"    -> btnDashboard.getStyleClass().setAll("sidebar-item", "sidebar-item-active");
            case "gestionUsers" -> btnGestionUsers.getStyleClass().setAll("sidebar-item", "sidebar-item-active");
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
