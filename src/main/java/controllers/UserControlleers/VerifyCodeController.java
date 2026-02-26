package controllers.UserControlleers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserServices.PasswordResetService;
import utils.EmailService;

import java.io.IOException;

public class VerifyCodeController {

    @FXML private TextField codeField;
    @FXML private Label messageLabel;
    @FXML private Label labelInfo;
    @FXML private Button btnVerifier;
    @FXML private Hyperlink linkRenvoyer;
    @FXML private Hyperlink linkRetour;

    private String email; // reçu depuis ForgotPasswordController
    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);

        // Limiter à 6 caractères numériques
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) codeField.setText(newVal.replaceAll("[^\\d]", ""));
            if (codeField.getText().length() > 6) codeField.setText(codeField.getText().substring(0, 6));
        });

        linkRetour.setOnAction(e -> retourForgotPassword());
        linkRenvoyer.setOnAction(e -> renvoyerCode());
    }

    /**
     * Appelé par ForgotPasswordController pour passer l'email.
     */
    public void setEmail(String email) {
        this.email = email;
        labelInfo.setText("Un code à 6 chiffres a été envoyé à :\n" + email);
    }

    @FXML
    void handleVerifier() {
        String code = codeField.getText().trim();

        if (code.length() != 6) {
            showError("Le code doit contenir 6 chiffres.");
            return;
        }

        boolean valide = resetService.verifierCode(email, code);

        if (valide) {
            showSuccess("Code correct ! Redirection...");

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.0)
            );
            pause.setOnFinished(ev -> naviguerVersResetPassword());
            pause.play();
        } else {
            showError("Code incorrect ou expiré. Réessayez.");
            codeField.clear();
        }
    }

    private void renvoyerCode() {
        if (email == null || email.isEmpty()) return;
        String nouveauCode = resetService.genererEtSauvegarderCode(email);
        boolean envoye = EmailService.envoyerCodeReset(email, nouveauCode);
        if (envoye) {
            showSuccess("Nouveau code envoyé à " + email);
            codeField.clear();
        } else {
            showError("Erreur lors de l'envoi. Réessayez.");
        }
    }

    private void naviguerVersResetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserViews/ResetPassword.fxml"));
            Parent root = loader.load();

            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Nouveau mot de passe");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retourForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/UserViews/ForgotPassword.fxml"));
            Stage stage = (Stage) linkRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Mot de passe oublié");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
