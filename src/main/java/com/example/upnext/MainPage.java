package com.example.upnext;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class MainPage extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Load font
        Font.loadFont(getClass().getResourceAsStream("/Feather.ttf"), 18);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Bold.otf"), 16);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Light.otf"), 24);

        // Set the application icon
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        stage.getIcons().add(appIcon);

        // Make the stage undecorated to remove the default title bar
        stage.initStyle(StageStyle.UNDECORATED);

        // Load the root layout
        FXMLLoader rootLoader = new FXMLLoader(MainPage.class.getResource("root-layout.fxml"));
        javafx.scene.Parent rootLayout = rootLoader.load();
        rootLayout.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(rootLayout, 1280, 720);
        // Set the scene fill to dark color to prevent white flash
        scene.setFill(javafx.scene.paint.Color.WHITE);
        scene.getStylesheets().add(Objects.requireNonNull(MainPage.class.getResource("styles.css")).toExternalForm());

        // Get the root controller and set the stage
        RootLayoutController rootController = rootLoader.getController();
        rootController.setStage(stage);

        // Get the root controller and set the stage


// Force light mode on startup
        rootLayout.getStyleClass().add("light-mode");
        rootController.setThemeMode(false);

        // Set the root controller in SceneTransitionUtil
        SceneTransitionUtil.setRootController(rootController);

        stage.setTitle("Harmony");
        stage.setScene(scene);
        stage.show();

        // Load the initial content (hello-view.fxml)
        MainController mainController = SceneTransitionUtil.changeContent(
                "/com/example/upnext/hello-view.fxml",
            SceneTransitionUtil.TransitionType.FADE, 
            MainController.class
        );
        mainController.setStage(stage);
    }


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        // Ensure all resources are released when the application is stopped
        SceneTransitionUtil.shutdown();

        // Call the superclass implementation
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
