package com.example.harmony;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

        Font.loadFont(getClass().getResourceAsStream("/Feather.ttf"), 18);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Bold.otf"), 16);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Light.otf"), 24);

        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        stage.getIcons().add(appIcon);

        stage.initStyle(StageStyle.UNDECORATED);

        // IMPORTANT: absolute path
        FXMLLoader loader = new FXMLLoader(MainPage.class.getResource("/com/example/harmony/front-layout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 720);
        scene.setFill(javafx.scene.paint.Color.WHITE);

        scene.getStylesheets().add(Objects.requireNonNull(
                MainPage.class.getResource("/com/example/harmony/styles.css")
        ).toExternalForm());

        // Default light
        root.getStyleClass().add("light-mode");

        stage.setTitle("Harmony");
        stage.setScene(scene);
        stage.show();

        FrontLayoutController controller = loader.getController();
        if (controller != null) {
            controller.setStage(stage);
        }
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        SceneTransitionUtil.shutdown();
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
