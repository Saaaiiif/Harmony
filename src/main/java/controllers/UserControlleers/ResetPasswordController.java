package controllers.UserControlleers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import services.UserServices.PasswordResetService;
import utils.PasswordUtils;

import java.io.IOException;

public class ResetPasswordController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label strengthLabel;
    @FXML private Region strengthBar;
    @FXML private Button btnReinitialiser;

    private String email;
    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);

        // Indicateur de force du mot de passe (même logique que InscriptionController)
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordStrength(newVal));
    }

    /**
     * Appelé par VerifyCodeController pour passer l'email vérifié.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    void handleReinitialiser() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (!isPasswordValid(newPassword)) {
            showError("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre.");
            return;
        }

        // Hasher et mettre à jour en base
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        boolean succes = resetService.mettreAJourMotDePasse(email, hashedPassword);

        if (succes) {
            showSuccess("Mot de passe réinitialisé avec succès ! Redirection...");
            btnReinitialiser.setDisable(true);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2.0)
            );
            pause.setOnFinished(ev -> retourLogin());
            pause.play();
        } else {
            showError("Une erreur est survenue. Veuillez réessayer.");
        }
    }

    private void retourLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/UserViews/Login.fxml"));
            Stage stage = (Stage) btnReinitialiser.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========== Indicateur de force (identique à l'inscription) ==========

    private void updatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        switch (score) {
            case 0, 1 -> {
                strengthBar.setPrefWidth(70);
                strengthBar.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 5;");
                strengthLabel.setText("Très faible");
                strengthLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12; -fx-font-weight: bold;");
            }
            case 2 -> {
                strengthBar.setPrefWidth(140);
                strengthBar.setStyle("-fx-background-color: #F97316; -fx-background-radius: 5;");
                strengthLabel.setText("Faible");
                strengthLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12; -fx-font-weight: bold;");
            }
            case 3 -> {
                strengthBar.setPrefWidth(210);
                strengthBar.setStyle("-fx-background-color: #EAB308; -fx-background-radius: 5;");
                strengthLabel.setText("Moyen");
                strengthLabel.setStyle("-fx-text-fill: #EAB308; -fx-font-size: 12; -fx-font-weight: bold;");
            }
            case 4 -> {
                strengthBar.setPrefWidth(280);
                strengthBar.setStyle("-fx-background-color: #22C55E; -fx-background-radius: 5;");
                strengthLabel.setText("Fort");
                strengthLabel.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 12; -fx-font-weight: bold;");
            }
            case 5 -> {
                strengthBar.setPrefWidth(350);
                strengthBar.setStyle("-fx-background-color: #16A34A; -fx-background-radius: 5;");
                strengthLabel.setText("Très fort ✓");
                strengthLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12; -fx-font-weight: bold;");
            }
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*");
    }

    private void showError(String msg) {
        messageLabel.setText("❌  " + msg);
        messageLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #FEE2E2; -fx-padding: 10 15; -fx-background-radius: 8;");
        messageLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText("✅  " + msg);
        messageLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-background-color: #D1FAE5; -fx-padding: 10 15; -fx-background-radius: 8;");
        messageLabel.setVisible(true);
    }
}
