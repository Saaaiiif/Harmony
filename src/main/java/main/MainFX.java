package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        primaryStage.setTitle("Harmony - Connexion");
        primaryStage.setScene(new Scene(root, 1000, 650)); // Taille adaptée au style
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}