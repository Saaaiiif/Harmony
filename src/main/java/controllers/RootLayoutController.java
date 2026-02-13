package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class RootLayoutController {

    @FXML private ImageView logoImage;
    @FXML private HBox titleBar;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggleButton;

    private boolean isDarkMode = true;
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
            System.out.println("Logo introuvable, on continue sans.");
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

        closeButton.setOnAction(event -> {
            if (stage != null) {
                // Utilisation directe car SceneTransitionUtil est dans le même package (controllers)
                SceneTransitionUtil.shutdown();
                javafx.application.Platform.exit();
            }
        });

        themeToggleButton.setOnAction(event -> toggleTheme());
        installSidebarHoverBehavior();
    }

    public void setStage(Stage stage) { this.stage = stage; }

    public <T> T loadContent(String fxmlPath, Class<T> controllerType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent content = loader.load();
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
        return loader.getController();
    }

    public StackPane getContentArea() { return contentArea; }
    public Stage getStage() { return stage; }
    public boolean isDarkMode() { return isDarkMode; }

    public void setThemeMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        updateThemeButtonText();
    }

    private void updateThemeButtonText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(isDarkMode ? "☀" : "☾");
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateThemeButtonText();

        if (stage != null && stage.getScene() != null) {
            if (isDarkMode) {
                stage.getScene().getRoot().getStyleClass().remove("light-mode");
                if (!contentArea.getChildren().isEmpty()) {
                    Node currentContent = contentArea.getChildren().get(0);
                    if (currentContent instanceof Parent) ((Parent) currentContent).getStyleClass().remove("light-mode");
                }
            } else {
                stage.getScene().getRoot().getStyleClass().add("light-mode");
                if (!contentArea.getChildren().isEmpty()) {
                    Node currentContent = contentArea.getChildren().get(0);
                    if (currentContent instanceof Parent) ((Parent) currentContent).getStyleClass().add("light-mode");
                }
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
                String suffix = isDarkMode ? "-dark.png" : "-light.png";
                String path = "/" + base + suffix;
                iv.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
            } catch (Exception e) {
                // Ignore si l'image manque
            }
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
        sidebarAnim = new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(sidebar.prefWidthProperty(), w),
                        new KeyValue(sidebar.minWidthProperty(), w),
                        new KeyValue(sidebar.maxWidthProperty(), w)
                )
        );
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