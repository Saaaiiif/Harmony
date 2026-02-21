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
    @FXML private CheckBox rememberMe;

    private final serviceUser service = new serviceUser();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        linkToRegister.setOnAction(e -> goToRegister());

        // Remember-Me : on attend que la scène soit complètement chargée
        Platform.runLater(this::checkRememberMe);
    }

    private void checkRememberMe() {
        String rememberedToken = loadRememberToken();
        if (rememberedToken != null) {
            SessionDAO sessionDAO = new SessionDAO();
            if (sessionDAO.isTokenValid(rememberedToken)) {
                Optional<user> optUser = sessionDAO.getUserByToken(rememberedToken);
                optUser.ifPresent(user -> {
                    Session.getInstance().startSession(user, rememberedToken, LocalDateTime.now().plusHours(2));
                    redirectAccordingToRole(user);
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
            errorLabel.setText("Veuillez remplir tous les champs");
            errorLabel.setVisible(true);
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

            if (rememberMe.isSelected()) {
                saveRememberToken(token);
            }

            redirectAccordingToRole(utilisateur);
        } else {
            errorLabel.setText("Email ou mot de passe incorrect");
            errorLabel.setVisible(true);
        }
    }

    private void redirectAccordingToRole(user user) {
        try {
            String fxmlPath = (user.getType_utilisateur() == Role.ETUDIANT)
                    ? "/views/Accueil.fxml"
                    : "/views/DashboardAdmin.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            // Récupération sécurisée de la Stage (fonctionne même pendant initialize)
            Stage stage = (Stage) emailLogin.getScene().getWindow();
            if (stage == null) {
                // Fallback ultra-sécurisé (au cas où)
                stage = (Stage) ((Stage) javafx.stage.Window.getWindows().get(0));
            }

            stage.setScene(new Scene(root));
            stage.setTitle(user.getType_utilisateur() == Role.ETUDIANT
                    ? "Accueil - Harmony"
                    : "Dashboard Admin - Harmony");
            stage.centerOnScreen(); // optionnel mais joli

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