package com.example.harmony;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SceneTransitionUtil {

    private static RootLayoutController rootController;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static java.util.function.IntConsumer wheelCycler;
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public static void setRootController(RootLayoutController controller) {
        rootController = controller;
    }
    public static void setWheelCycler(java.util.function.IntConsumer cycler) {
        wheelCycler = cycler;
    }
    public static void cycleWheel(int step) {
        if (wheelCycler != null) wheelCycler.accept(step);
    }
    public static RootLayoutController getRootController() {
        if (rootController == null) {
            throw new IllegalStateException("Root controller not set (call setRootController after loading root FXML)");
        }
        return rootController;
    }
    private static VBox createLoadingIndicator(String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinner.setStyle("-fx-progress-color: #BD2526;");

        Label loadingLabel = new Label(message);
        loadingLabel.setStyle("-fx-font-family: 'Feather Bold'; -fx-font-size: 16px; -fx-text-fill: #acacac;");

        VBox loadingBox = new VBox(10, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-background-color: rgba(30, 30, 30, 0.8); -fx-background-radius: 10px; -fx-padding: 20px;");
        return loadingBox;
    }

    public enum TransitionType {
        FADE,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        ZOOM
    }

    /**
     * Only use changeScene if you are actually replacing the whole Scene.
     * If you are using RootLayout + content swapping, prefer changeContent.
     */
    public static <T> T changeScene(Scene currentScene, String fxmlPath, TransitionType transitionType, Class<T> controller)
            throws IOException {

        Stage stage = (Stage) currentScene.getWindow();

        FXMLLoader loader = new FXMLLoader(SceneTransitionUtil.class.getResource(fxmlPath));
        Parent newRoot = loader.load();

        Scene newScene = new Scene(newRoot, currentScene.getWidth(), currentScene.getHeight());
        newScene.getStylesheets().add(Objects.requireNonNull(
                SceneTransitionUtil.class.getResource("/com/example/harmony/styles.css")
        ).toExternalForm());



        Parent currentRoot = currentScene.getRoot();

        switch (transitionType) {
            case FADE -> fadeTransition(stage, currentRoot, newRoot, newScene);
            case SLIDE_LEFT -> slideTransition(stage, currentRoot, newRoot, newScene, -1);
            case SLIDE_RIGHT -> slideTransition(stage, currentRoot, newRoot, newScene, 1);
            case ZOOM -> zoomTransition(stage, currentRoot, newRoot, newScene);
        }

        return loader.getController();
    }

    private static void fadeTransition(Stage stage, Parent currentRoot, Parent newRoot, Scene newScene) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            stage.setScene(newScene);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private static void slideTransition(Stage stage, Parent currentRoot, Parent newRoot, Scene newScene, int direction) {
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentRoot);
        slideOut.setFromX(0);
        slideOut.setToX(direction * -currentRoot.getScene().getWidth());

        slideOut.setOnFinished(event -> {
            stage.setScene(newScene);

            newRoot.setTranslateX(direction * newScene.getWidth());

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newRoot);
            slideIn.setFromX(direction * newScene.getWidth());
            slideIn.setToX(0);
            slideIn.play();
        });

        slideOut.play();
    }

    private static void zoomTransition(Stage stage, Parent currentRoot, Parent newRoot, Scene newScene) {
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), currentRoot);
        scaleOut.setFromX(1.0);
        scaleOut.setFromY(1.0);
        scaleOut.setToX(0.8);
        scaleOut.setToY(0.8);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition parallelOut = new ParallelTransition(scaleOut, fadeOut);

        parallelOut.setOnFinished(event -> {
            stage.setScene(newScene);

            newRoot.setScaleX(1.2);
            newRoot.setScaleY(1.2);
            newRoot.setOpacity(0.0);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), newRoot);
            scaleIn.setFromX(1.2);
            scaleIn.setFromY(1.2);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            ParallelTransition parallelIn = new ParallelTransition(scaleIn, fadeIn);
            parallelIn.play();
        });

        parallelOut.play();
    }

    public static <T> T changeContent(String fxmlPath, TransitionType transitionType, Class<T> controllerClass) throws IOException {
        if (rootController == null) {
            throw new IllegalStateException("Root controller not set. Call setRootController first.");
        }

        StackPane contentArea = rootController.getContentArea();

        FXMLLoader loader = new FXMLLoader(SceneTransitionUtil.class.getResource(fxmlPath));
        Parent newContent = loader.load();

        Object c = loader.getController();
        rootController.setCurrentContentController(c);

        if (c instanceof com.example.harmony.interfaces.WheelCyclable wc) {
            setWheelCycler(wc::cycleWheel);
        } else {
            setWheelCycler(null);
        }

        rootController.applyThemeToNode(newContent);

        if (!contentArea.getChildren().isEmpty()) {
            Node currentContent = contentArea.getChildren().get(0);

            switch (transitionType) {
                case FADE -> fadeContentTransition(contentArea, currentContent, newContent);
                case SLIDE_LEFT -> slideContentTransition(contentArea, currentContent, newContent, -1);
                case SLIDE_RIGHT -> slideContentTransition(contentArea, currentContent, newContent, 1);
                case ZOOM -> zoomContentTransition(contentArea, currentContent, newContent);
            }
        } else {
            contentArea.getChildren().add(newContent);
        }

        return loader.getController();
    }


    public static <T> void changeContentWithPreload(
            String fxmlPath,
            TransitionType transitionType,
            Class<T> controller,
            Consumer<T> dataLoader) throws IOException {

        if (rootController == null) {
            throw new IllegalStateException("Root controller not set. Call setRootController first.");
        }

        StackPane contentArea = rootController.getContentArea();
        VBox loadingIndicator = createLoadingIndicator("Loading...");

        Runnable startLoad = () -> {
            Task<FXMLLoader> loadTask = new Task<>() {
                @Override
                protected FXMLLoader call() throws Exception {
                    FXMLLoader loader = new FXMLLoader(SceneTransitionUtil.class.getResource(fxmlPath));
                    loader.load();
                    return loader;
                }
            };

            loadTask.setOnSucceeded(e -> {
                try {
                    FXMLLoader loader = loadTask.getValue();
                    Parent newContent = (Parent) loader.getRoot();
                    T controllerInstance = loader.getController();
                    rootController.applyThemeToNode(newContent);
                    Task<Void> dataTask = new Task<>() {
                        @Override
                        protected Void call() {
                            return null;
                        }
                    };

                    dataTask.setOnSucceeded(dataEvent -> Platform.runLater(() -> {
                        contentArea.getChildren().setAll(newContent);

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();

                        dataLoader.accept(controllerInstance);
                    }));

                    dataTask.setOnFailed(dataEvent -> Platform.runLater(() -> {
                        contentArea.getChildren().clear();

                        Label errorLabel = new Label("Failed to load data. Please try again.");
                        errorLabel.setStyle("-fx-font-family: 'Feather Bold'; -fx-font-size: 16px; -fx-text-fill: #BD2526;");

                        Button retryButton = new Button("Retry");
                        retryButton.setStyle("-fx-background-color: #BD2526; -fx-text-fill: white; -fx-font-family: 'Feather Bold';");
                        retryButton.setOnAction(actionEvent -> {
                            try {
                                changeContentWithPreload(fxmlPath, transitionType, controller, dataLoader);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });

                        VBox errorBox = new VBox(10, errorLabel, retryButton);
                        errorBox.setAlignment(Pos.CENTER);
                        contentArea.getChildren().add(errorBox);
                    }));

                    executor.submit(dataTask);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            loadTask.setOnFailed(e -> Platform.runLater(() -> {
                contentArea.getChildren().clear();

                Label errorLabel = new Label("Failed to load view. Please try again.");
                errorLabel.setStyle("-fx-font-family: 'Feather Bold'; -fx-font-size: 16px; -fx-text-fill: #BD2526;");

                Button retryButton = new Button("Retry");
                retryButton.setStyle("-fx-background-color: #BD2526; -fx-text-fill: white; -fx-font-family: 'Feather Bold';");
                retryButton.setOnAction(actionEvent -> {
                    try {
                        changeContentWithPreload(fxmlPath, transitionType, controller, dataLoader);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                VBox errorBox = new VBox(10, errorLabel, retryButton);
                errorBox.setAlignment(Pos.CENTER);
                contentArea.getChildren().add(errorBox);
            }));

            executor.submit(loadTask);
        };

        if (!contentArea.getChildren().isEmpty()) {
            Node currentContent = contentArea.getChildren().get(0);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                contentArea.getChildren().setAll(loadingIndicator);
                startLoad.run();
            });

            fadeOut.play();
        } else {
            contentArea.getChildren().setAll(loadingIndicator);
            startLoad.run();
        }
    }

    private static void fadeContentTransition(StackPane contentArea, Node currentContent, Parent newContent) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            contentArea.getChildren().setAll(newContent);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private static void slideContentTransition(StackPane contentArea, Node currentContent, Parent newContent, int direction) {
        double width = contentArea.getWidth();

        newContent.setTranslateX(direction * width);
        contentArea.getChildren().add(newContent);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentContent);
        slideOut.setFromX(0);
        slideOut.setToX(direction * -width);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newContent);
        slideIn.setFromX(direction * width);
        slideIn.setToX(0);

        ParallelTransition pt = new ParallelTransition(slideOut, slideIn);
        pt.setOnFinished(event -> {
            contentArea.getChildren().remove(currentContent);
            newContent.setTranslateX(0);
        });

        pt.play();
    }

    private static void zoomContentTransition(StackPane contentArea, Node currentContent, Parent newContent) {
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), currentContent);
        scaleOut.setFromX(1.0);
        scaleOut.setFromY(1.0);
        scaleOut.setToX(0.8);
        scaleOut.setToY(0.8);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition parallelOut = new ParallelTransition(scaleOut, fadeOut);

        parallelOut.setOnFinished(event -> {
            contentArea.getChildren().setAll(newContent);

            newContent.setScaleX(1.2);
            newContent.setScaleY(1.2);
            newContent.setOpacity(0.0);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), newContent);
            scaleIn.setFromX(1.2);
            scaleIn.setFromY(1.2);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            new ParallelTransition(scaleIn, fadeIn).play();
        });

        parallelOut.play();
    }
}
