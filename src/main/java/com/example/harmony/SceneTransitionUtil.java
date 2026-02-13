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

/**
 * Utility class for handling scene transitions with animations.
 */
public class SceneTransitionUtil {

    // Static reference to the root layout controller
    private static RootLayoutController rootController;

    // Executor service for background tasks
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Shuts down the executor service and releases resources.
     * This should be called when the application is shutting down.
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    /**
     * Sets the root layout controller.
     * @param controller The root layout controller
     */
    public static void setRootController(RootLayoutController controller) {
        rootController = controller;
    }

    /**
     * Gets the root layout controller.
     * @return The root layout controller
     */
    public static RootLayoutController getRootController() {
        return rootController;
    }

    /**
     * Creates a loading indicator with a spinner and message.
     * 
     * @param message The message to display
     * @return A VBox containing the loading indicator
     */
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

    /**
     * Transition types available for scene changes.
     */
    public enum TransitionType {
        FADE,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        ZOOM
    }

    /**
     * Changes the scene with an animation.
     *
     * @param currentScene The current scene
     * @param fxmlPath The path to the FXML file for the new scene
     * @param transitionType The type of transition animation to use
     * @param controller The controller object that will be returned by the FXMLLoader
     * @param <T> The type of the controller
     * @return The controller of the loaded FXML
     * @throws IOException If the FXML file cannot be loaded
     */
    public static <T> T changeScene(Scene currentScene, String fxmlPath, TransitionType transitionType, Class<T> controller) throws IOException {
        Stage stage = (Stage) currentScene.getWindow();
        FXMLLoader loader = new FXMLLoader(SceneTransitionUtil.class.getResource(fxmlPath));
        Parent root = loader.load();
        // Set a dark background color for the root node to prevent white flash
        root.setStyle("-fx-background-color: #121212;");
        Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
        // Set the scene fill to dark color to prevent white flash during transition
        newScene.setFill(javafx.scene.paint.Color.rgb(18, 18, 18));
        newScene.getStylesheets().add(Objects.requireNonNull(SceneTransitionUtil.class.getResource("/com/example/harmony/styles.css")).toExternalForm());

        // Apply animation to the current scene before switching
        Parent currentRoot = currentScene.getRoot();

        switch (transitionType) {
            case FADE:
                fadeTransition(stage, currentRoot, root, newScene);
                break;
            case SLIDE_LEFT:
                slideTransition(stage, currentRoot, root, newScene, -1);
                break;
            case SLIDE_RIGHT:
                slideTransition(stage, currentRoot, root, newScene, 1);
                break;
            case ZOOM:
                zoomTransition(stage, currentRoot, root, newScene);
                break;
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
        // Prepare the new scene to slide in from the side
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentRoot);
        slideOut.setFromX(0);
        slideOut.setToX(direction * -currentRoot.getScene().getWidth());

        slideOut.setOnFinished(event -> {
            stage.setScene(newScene);

            // Start with the new scene off-screen
            newRoot.setTranslateX(direction * newScene.getWidth());

            // Slide the new scene in
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newRoot);
            slideIn.setFromX(direction * newScene.getWidth());
            slideIn.setToX(0);
            slideIn.play();
        });

        slideOut.play();
    }

    private static void zoomTransition(Stage stage, Parent currentRoot, Parent newRoot, Scene newScene) {
        // Zoom out the current scene
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

            // Start with the new scene zoomed in
            newRoot.setScaleX(1.2);
            newRoot.setScaleY(1.2);
            newRoot.setOpacity(0.0);

            // Zoom in the new scene
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

    /**
     * Changes the content in the root layout with an animation.
     *
     * @param fxmlPath The path to the FXML file for the new content
     * @param transitionType The type of transition animation to use
     * @param controller The controller object that will be returned by the FXMLLoader
     * @param <T> The type of the controller
     * @return The controller of the loaded FXML
     * @throws IOException If the FXML file cannot be loaded
     */
    public static <T> T changeContent(String fxmlPath, TransitionType transitionType, Class<T> controller) throws IOException {
        if (rootController == null) {
            throw new IllegalStateException("Root controller not set. Call setRootController first.");
        }

        StackPane contentArea = rootController.getContentArea();

        // Load the new content
        FXMLLoader loader = new FXMLLoader(SceneTransitionUtil.class.getResource(fxmlPath));
        Parent newContent = loader.load();

        // Get the theme preference from SessionManager
      //  boolean isDarkMode = SessionManager.getInstance().isDarkMode();
        //if (!isDarkMode) {
            // If light mode is selected, apply it to the new content
       //     newContent.getStyleClass().add("light-mode");
       // }

        // Set the theme mode for controllers that support it
        // No specific controller theme handling needed for this simplified version

        // Apply animation to the current content before switching
        if (!contentArea.getChildren().isEmpty()) {
            Node currentContent = contentArea.getChildren().get(0);

            switch (transitionType) {
                case FADE:
                    fadeContentTransition(contentArea, currentContent, newContent);
                    break;
                case SLIDE_LEFT:
                    slideContentTransition(contentArea, currentContent, newContent, -1);
                    break;
                case SLIDE_RIGHT:
                    slideContentTransition(contentArea, currentContent, newContent, 1);
                    break;
                case ZOOM:
                    zoomContentTransition(contentArea, currentContent, newContent);
                    break;
            }
        } else {
            // No current content, just add the new content
            contentArea.getChildren().add(newContent);
        }

        return loader.getController();
    }

    /**
     * Changes the content in the root layout with an animation, but first preloads data.
     * Shows a loading indicator while data is being loaded.
     *
     * @param fxmlPath The path to the FXML file for the new content
     * @param transitionType The type of transition animation to use
     * @param controller The controller object that will be returned by the FXMLLoader
     * @param dataLoader A function that loads data and calls the provided callback when done
     * @param <T> The type of the controller
     * @throws IOException If the FXML file cannot be loaded
     */
    public static <T> void changeContentWithPreload(
            String fxmlPath, 
            TransitionType transitionType, 
            Class<T> controller,
            Consumer<T> dataLoader) throws IOException {

        if (rootController == null) {
            throw new IllegalStateException("Root controller not set. Call setRootController first.");
        }

        StackPane contentArea = rootController.getContentArea();

        // Create and show loading indicator
        VBox loadingIndicator = createLoadingIndicator("Loading...");

        // Apply fade transition to current content
        if (!contentArea.getChildren().isEmpty()) {
            Node currentContent = contentArea.getChildren().get(0);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loadingIndicator);


                // Load the FXML in background
                Task<FXMLLoader> loadTask = new Task<FXMLLoader>() {
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

                        // Load data in background
                        Task<Void> dataTask = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                // This will run in background thread
                                return null;
                            }
                        };

                        dataTask.setOnSucceeded(dataEvent -> {
                            // Now that data is loaded, transition to the new content
                            Platform.runLater(() -> {
                                // Get the theme preference from SessionManager
                               // boolean isDarkMode = SessionManager.getInstance().isDarkMode();
                              //  if (!isDarkMode) {
                                    // If light mode is selected, apply it to the new content
                              //      newContent.getStyleClass().add("light-mode");
                              //  }

                                // Set the theme mode for controllers that support it
                                // No specific controller theme handling needed for this simplified version

                                contentArea.getChildren().clear();
                                contentArea.getChildren().add(newContent);

                                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
                                fadeIn.setFromValue(0.0);
                                fadeIn.setToValue(1.0);
                                fadeIn.play();

                                // Call the data loader with the controller instance
                                dataLoader.accept(controllerInstance);
                            });
                        });

                        dataTask.setOnFailed(dataEvent -> {
                            // Handle data loading failure
                            Platform.runLater(() -> {
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
                            });
                        });

                        // Start data loading
                        executor.submit(dataTask);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                loadTask.setOnFailed(e -> {
                    // Handle FXML loading failure
                    Platform.runLater(() -> {
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
                    });
                });

                // Start FXML loading
                executor.submit(loadTask);
            });

            fadeOut.play();
        } else {
            // No current content, just add the loading indicator
            contentArea.getChildren().add(loadingIndicator);

            // Continue with loading as above
            // (This code is duplicated from above, but it's clearer this way)

            // Load the FXML in background
            Task<FXMLLoader> loadTask = new Task<FXMLLoader>() {
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

                    // Load data in background
                    Task<Void> dataTask = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            // This will run in background thread
                            return null;
                        }
                    };

                    dataTask.setOnSucceeded(dataEvent -> {
                        // Now that data is loaded, transition to the new content
                        Platform.runLater(() -> {
                            // Get the theme preference from SessionManager
                           // boolean isDarkMode = SessionManager.getInstance().isDarkMode();
                           // if (!isDarkMode) {
                                // If light mode is selected, apply it to the new content
                           //     newContent.getStyleClass().add("light-mode");
                         //   }
                            // Set the theme mode for controllers that support it
                            // No specific controller theme handling needed for this simplified version

                            contentArea.getChildren().clear();
                            contentArea.getChildren().add(newContent);

                            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
                            fadeIn.setFromValue(0.0);
                            fadeIn.setToValue(1.0);
                            fadeIn.play();

                            // Call the data loader with the controller instance
                            dataLoader.accept(controllerInstance);
                        });
                    });

                    dataTask.setOnFailed(dataEvent -> {
                        // Handle data loading failure
                        Platform.runLater(() -> {
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
                        });
                    });

                    // Start data loading
                    executor.submit(dataTask);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            loadTask.setOnFailed(e -> {
                // Handle FXML loading failure
                Platform.runLater(() -> {
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
                });
            });

            // Start FXML loading
            executor.submit(loadTask);
        }
    }

    private static void fadeContentTransition(StackPane contentArea, Node currentContent, Parent newContent) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(newContent);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private static void slideContentTransition(StackPane contentArea, Node currentContent, Parent newContent, int direction) {
        double width = contentArea.getWidth();

        // Prepare the new content to slide in from the side
        newContent.setTranslateX(direction * width);
        contentArea.getChildren().add(newContent);

        // Slide out the current content
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentContent);
        slideOut.setFromX(0);
        slideOut.setToX(direction * -width);

        // Slide in the new content
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newContent);
        slideIn.setFromX(direction * width);
        slideIn.setToX(0);

        ParallelTransition parallelTransition = new ParallelTransition(slideOut, slideIn);
        parallelTransition.setOnFinished(event -> {
            contentArea.getChildren().remove(currentContent);
            newContent.setTranslateX(0);
        });

        parallelTransition.play();
    }

    private static void zoomContentTransition(StackPane contentArea, Node currentContent, Parent newContent) {
        // Zoom out the current content
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
            contentArea.getChildren().clear();
            contentArea.getChildren().add(newContent);

            // Start with the new content zoomed in
            newContent.setScaleX(1.2);
            newContent.setScaleY(1.2);
            newContent.setOpacity(0.0);

            // Zoom in the new content
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), newContent);
            scaleIn.setFromX(1.2);
            scaleIn.setFromY(1.2);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            ParallelTransition parallelIn = new ParallelTransition(scaleIn, fadeIn);
            parallelIn.play();
        });

        parallelOut.play();
    }
}
