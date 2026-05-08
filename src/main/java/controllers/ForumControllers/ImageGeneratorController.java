package controllers.ForumControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.ForumServices.ImageGenerationService;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageGeneratorController {

    @FXML private TextArea promptArea;
    @FXML private ImageView generatedImage;
    @FXML private ProgressIndicator loader;
    @FXML private Label statusLabel;
    @FXML private Button generateBtn;
    @FXML private VBox resultBox;
    @FXML private HBox styleBox;

    private ImageGenerationService service = new ImageGenerationService();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String selectedStyle = "";
    private byte[] lastImageBytes = null;

    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    // ── Sélection style ───────────────────────────────────────────────────────
    @FXML
    private void selectStyle(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        selectedStyle = (String) clicked.getUserData();
        for (javafx.scene.Node node : styleBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().remove("style-chip-active");
            }
        }
        clicked.getStyleClass().add("style-chip-active");
    }

    // ── Génération ────────────────────────────────────────────────────────────
    @FXML
    private void handleGenerate() {
        String prompt = promptArea.getText();
        if (prompt == null || prompt.trim().isEmpty()) {
            statusLabel.setText("⚠️  Écrivez d'abord une description !");
            statusLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        // UI loading state
        loader.setVisible(true);
        generateBtn.setDisable(true);
        statusLabel.setText("⏳  Génération en cours... (30-60 secondes)");
        statusLabel.setStyle("-fx-text-fill: #6b7280;");
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        executor.submit(() -> {
            try {
                // ✅ Appel Hugging Face → retourne bytes PNG directement
                byte[] imageBytes = service.generateImageBytes(prompt.trim(), selectedStyle);
                lastImageBytes = imageBytes;

                // Charger l'image depuis les bytes
                Image image = new Image(new ByteArrayInputStream(imageBytes));

                Platform.runLater(() -> {
                    loader.setVisible(false);
                    generateBtn.setDisable(false);

                    if (image.isError()) {
                        statusLabel.setText("❌  Format image invalide.");
                        statusLabel.setStyle("-fx-text-fill: #ef4444;");
                    } else {
                        generatedImage.setImage(image);
                        resultBox.setVisible(true);
                        resultBox.setManaged(true);
                        statusLabel.setText("✅  Image générée !");
                        statusLabel.setStyle("-fx-text-fill: #10b981;");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loader.setVisible(false);
                    generateBtn.setDisable(false);
                    statusLabel.setText("❌  " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ef4444;");
                });
            }
        });
    }

    // ── Sauvegarder l'image ───────────────────────────────────────────────────
    @FXML
    private void handleCopyUrl() {
        if (lastImageBytes == null) return;
        try {
            // Sauvegarder dans le dossier temp et ouvrir
            java.io.File tempFile = java.io.File.createTempFile("generated_image_", ".png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(lastImageBytes);
            }
            // Copier le chemin dans le presse-papier
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(tempFile.getAbsolutePath());
            clipboard.setContent(content);
            statusLabel.setText("📋  Chemin copié : " + tempFile.getName());
            statusLabel.setStyle("-fx-text-fill: #7c3aed;");

            // Ouvrir avec le viewer par défaut
            java.awt.Desktop.getDesktop().open(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Fermer ────────────────────────────────────────────────────────────────
    @FXML
    private void handleClose() {
        Stage stage = (Stage) promptArea.getScene().getWindow();
        stage.close();
        executor.shutdownNow();
    }
}
