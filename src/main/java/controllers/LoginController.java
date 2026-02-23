package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Role;
import models.user;
import models.Session;
import services.serviceUser;
import services.SessionDAO;
import utils.CaptchaService;

import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

public class LoginController {

    // ─── Champs existants ────────────────────────────────────────────────────
    @FXML private TextField     emailLogin;
    @FXML private PasswordField passwordLogin;
    @FXML private Label         errorLabel;
    @FXML private Hyperlink     linkToRegister;
    @FXML private Hyperlink     linkForgotPassword;
    @FXML private CheckBox      rememberMe;

    // ─── CAPTCHA ─────────────────────────────────────────────────────────────
    @FXML private Pane      captchaPane;
    @FXML private TextField captchaInput;
    @FXML private Label     captchaErrorLabel;
    @FXML private Button    btnRefreshCaptcha;

    // ─── Services ────────────────────────────────────────────────────────────
    private final serviceUser    service        = new serviceUser();
    private final CaptchaService captchaService = new CaptchaService();

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        captchaErrorLabel.setVisible(false);

        linkToRegister.setOnAction(e -> goToRegister());
        linkForgotPassword.setOnAction(e -> goToForgotPassword());

        Platform.runLater(() -> {
            renderCaptcha();
            checkRememberMe();
        });
    }

    // =========================================================================
    //  CAPTCHA
    // =========================================================================

    private void renderCaptcha() {
        Canvas canvas = captchaService.generateCaptchaCanvas();
        captchaPane.getChildren().setAll(canvas);
        captchaInput.clear();
        captchaErrorLabel.setVisible(false);
    }

    @FXML
    void handleRefreshCaptcha(ActionEvent event) {
        renderCaptcha();
        javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                javafx.util.Duration.millis(400), btnRefreshCaptcha);
        rt.setByAngle(360);
        rt.play();
    }

    // =========================================================================
    //  CONNEXION
    // =========================================================================

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailLogin.getText().trim();
        String pass  = passwordLogin.getText();

        // 1. Champs vides
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // 2. Validation CAPTCHA
        String captchaSaisie = captchaInput.getText().trim();
        if (captchaSaisie.isEmpty()) {
            showCaptchaError("Veuillez compléter la vérification de sécurité.");
            captchaInput.requestFocus();
            return;
        }
        if (!captchaService.validate(captchaSaisie)) {
            showCaptchaError("Code de vérification incorrect. Nouveau code généré.");
            renderCaptcha();
            captchaInput.requestFocus();
            return;
        }
        captchaErrorLabel.setVisible(false);

        // 3. Authentification
        user utilisateur = service.getByEmailAndPassword(email, pass);

        if (utilisateur == null) {
            renderCaptcha();
            if (service.isEmailArchived(email)) showArchivedError();
            else showError("Email ou mot de passe incorrect.");
            return;
        }

        // 4. ✅ Vérification biométrique (si un visage a été enregistré)
        if (utilisateur.getFace_image_path() != null
                && !utilisateur.getFace_image_path().isEmpty()) {

            boolean faceOk = openFaceVerificationPopup(utilisateur.getFace_image_path());

            if (!faceOk) {
                renderCaptcha();
                showError("❌ Vérification du visage échouée. Réessayez.");
                return;
            }
        }

        // 5. Créer la session et rediriger
        createSessionAndRedirect(utilisateur);
    }

    // =========================================================================
    //  POPUP DE VÉRIFICATION BIOMÉTRIQUE
    // =========================================================================

    /**
     * Ouvre le popup de vérification biométrique de manière modale.
     *
     * @param storedFacePath Chemin du visage enregistré à l'inscription
     * @return true si le visage a été reconnu, false sinon
     */
    private boolean openFaceVerificationPopup(String storedFacePath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/FaceVerification.fxml"));
            Parent root = loader.load();

            FaceVerificationController ctrl = loader.getController();

            Stage popupStage = new Stage();
            popupStage.setTitle("Harmony — Vérification biométrique");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(emailLogin.getScene().getWindow());
            popupStage.setResizable(false);

            // ✅ Setup AVANT d'afficher la scène
            ctrl.setup(storedFacePath, popupStage);

            popupStage.setScene(new Scene(root));
            popupStage.showAndWait(); // Bloque jusqu'à fermeture du popup

            return ctrl.isVerified();

        } catch (Exception e) {
            e.printStackTrace();
            // En cas d'erreur de chargement du popup, on laisse passer (dégradé)
            return true;
        }
    }

    // =========================================================================
    //  SESSION + REDIRECTION
    // =========================================================================

    private void createSessionAndRedirect(user utilisateur) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        SessionDAO sessionDAO = new SessionDAO();
        sessionDAO.deleteSessionsByUser(utilisateur.getUser_id());
        sessionDAO.createSession(utilisateur.getUser_id(), token, expiresAt);
        Session.getInstance().startSession(utilisateur, token, expiresAt);

        if (rememberMe.isSelected()) saveRememberToken(token);

        redirectAccordingToRole(utilisateur);
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    private void goToForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ForgotPassword.fxml"));
            Stage stage = (Stage) linkForgotPassword.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Mot de passe oublié");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Inscription.fxml"));
            Stage stage = (Stage) linkToRegister.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Inscription");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void redirectAccordingToRole(user user) {
        try {
            String fxmlPath = (user.getType_utilisateur() == Role.ETUDIANT)
                    ? "/views/Accueil.fxml"
                    : "/views/DashboardAdmin.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage  = (Stage) emailLogin.getScene().getWindow();
            if (stage == null) stage = (Stage) javafx.stage.Window.getWindows().get(0);

            stage.setScene(new Scene(root));
            stage.setTitle(user.getType_utilisateur() == Role.ETUDIANT
                    ? "Accueil - Harmony"
                    : "Dashboard Admin - Harmony");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  REMEMBER-ME
    // =========================================================================

    private void checkRememberMe() {
        String rememberedToken = loadRememberToken();
        if (rememberedToken == null) return;

        SessionDAO sessionDAO = new SessionDAO();
        if (sessionDAO.isTokenValid(rememberedToken)) {
            Optional<user> optUser = sessionDAO.getUserByToken(rememberedToken);
            optUser.ifPresent(u -> {
                if (u.isIs_active()) {
                    Session.getInstance().startSession(u, rememberedToken, LocalDateTime.now().plusHours(2));
                    redirectAccordingToRole(u);
                } else {
                    sessionDAO.deleteSession(rememberedToken);
                    deleteRememberFile();
                    showArchivedError();
                }
            });
        } else {
            deleteRememberFile();
        }
    }

    private void saveRememberToken(String token) {
        try (FileWriter writer = new FileWriter("remember.dat")) { writer.write(token); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private String loadRememberToken() {
        try (BufferedReader reader = new BufferedReader(new FileReader("remember.dat"))) {
            return reader.readLine();
        } catch (IOException e) { return null; }
    }

    private void deleteRememberFile() { new File("remember.dat").delete(); }

    // =========================================================================
    //  MESSAGES D'ERREUR
    // =========================================================================

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #FEE2E2; -fx-padding: 10 15; -fx-background-radius: 8;");
        errorLabel.setVisible(true);
    }

    private void showArchivedError() {
        errorLabel.setText("🚫 Ce compte a été désactivé. Contactez l'administrateur.");
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #FEE2E2; -fx-padding: 10 15; -fx-background-radius: 8;");
        errorLabel.setVisible(true);
    }

    private void showCaptchaError(String message) {
        captchaErrorLabel.setText("⚠  " + message);
        captchaErrorLabel.setVisible(true);
        shakeNode(captchaInput);
    }

    private void shakeNode(javafx.scene.Node node) {
        javafx.animation.TranslateTransition shake =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }
}

