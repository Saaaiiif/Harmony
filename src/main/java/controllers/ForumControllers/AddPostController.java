package controllers.ForumControllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.ForumModels.Post;
import models.ForumModels.Categorie;
import services.ForumServices.ServicePost;
import services.ForumServices.BadWordsService;

import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.ForumServices.TranslationService;
import services.ForumServices.SpellCheckService;
import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddPostController {

    private SpellCheckService spellService = new SpellCheckService();
    private BadWordsService badWordsService = new BadWordsService(); // ✅ AJOUTÉ
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private int currentUserId = -1;
    public void setCurrentUserId(int id) { this.currentUserId = id; }

    private void enableLiveSpellCheck() {
        titreField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                executor.submit(() -> {
                    String corrected = spellService.correctText(titreField.getText());
                    Platform.runLater(() -> titreField.setText(corrected));
                });
            }
        });
        contenuArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                executor.submit(() -> {
                    String corrected = spellService.correctText(contenuArea.getText());
                    Platform.runLater(() -> contenuArea.setText(corrected));
                });
            }
        });
    }

    // ✅ AJOUTÉ — bordure rouge en temps réel si gros mot détecté
    private void enableBadWordsLiveCheck() {
        titreField.textProperty().addListener((obs, old, newVal) -> {
            if (badWordsService.containsBadWordsLocal(newVal)) {
                titreField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            } else {
                titreField.setStyle("");
            }
        });
        contenuArea.textProperty().addListener((obs, old, newVal) -> {
            if (badWordsService.containsBadWordsLocal(newVal)) {
                contenuArea.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            } else {
                contenuArea.setStyle("");
            }
        });
    }

    @FXML
    public void initialize() {
        enableLiveSpellCheck();
        enableBadWordsLiveCheck(); // ✅ AJOUTÉ
    }

    @FXML private ImageView previewImageView;
    private File selectedImageFile;
    @FXML private Button uploadImageBtn;
    @FXML private TextField titreField;
    @FXML private TextArea contenuArea;
    private Categorie selectedCategorie;
    private ServicePost servicePost = new ServicePost();
    @FXML private Button backBtn;

    @FXML
    private void handleBack() {
        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.close();
    }

    public void setCategorie(Categorie categorie) {
        this.selectedCategorie = categorie;
    }

    @FXML
    private void handleAddPost() {
        String titre = spellService.correctText(titreField.getText());
        String contenu = spellService.correctText(contenuArea.getText());

        titreField.setText(titre);
        contenuArea.setText(contenu);

        if (titre.isEmpty() || contenu.isEmpty()) {
            System.out.println("Champs vides !");
            return;
        }

        // ✅ AJOUTÉ — vérification gros mots avant soumission
        BadWordsService.DetectionResult titreCheck = badWordsService.checkText(titre);
        BadWordsService.DetectionResult contenuCheck = badWordsService.checkText(contenu);

        if (titreCheck.hasBadWords()) {
            showBadWordAlert("titre", titreCheck.getDetectedWord());
            titreField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            return;
        }
        if (contenuCheck.hasBadWords()) {
            showBadWordAlert("contenu", contenuCheck.getDetectedWord());
            contenuArea.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 6;");
            return;
        }
        // ── texte propre, on continue ──

        String imagePath = null;
        if (selectedImageFile != null) {
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) uploadDir.mkdirs();
                String newFileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                File destination = new File(uploadDir, newFileName);
                Files.copy(selectedImageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                imagePath = destination.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Post post = new Post(titre, contenu, currentUserId, selectedCategorie.getIdCategorie(), imagePath);
        servicePost.add(post);

        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }

    // ✅ AJOUTÉ
    private void showBadWordAlert(String champ, String mot) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("⛔ Contenu inapproprié");
        alert.setHeaderText("Mot interdit dans le " + champ);
        alert.setContentText("\"" + mot + "\" n'est pas autorisé sur ce forum.\nMerci de respecter la communauté 🙏");
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-font-size: 13px;");
        alert.showAndWait();
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fileChooser.showOpenDialog(uploadImageBtn.getScene().getWindow());
        if (selectedImageFile != null) {
            uploadImageBtn.setText("✅ Image sélectionnée");
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                previewImageView.setImage(image);
                previewImageView.setVisible(true);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private TranslationService translationService = new TranslationService();

    @FXML
    private void handleTranslate() {
        String originalText = contenuArea.getText();
        if (originalText == null || originalText.isEmpty()) {
            contenuArea.setText("Veuillez entrer un texte !");
            return;
        }
        try {
            String translated = translationService.translate(originalText, "fr", "en");
            contenuArea.setText(translated);
        } catch (Exception e) {
            e.printStackTrace();
            contenuArea.setText("Problème de connexion API !");
        }
    }
}
