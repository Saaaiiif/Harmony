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

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Button btnEnvoyer;
    @FXML private Hyperlink linkRetourLogin;

    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
        linkRetourLogin.setOnAction(e -> retourLogin());
    }

    @FXML
    void handleEnvoyerCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez saisir votre adresse email.");
            return;
        }

        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse email invalide.");
            return;
        }

        // Vérifier que l'email existe dans la BDD (utilisateur actif)
        if (!resetService.emailExiste(email)) {
            // Pour des raisons de sécurité, on affiche toujours le même message
            showSuccess("Si un compte existe avec cet email, un code a été envoyé.");
            return;
        }

        // Générer un code à 6 chiffres et le stocker
        String code = resetService.genererEtSauvegarderCode(email);

        // Envoyer l'email
        boolean envoye = EmailService.envoyerCodeReset(email, code);

        if (envoye) {
            showSuccess("Code envoyé ! Vérifiez votre boîte email.");

            // Transition vers la page de vérification du code après 1.5s
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5)
            );
            pause.setOnFinished(ev -> naviguerVersVerification(email));
            pause.play();
        } else {
            showError("Erreur lors de l'envoi de l'email. Réessayez.");
        }
    }

    private void naviguerVersVerification(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserViews/VerifyCode.fxml"));
            Parent root = loader.load();

            // Passer l'email au controller suivant
            VerifyCodeController controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Vérification du code");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retourLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/UserViews/Login.fxml"));
            Stage stage = (Stage) linkRetourLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Harmony - Connexion");
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
