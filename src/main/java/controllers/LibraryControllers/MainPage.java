package controllers.LibraryControllers;

import com.example.harmony.SessionManager;
import services.LibraryServices.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
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

        showLoginGate(stage);
    }

    private void showLoginGate(Stage stage) {
        UserService userService = new UserService();

        Label title = new Label("Who's using Harmony?");
        title.getStyleClass().add("section-title");

        ComboBox<UserService.UserRow> userPicker = new ComboBox<>();
        userPicker.setPromptText("Select user...");
        userPicker.setMaxWidth(Double.MAX_VALUE);
        userPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(UserService.UserRow u) {
                if (u == null) return "";
                return u.fullName() + " — " + u.email();
            }
            @Override public UserService.UserRow fromString(String s) { return null; }
        });

        try { userPicker.getItems().setAll(userService.listUsers()); } catch (Exception e) { e.printStackTrace(); }

        Button loginBtn = new Button("Continue");
        loginBtn.getStyleClass().add("action-button");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.disableProperty().bind(userPicker.valueProperty().isNull());

        loginBtn.setOnAction(e -> {
            UserService.UserRow u = userPicker.getValue();
            SessionManager.getInstance().login(new SessionManager.UserSession(
                    u.id(), u.nom(), u.prenom(), u.email(), u.imagePath(), u.typeUtilisateur()
            ));
            try { launchMainLayout(stage); } catch (IOException ex) { ex.printStackTrace(); }
        });

        Label orLabel = new Label("— or create new user —");
        orLabel.setStyle("-fx-opacity: 0.5; -fx-font-size: 12px;");

        TextField newNom = new TextField();
        newNom.setPromptText("Last name (Nom)");
        TextField newPrenom = new TextField();
        newPrenom.setPromptText("First name (Prénom)");
        TextField newEmail = new TextField();
        newEmail.setPromptText("Email");

        Button createBtn = new Button("Create & Continue");
        createBtn.getStyleClass().add("action-button");
        createBtn.setMaxWidth(Double.MAX_VALUE);

        createBtn.setOnAction(e -> {
            String nom = newNom.getText().trim();
            String prenom = newPrenom.getText().trim();
            String email = newEmail.getText().trim();
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) return;
            try {
                UserService.UserRow u = userService.createUser(nom, prenom, email);
                SessionManager.getInstance().login(new SessionManager.UserSession(
                        u.id(), u.nom(), u.prenom(), u.email(), u.imagePath(), u.typeUtilisateur()
                ));
                launchMainLayout(stage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        VBox box = new VBox(12,
                title,
                userPicker,
                loginBtn,
                orLabel,
                newNom,
                newPrenom,
                newEmail,
                createBtn
        );
        box.setPadding(new Insets(32));
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(400);
        box.getStyleClass().add("popup-root");
        box.getStyleClass().add("light-mode");

        Scene scene = new Scene(box, 400, 380);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/views/LibraryViews/styles.css")
        ).toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Harmony");
        stage.show();
    }

    private void launchMainLayout(Stage stage) throws IOException {
        FXMLLoader shellLoader = new FXMLLoader(
                MainPage.class.getResource("/views/LibraryViews/root-layout.fxml")
        );
        Parent shellRoot = shellLoader.load();

        Scene scene = new Scene(shellRoot, 1280, 720);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainPage.class.getResource("/views/LibraryViews/styles.css")
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

        stage.setScene(scene);
        stage.setTitle("Harmony");

        RootLayoutController shellController = shellLoader.getController();
        shellController.setStage(stage);
        SceneTransitionUtil.setRootController(shellController);

        FrontLayoutController frontController = SceneTransitionUtil.changeContent(
                "/views/LibraryViews/front-layout.fxml",
                SceneTransitionUtil.TransitionType.FADE,
                FrontLayoutController.class
        );
        if (frontController != null) frontController.setStage(stage);
    }

    public static void main(String[] args) { launch(); }

    @Override
    public void stop() {
        SceneTransitionUtil.shutdown();
        try { super.stop(); } catch (Exception e) { e.printStackTrace(); }
    }
}
