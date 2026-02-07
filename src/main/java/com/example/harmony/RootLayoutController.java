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

/**
 * Controller for the root layout that contains the title bar and content area.
 */
public class RootLayoutController {

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

    private boolean isDarkMode = true; // Default is dark mode

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
        // Load logo
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        logoImage.setImage(logo);

        // Set up title bar functionality
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

        // Set up minimize button functionality
        minimizeButton.setOnAction(event -> {
            if (stage != null) {
                stage.setIconified(true);
            }
        });

        // Set up close button functionality
        closeButton.setOnAction(event -> {
            if (stage != null) {
                // Shutdown executor service and release resources
                SceneTransitionUtil.shutdown();

                // Ensure the application fully terminates when closed
                javafx.application.Platform.exit();
            }
        });

        // Set up theme toggle button functionality
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

        // Clear existing content and add new content
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

    /**
     * Updates the theme toggle button text based on the current theme.
     */
    private void updateThemeButtonText() {
        if (themeToggleButton != null) {
            if (isDarkMode) {
                themeToggleButton.setText("☀"); // Sun icon for light mode toggle
            } else {
                themeToggleButton.setText("☾"); // Moon icon for dark mode toggle
            }
        }
    }

    /**
     * Toggles between light and dark mode.
     */
    private void toggleTheme() {
        isDarkMode = !isDarkMode;

        // Save the theme preference to SessionManager
        //SessionManager.getInstance().setDarkMode(isDarkMode);

        // Update the button text
        updateThemeButtonText();

        // Get the scene
        if (stage != null && stage.getScene() != null) {
            if (isDarkMode) {
                // Switch to dark mode
                stage.getScene().getRoot().getStyleClass().remove("light-mode");

                // Also remove light-mode class from current content
                if (!contentArea.getChildren().isEmpty()) {
                    Node currentContent = contentArea.getChildren().get(0);
                    if (currentContent instanceof Parent) {
                        ((Parent) currentContent).getStyleClass().remove("light-mode");
                    }
                }
            } else {
                // Switch to light mode
                stage.getScene().getRoot().getStyleClass().add("light-mode");

                // Also add light-mode class to current content
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
        Set<Node> icons = root.lookupAll(".sidebar-icon"); // ImageViews in your sidebar [web:72]

        for (Node n : icons) {
            if (!(n instanceof ImageView iv)) continue;

            String base = iv.getId(); // from FXML: id="calendar-11015" etc. [web:78]
            if (base == null || base.isBlank()) continue;

            String suffix = isDarkMode ? "-dark.png" : "-light.png";
            String path = "/" + base + suffix; // resource path

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
