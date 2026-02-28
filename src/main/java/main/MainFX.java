package main;

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

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("🚀 Démarrage de l'application Harmony...");


        loadCustomFonts();


        loadApplicationIcon(primaryStage);


        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Harmony - Authentification");

        try {
            loadLoginInterface(primaryStage);
            System.out.println("✅ Application Harmony démarrée avec succès !");
        } catch (IOException e) {
            System.err.println("❌ Erreur critique au démarrage : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void loadCustomFonts() {
        try {
            // Font Feather pour les icônes
            try {
                Font.loadFont(getClass().getResourceAsStream("/Feather.ttf"), 18);
                System.out.println("✓ Font Feather.ttf chargée");
            } catch (Exception e) {
                System.err.println("⚠️ Impossible de charger Feather.ttf : " + e.getMessage());
            }

            // Font SF Pro Text Bold
            try {
                Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Bold.otf"), 16);
                System.out.println("✓ Font SF-Pro-Text-Bold.otf chargée");
            } catch (Exception e) {
                System.err.println("⚠️ Impossible de charger SF-Pro-Text-Bold.otf : " + e.getMessage());
            }

            // Font SF Pro Text Light
            try {
                Font.loadFont(getClass().getResourceAsStream("/SF-Pro-Text-Light.otf"), 24);
                System.out.println("✓ Font SF-Pro-Text-Light.otf chargée");
            } catch (Exception e) {
                System.err.println("⚠️ Impossible de charger SF-Pro-Text-Light.otf : " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des fonts : " + e.getMessage());
        }
    }


    private void loadApplicationIcon(Stage primaryStage) {
        try {
            Image appIcon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/logo.png")));
            primaryStage.getIcons().add(appIcon);
            System.out.println("✓ Icône de l'application chargée");
        } catch (Exception e) {
            System.err.println("⚠️ Logo non trouvé : " + e.getMessage());
        }
    }


    private void loadLoginInterface(Stage primaryStage) throws IOException {
        // Charger le fichier FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserViews/Login.fxml"));
        Parent root = loader.load();

        // Créer la scène
        Scene scene = new Scene(root, 1150, 730);
        scene.setFill(javafx.scene.paint.Color.WHITE);

        // Charger et appliquer le CSS
        try {
            String cssResource = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssResource);
            System.out.println("✓ Stylesheet /styles.css chargée");
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger le CSS : " + e.getMessage());
        }

        // Appliquer le mode clair par défaut
        root.getStyleClass().add("light-mode");

        // Afficher la scène
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();

        System.out.println("✓ Interface de connexion chargée et affichée");
    }

    
    public static void main(String[] args) {
        launch(args);
    }
}