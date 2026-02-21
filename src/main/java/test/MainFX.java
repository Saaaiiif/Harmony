package test;

import controllers.MainController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.fxml.FXMLLoader;
import utils.MyDatabase;

import java.util.Objects;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            MyDatabase.getInstance();

            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Feather.ttf"), 18);
            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/SF-Pro-Text-Bold.otf"), 16);
            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/SF-Pro-Text-Light.otf"), 24);
            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/SF-Pro-Display-Regular.otf"), 16);
            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/SFUIText-Regular.otf"), 16);

            try {
                Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/harmonie.png")));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("Could not load app icon: " + e.getMessage());
            }

            primaryStage.initStyle(StageStyle.UNDECORATED);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 700);
            scene.setFill(javafx.scene.paint.Color.WHITE);

            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/style.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/front-style.css")).toExternalForm());

            root.getStyleClass().add("light-mode");

            primaryStage.setTitle("Harmonie");
            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.centerOnScreen();

            MainController controller = loader.getController();
            if (controller != null) {
                controller.setStage(primaryStage);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

