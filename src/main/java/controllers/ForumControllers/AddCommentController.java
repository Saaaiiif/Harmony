package controllers.ForumControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.ForumModels.Commentaire;
import models.ForumModels.Post;
import services.ForumServices.BadWordsService;
import services.ForumServices.ServiceCommentaire;
import services.ForumServices.SpellCheckService;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCommentController {

    private SpellCheckService spellService = new SpellCheckService();
    private BadWordsService badWordsService = new BadWordsService(); // ✅ AJOUTÉ
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML private TextArea contenuField;
    @FXML private Button backBtn;

    private Post post;
    private ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    private int currentUserId = -1;
    public void setCurrentUserId(int id) { this.currentUserId = id; }

    @FXML
    public void initialize() {
        // Spell check existant
        contenuField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                executor.submit(() -> {
                    String corrected = spellService.correctText(contenuField.getText());
                    Platform.runLater(() -> contenuField.setText(corrected));
                });
            }
        });

        // ✅ AJOUTÉ — bordure rouge temps réel
        contenuField.textProperty().addListener((obs, old, newVal) -> {
            if (badWordsService.containsBadWordsLocal(newVal)) {
                contenuField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            } else {
                contenuField.setStyle("");
            }
        });
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.close();
    }

    public void setPost(Post post) { this.post = post; }

    @FXML
    private void handleSubmit() {
        String contenu = spellService.correctText(contenuField.getText());
        contenuField.setText(contenu);

        if (contenu.isEmpty()) return;

        // ✅ AJOUTÉ — vérification gros mots avant soumission
        BadWordsService.DetectionResult check = badWordsService.checkText(contenu);
        if (check.hasBadWords()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("⛔ Contenu inapproprié");
            alert.setHeaderText("Mot interdit détecté");
            alert.setContentText("\"" + check.getDetectedWord() + "\" n'est pas autorisé.\nMerci de respecter la communauté 🙏");
            alert.getDialogPane().setStyle("-fx-background-color: white; -fx-font-size: 13px;");
            alert.showAndWait();
            contenuField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            return;
        }
        // ── texte propre ──

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(contenu);
        commentaire.setIdPost(post.getIdPost());
        commentaire.setIdEtudiant(currentUserId);
        commentaire.setDateCommentaire(LocalDateTime.now());
        serviceCommentaire.add(commentaire);

        Stage stage = (Stage) contenuField.getScene().getWindow();
        stage.close();
    }
}
