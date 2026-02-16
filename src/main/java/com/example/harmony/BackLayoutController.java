package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;


public class BackLayoutController {

    @FXML
    private ImageView logoImage;

    @FXML
    private HBox titleBar;

    @FXML
    private Button themeToggleButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button closeButton;

    private boolean isDarkMode = true;

    @FXML
    private StackPane contentArea;

    @FXML private VBox sidebar;

    @FXML private Button sidebarToggleButton;

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    private boolean sidebarCollapsed = false;
    private static final double SIDEBAR_EXPANDED = 320.0;
    private static final double SIDEBAR_COLLAPSED = 80.0;

    private Timeline sidebarAnim;

    @FXML
    public void initialize() {

        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        logoImage.setImage(logo);


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


        minimizeButton.setOnAction(event -> {
            if (stage != null) {
                stage.setIconified(true);
            }
        });


        closeButton.setOnAction(event -> {
            if (stage != null) {

                SceneTransitionUtil.shutdown();


                javafx.application.Platform.exit();
            }
        });
        themeToggleButton.setOnAction(event -> {
            toggleTheme();
        });

        installSidebarHoverBehavior();

    }

    /**
     * Sets the stage for window operations.
     * @param stage The primary stage of the application
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Loads content into the content area.
     * @param fxmlPath The path to the FXML file to load
     * @return The controller of the loaded FXML
     * @throws IOException If the FXML file cannot be loaded
     */
    public <T> T loadContent(String fxmlPath, Class<T> controllerType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent content = loader.load();


        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);

        return loader.getController();
    }

    /**
     * Gets the content area for direct manipulation.
     * @return The content area StackPane
     */
    public StackPane getContentArea() {
        return contentArea;
    }

    /**
     * Gets the stage for window operations.
     * @return The primary stage of the application
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Gets the current theme mode.
     * @return true if dark mode, false if light mode
     */
    public boolean isDarkMode() {
        return isDarkMode;
    }

    /**
     * Sets the theme mode.
     * @param isDarkMode true for dark mode, false for light mode
     */
    public void setThemeMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        updateThemeButtonText();
    }


    private void updateThemeButtonText() {
        if (themeToggleButton != null) {
            if (isDarkMode) {
                themeToggleButton.setText("☀");
            } else {
                themeToggleButton.setText("☾");
            }
        }
    }

    /**
     * Toggles between light and dark mode.
     */
    private void toggleTheme() {
        isDarkMode = !isDarkMode;

        updateThemeButtonText();

        if (stage != null && stage.getScene() != null) {
            if (isDarkMode) {
                stage.getScene().getRoot().getStyleClass().remove("light-mode");

                if (!contentArea.getChildren().isEmpty()) {
                    Node currentContent = contentArea.getChildren().get(0);
                    if (currentContent instanceof Parent) {
                        ((Parent) currentContent).getStyleClass().remove("light-mode");
                    }
                }
            } else {
                stage.getScene().getRoot().getStyleClass().add("light-mode");

                if (!contentArea.getChildren().isEmpty()) {
                    Node currentContent = contentArea.getChildren().get(0);
                    if (currentContent instanceof Parent) {
                        ((Parent) currentContent).getStyleClass().add("light-mode");
                    }
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

            String suffix = isDarkMode ? "-dark.png" : "-light.png";
            String path = "/" + base + suffix;

            iv.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
        }
    }

    private void installSidebarHoverBehavior() {
        javafx.application.Platform.runLater(() -> {
            setSidebarCollapsed(true);
            sidebar.setPrefWidth(SIDEBAR_COLLAPSED);
            sidebar.setMinWidth(SIDEBAR_COLLAPSED);
            sidebar.setMaxWidth(SIDEBAR_COLLAPSED);
        });

        sidebar.setOnMouseEntered(e -> {
            setSidebarCollapsed(false);
            animateSidebarTo(SIDEBAR_EXPANDED);
        });

        sidebar.setOnMouseExited(e -> {
            setSidebarCollapsed(true);
            animateSidebarTo(SIDEBAR_COLLAPSED);
        });
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
            if (!sidebar.getStyleClass().contains("sidebar-collapsed")) {
                sidebar.getStyleClass().add("sidebar-collapsed");
            }
        } else {
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }
    }



}
