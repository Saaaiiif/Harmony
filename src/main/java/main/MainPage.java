package main;

import controllers.FrontLayoutController;
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
        // Chargement des polices à la racine de resources/
        Font.loadFont(getClass().getResourceAsStream("/Feather.ttf"), 18);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Bold.otf"), 16);
        Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Light.otf"), 24);

        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        stage.getIcons().add(appIcon);
        stage.initStyle(StageStyle.UNDECORATED);

        // Chargement de l'interface principale (chemin direct)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/front-layout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 720);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        root.getStyleClass().add("light-mode"); // Mode clair par défaut

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
}