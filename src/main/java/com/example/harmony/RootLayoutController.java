package com.example.harmony;

import com.example.harmony.interfaces.ThemeAware;
import com.example.harmony.interfaces.WheelCyclable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class RootLayoutController {

    @FXML private ImageView logoImage;
    @FXML private HBox titleBar;
    @FXML private BorderPane root;

    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;

    @FXML private StackPane contentArea;

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;
    private final KeyCodeCombination CTRL_RIGHT =
            new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);
    private final KeyCodeCombination CTRL_LEFT =
            new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN);
    // true = dark, false = light
    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);

    // Keep reference to currently loaded content controller, so we can notify it
    private Object currentContentController;

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
            if (stage != null) stage.setIconified(true);
        });

        closeButton.setOnAction(event -> {
            SceneTransitionUtil.shutdown();
            Platform.exit();
        });

        themeToggleButton.setOnAction(event -> toggleTheme());

        // Keep UI in sync when darkMode changes
        darkMode.addListener((obs, oldV, newV) -> {
            updateThemeButtonText();
            applyThemeToScene();
            notifyThemeChanged(currentContentController);
        });

        updateThemeButtonText();
        Platform.runLater(this::applyThemeToScene);

        Platform.runLater(this::installGlobalWheelHotkeys);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public StackPane getContentArea() {
        return contentArea;
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public void setThemeMode(boolean isDarkMode) {
        darkMode.set(isDarkMode);
    }

    public <T> T loadContent(String fxmlPath, Class<T> controllerType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent content = loader.load();

        T controller = loader.getController(); // controller available after load() [web:18]
        currentContentController = controller;

        contentArea.getChildren().setAll(content);

        // Apply CSS mode class to the new content root too
        applyThemeToNode(content);
        if (controller instanceof FrontLayoutController c) {
            c.bindTheme(darkModeProperty());
        }

        // Let controller update non-CSS stuff (e.g., ImageView icons)
        notifyThemeChanged(controller);

        return controller;
    }

    private void updateThemeButtonText() {
        if (themeToggleButton == null) return;
        themeToggleButton.setText(isDarkMode() ? "☀" : "☾");
    }

    private void toggleTheme() {
        darkMode.set(!darkMode.get());
        System.out.println("Root darkMode=" + isDarkMode() + ", controller=" + currentContentController);

    }

    private void applyThemeToScene() {
        if (stage == null || stage.getScene() == null) return;

        Parent sceneRoot = stage.getScene().getRoot();
        applyThemeToNode(sceneRoot);

        // Optional: also apply class to currently loaded content root
        if (!contentArea.getChildren().isEmpty()) {
            Node currentContent = contentArea.getChildren().getFirst();
            if (currentContent instanceof Parent p) {
                applyThemeToNode(p);
            }
        }
    }

    public void applyThemeToNode(Parent node) {
        if (node == null) return;

        if (isDarkMode()) {
            node.getStyleClass().remove("light-mode");
        } else {
            if (!node.getStyleClass().contains("light-mode")) {
                node.getStyleClass().add("light-mode");
            }
        }
    }

    private void notifyThemeChanged(Object controller) {
        if (controller instanceof ThemeAware aware) {
            aware.onThemeChanged();
        }
    }

    public void setCurrentContentController(Object controller) {
        this.currentContentController = controller;
    }

    private void installGlobalWheelHotkeys() {
        Scene scene = root.getScene();
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> { /* ctrl+left/right */ });
    }


    private void installGlobalWheelHotkeys(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (CTRL_RIGHT.match(e)) {
                e.consume();
                cycleActiveWheel(+1);
            } else if (CTRL_LEFT.match(e)) {
                e.consume();
                cycleActiveWheel(-1);
            }
        });
    }



    private void cycleActiveWheel(int step) {
        if (currentContentController instanceof WheelCyclable wc) {
            wc.cycleWheel(step);
        }
    }
}
