package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Role;
import models.user;
import models.Session;
import services.serviceUser;
import services.SessionDAO;

import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.io.IOException;

public class LoginController {

    @FXML private TextField emailLogin;
    @FXML private PasswordField passwordLogin;
    @FXML private Label errorLabel;
    @FXML private Hyperlink linkToRegister;
    @FXML private Hyperlink linkForgotPassword; // ✅ NOUVEAU
    @FXML private CheckBox rememberMe;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        linkToRegister.setOnAction(e -> goToRegister());

        // ✅ NOUVEAU : handler "Mot de passe oublié ?"
        linkForgotPassword.setOnAction(e -> goToForgotPassword());

        Platform.runLater(this::checkRememberMe);
    }

    private void checkRememberMe() {
        String rememberedToken = loadRememberToken();
        if (rememberedToken != null) {
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
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailLogin.getText().trim();
        String pass = passwordLogin.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        user utilisateur = service.getByEmailAndPassword(email, pass);

        if (utilisateur != null) {
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
        } else {
            if (service.isEmailArchived(email)) {
                showArchivedError();
            } else {
                showError("Email ou mot de passe incorrect");
            }
        }
    }

    // ✅ NOUVEAU : navigation vers la page "Mot de passe oublié"
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

    private void showArchivedError() {
        errorLabel.setText("🚫 Ce compte a été désactivé. Contactez l'administrateur.");
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #FEE2E2; -fx-padding: 10 15; -fx-background-radius: 8;");
        errorLabel.setVisible(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #FEE2E2; -fx-padding: 10 15; -fx-background-radius: 8;");
        errorLabel.setVisible(true);
    }

    private void redirectAccordingToRole(user user) {
        try {
            String fxmlPath = (user.getType_utilisateur() == Role.ETUDIANT)
                    ? "/views/Accueil.fxml"
                    : "/views/DashboardAdmin.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            Stage stage = (Stage) emailLogin.getScene().getWindow();
            if (stage == null) {
                stage = (Stage) javafx.stage.Window.getWindows().get(0);
            }

            stage.setScene(new Scene(root));
            stage.setTitle(user.getType_utilisateur() == Role.ETUDIANT
                    ? "Accueil - Harmony"
                    : "Dashboard Admin - Harmony");
            stage.centerOnScreen();

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

    private void saveRememberToken(String token) {
        try (FileWriter writer = new FileWriter("remember.dat")) {
            writer.write(token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadRememberToken() {
        try (BufferedReader reader = new BufferedReader(new FileReader("remember.dat"))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void deleteRememberFile() {
        new File("remember.dat").delete();
    }
}
