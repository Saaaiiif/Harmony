package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RootLayoutController {

    @FXML private ImageView logoImage;
    @FXML private HBox titleBar;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;

    private boolean isDarkMode = false;
    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    private boolean sidebarCollapsed = false;
    private static final double SIDEBAR_EXPANDED = 320.0;
    private static final double SIDEBAR_COLLAPSED = 80.0;
    private Timeline sidebarAnim;

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo introuvable.");
        }

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            if (stage != null) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        minimizeButton.setOnAction(event -> { if (stage != null) stage.setIconified(true); });
        closeButton.setOnAction(event -> { if (stage != null) javafx.application.Platform.exit(); });

        themeToggleButton.setOnAction(event -> toggleTheme());
        installSidebarHoverBehavior();
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public StackPane getContentArea() { return contentArea; }

    public <T> T loadContent(String fxmlPath, Class<T> controllerType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent content = loader.load();
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
        return loader.getController();
    }

    @FXML
    void goToGestionNutrition(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/GestionNutrition.fxml");
            if (fxmlUrl == null) {
                System.err.println("⚠️ Le fichier /GestionNutrition.fxml n'existe pas encore.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToGestionSport(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/GestionSport.fxml");
            if (fxmlUrl == null) {
                System.err.println("⚠️ Le fichier /GestionSport.fxml n'existe pas encore.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================================
    // LE CORRECTIF EST ICI ! (Aucun changement au Front-Office)
    // ==========================================================
    @FXML
    void goToFrontOffice(ActionEvent event) {
        try {
            // 1. On charge LE VRAI CONTENEUR DU FRONT (front-layout.fxml) et non pas juste l'Accueil
            URL fxmlUrl = getClass().getResource("/front-layout.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.setFill(javafx.scene.paint.Color.WHITE);

            URL frontCssUrl = getClass().getResource("/styles.css");
            if (frontCssUrl != null) {
                scene.getStylesheets().add(frontCssUrl.toExternalForm());
            }

            root.getStyleClass().add("light-mode");

            currentStage.setScene(scene);
            currentStage.centerOnScreen();
            currentStage.show();

            // 2. On récupère ton FrontLayoutController pour réactiver le "FrontLayoutController.instance"
            controllers.FrontLayoutController controller = loader.getController();
            if (controller != null) {
                controller.setStage(currentStage);
                // 3. On lui dit d'afficher AccueilActivite.fxml à l'intérieur !
                controller.loadPage("/AccueilActivite.fxml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================================
    // THÈME ET ANIMATIONS
    // ==========================================================

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (themeToggleButton != null) themeToggleButton.setText(isDarkMode ? "☀" : "☾");

        if (stage != null && stage.getScene() != null) {
            if (isDarkMode) {
                stage.getScene().getRoot().getStyleClass().remove("light-mode");
            } else {
                stage.getScene().getRoot().getStyleClass().add("light-mode");
            }
        }
        updateSidebarIcons();
    }

    private void updateSidebarIcons() {
        if (stage == null || stage.getScene() == null) return;
        Parent root = stage.getScene().getRoot();
        Set<Node> icons = root.lookupAll(".sidebar-icon");

        for (Node n : icons) {
            if (!(n instanceof ImageView iv)) continue;
            String base = iv.getId();
            if (base == null || base.isBlank()) continue;
            try {
                String suffix = isDarkMode ? "-light.png" : "-dark.png";
                iv.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/" + base + suffix))));
            } catch (Exception ignored) {}
        }
    }

    private void installSidebarHoverBehavior() {
        javafx.application.Platform.runLater(() -> {
            setSidebarCollapsed(true);
            sidebar.setPrefWidth(SIDEBAR_COLLAPSED);
            sidebar.setMinWidth(SIDEBAR_COLLAPSED);
            sidebar.setMaxWidth(SIDEBAR_COLLAPSED);
        });

        sidebar.setOnMouseEntered(e -> { setSidebarCollapsed(false); animateSidebarTo(SIDEBAR_EXPANDED); });
        sidebar.setOnMouseExited(e -> { setSidebarCollapsed(true); animateSidebarTo(SIDEBAR_COLLAPSED); });
    }

    private void animateSidebarTo(double w) {
        if (sidebarAnim != null) sidebarAnim.stop();
        sidebarAnim = new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(sidebar.prefWidthProperty(), w),
                new KeyValue(sidebar.minWidthProperty(), w),
                new KeyValue(sidebar.maxWidthProperty(), w)));
        sidebarAnim.play();
    }

    private void setSidebarCollapsed(boolean collapsed) {
        sidebarCollapsed = collapsed;
        if (collapsed) {
            if (!sidebar.getStyleClass().contains("sidebar-collapsed")) sidebar.getStyleClass().add("sidebar-collapsed");
        } else {
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }
    }
}