package controllers;

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
import models.Session;
import models.user;
import services.SessionDAO;

import java.io.File;
import java.io.IOException;

/**
 * Controller de la sidebar partagée entre toutes les pages admin.
 *
 * Le controller parent (DashboardAdminController, GestionUsersController, ...)
 * doit appeler dans son initialize() :
 *
 *   sidebarController.setActiveButton("dashboard");   // ou "gestionUsers"
 *   sidebarController.setNavigationHandler(event -> { ... }); // optionnel
 */
public class AdminSidebarController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Button    btnDashboard;
    @FXML private Button    btnGestionUsers;
    @FXML private Button    btnLogout;

    @FXML private StackPane profileZone;
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitial;
    @FXML private Label     profileNameLabel;

    @FXML private VBox      profileTooltip;
    @FXML private Label     tooltipNom;
    @FXML private Label     tooltipEmail;

    // ── Styles ────────────────────────────────────────────────────────────────
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 13; " +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; " +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 18;";

    private static final String STYLE_INACTIVE =
            "-fx-background-color: #374151; -fx-text-fill: white; -fx-font-size: 13; " +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; " +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 18;";

    // ── État ──────────────────────────────────────────────────────────────────
    private String currentPage = "";

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        Platform.runLater(this::loadAdminProfile);
    }

    /**
     * Charge les données de l'admin connecté et affiche son avatar.
     */
    private void loadAdminProfile() {
        user admin = Session.getInstance().getUser();
        if (admin == null) return;

        // ── Tooltip ──────────────────────────────────────────────────────────
        String nomComplet = (admin.getUser_prenom() != null ? admin.getUser_prenom() : "")
                + " "
                + (admin.getUser_nom() != null ? admin.getUser_nom() : "");
        tooltipNom.setText(nomComplet.trim());
        tooltipEmail.setText(admin.getUser_email() != null ? admin.getUser_email() : "");
        profileNameLabel.setText(admin.getUser_prenom() != null ? admin.getUser_prenom() : "Admin");

        // ── Avatar ───────────────────────────────────────────────────────────
        String imagePath = admin.getUser_image_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 54, 54, false, true);
                    avatarImage.setImage(img);
                    avatarImage.setVisible(true);
                    avatarInitial.setVisible(false);
                    return;
                } catch (Exception ignored) {}
            }
        }

        // Pas d'image : afficher la première lettre du prénom
        String initial = (admin.getUser_prenom() != null && !admin.getUser_prenom().isEmpty())
                ? admin.getUser_prenom().substring(0, 1).toUpperCase()
                : "A";
        avatarInitial.setText(initial);
        avatarInitial.setVisible(true);
        avatarImage.setVisible(false);
    }

    // =========================================================================
    //  BOUTON ACTIF
    // =========================================================================

    /**
     * Met en surbrillance le bouton de la page courante.
     * Appeler depuis le controller parent.
     * @param page "dashboard" | "gestionUsers"
     */
    public void setActiveButton(String page) {
        this.currentPage = page;
        btnDashboard.setStyle(STYLE_INACTIVE);
        btnGestionUsers.setStyle(STYLE_INACTIVE);

        switch (page) {
            case "dashboard"    -> btnDashboard.setStyle(STYLE_ACTIVE);
            case "gestionUsers" -> btnGestionUsers.setStyle(STYLE_ACTIVE);
        }
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    @FXML
    void handleDashboard() {
        if ("dashboard".equals(currentPage)) return;
        navigateTo("/views/DashboardAdmin.fxml", "Harmony - Dashboard Admin",
                btnDashboard);
    }

    @FXML
    void handleGestionUsers() {
        if ("gestionUsers".equals(currentPage)) return;
        navigateTo("/views/GestionUsers.fxml", "Harmony - Gestion des Utilisateurs",
                btnGestionUsers);
    }

    @FXML
    void handleLogout() {
        Session session = Session.getInstance();
        SessionDAO dao = new SessionDAO();
        if (session.getToken() != null) dao.deleteSession(session.getToken());
        session.clearSession();
        new File("remember.dat").delete();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, String title, Button sourceBtn) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        return (Stage) btnDashboard.getScene().getWindow();
    }

    // =========================================================================
    //  PROFIL — SURVOL (TOOLTIP PERSONNALISÉ)
    // =========================================================================

    @FXML
    void handleProfileHover(MouseEvent event) {
        profileTooltip.setVisible(true);
        profileTooltip.setManaged(true);

        // Légère animation d'agrandissement de l'avatar
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
    //  PROFIL — DOUBLE-CLIC (POPUP D'ÉDITION)
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
                    getClass().getResource("/views/AdminProfilePopup.fxml"));
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
}
