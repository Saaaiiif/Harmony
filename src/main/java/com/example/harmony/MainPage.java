package com.example.harmony;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

        FXMLLoader shellLoader = new FXMLLoader(
                MainPage.class.getResource("/com/example/harmony/root-layout.fxml")
        );
        Parent shellRoot = shellLoader.load();

        Scene scene = new Scene(shellRoot, 1280, 720);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainPage.class.getResource("/com/example/harmony/styles.css")
        ).toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.RIGHT) {
                e.consume();
                SceneTransitionUtil.cycleWheel(+1);
            } else if (e.isControlDown() && e.getCode() == KeyCode.LEFT) {
                e.consume();
                SceneTransitionUtil.cycleWheel(-1);
            }
        });


        shellRoot.getStyleClass().add("light-mode");

        stage.setTitle("Harmony");
        stage.setScene(scene);
        stage.show();

        RootLayoutController shellController = shellLoader.getController();
        shellController.setStage(stage);
        SceneTransitionUtil.setRootController(shellController);

        FrontLayoutController frontController = SceneTransitionUtil.changeContent(
                "/com/example/harmony/front-layout.fxml",
                SceneTransitionUtil.TransitionType.FADE,
                FrontLayoutController.class
        );
        if (frontController != null) {
            frontController.setStage(stage);
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
