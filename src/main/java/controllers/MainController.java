package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.IOException;
import java.util.*;

public class MainController {

    @FXML private StackPane wheelZone;
    @FXML private HBox titleBar;
    @FXML private ImageView logoImage;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private BorderPane root;
    @FXML private StackPane contentArea;
    @FXML private StackPane navWheelContainer;

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    private boolean isDarkMode = false;

    private static final double SLOT_W = 60;
    private static final double SLOT_H = 60;
    private static final double RADIUS = 300;
    private static final double ARC_START_DEG = -30;
    private static final double ARC_END_DEG = 30;
    private static final double CENTER_Y_PADDING = 20;
    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);

    private final List<NavItem> navItems = List.of(
            new NavItem("Accueil", "homepage-6104", null),
            new NavItem("Admin", "settings-5666", "/admin/AdminDashboard.fxml"),
            new NavItem("Étudiant", "book-13427", "/etudiant/EtudiantDashboard.fxml"),
            new NavItem("Méditation", "cooling-symbol-3341", "/admin/AdminMeditation.fxml"),
            new NavItem("Journal", "calendar-11015", "/admin/AdminJournal.fxml"),
            new NavItem("Mon Journal", "photos-10614", "/etudiant/EtudiantJournal.fxml"),
            new NavItem("Mes Méditations", "play-button-4213", "/etudiant/EtudiantMeditation.fxml"),
            new NavItem("Santé", "black-hospital-cross-10726", null),
            new NavItem("Info", "information-6255", null)
    );

    private AnimationTimer rollTimer;
    private long rollStartNs;
    private double rollFrom, rollTo;
    private double rollDurationSec = 0.7;

    private int currentIndex = 0;
    private Pane wheelPane;
    private static final int VISIBLE_COUNT = 9;

    private static class WheelSlot {
        final VBox box;
        final ImageView icon;
        final Label label;
        int itemIndex;
        int offsetFromCenter;

        WheelSlot(VBox box, ImageView icon, Label label) {
            this.box = box; this.icon = icon; this.label = label;
        }
    }
    private final List<WheelSlot> slots = new ArrayList<>(VISIBLE_COUNT);

    private final Map<String, Image> iconCache = new HashMap<>();

    private Image getIcon(String base, boolean dark) {
        String key = base + (dark ? "-dark" : "-light");
        return iconCache.computeIfAbsent(key, k -> new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/" + base + (dark ? "-dark.png" : "-light.png"))
        )));
    }

    private static class NavItem {
        final String label;
        final String baseIconName;
        final String fxmlPath;

        NavItem(String label, String baseIconName, String fxmlPath) {
            this.label = label;
            this.baseIconName = baseIconName;
            this.fxmlPath = fxmlPath;
        }
    }

    @FXML
    public void initialize() {
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/logo.png")));
        logoImage.setImage(logo);

        themeToggleButton.setOnAction(e -> toggleTheme());

        minimizeButton.setOnAction(e -> {
            if (stage != null) stage.setIconified(true);
        });

        closeButton.setOnAction(e -> Platform.exit());

        updateThemeButtonText();
        buildNavWheel();
        Platform.runLater(() -> {
            animatedCenter.set(currentIndex);
            layoutSlots(animatedCenter.get());
            installWheelHoverBehavior();
            showWelcomeContent();
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            if (this.stage != null) {
                this.stage.setX(event.getScreenX() - xOffset);
                this.stage.setY(event.getScreenY() - yOffset);
            }
        });

        applyTheme();
        layoutSlots(animatedCenter.get());
    }

    private void showWelcomeContent() {
        VBox welcome = new VBox(25);
        welcome.setAlignment(javafx.geometry.Pos.CENTER);
        welcome.setStyle("-fx-padding: 60;");

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/logo.png"))));
        logo.setFitWidth(100);
        logo.setFitHeight(100);
        logo.setPreserveRatio(true);

        StackPane logoWrap = new StackPane(logo);
        logoWrap.setMaxWidth(120);
        logoWrap.setMaxHeight(120);
        logoWrap.setStyle("-fx-border-color: rgba(205,157,241,0.4); -fx-border-width: 2; " +
                "-fx-border-radius: 60; -fx-padding: 10; -fx-alignment: center;");

        Label title = new Label("Harmonie");
        title.setStyle("-fx-font-family: 'SF Pro Display'; -fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #cd9df1;");

        Label subtitle = new Label("Votre espace de bien-être mental");
        subtitle.setStyle("-fx-font-family: 'SF Pro Text'; -fx-font-size: 18px; -fx-text-fill: rgba(150,150,150,0.9);");

        Region separator = new Region();
        separator.setMaxWidth(80);
        separator.setMinHeight(2);
        separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #cd9df1, transparent); -fx-background-radius: 1;");

        Label hint = new Label("Survolez la barre en bas pour naviguer");
        hint.setStyle("-fx-font-family: 'SF Pro Text'; -fx-font-size: 13px; -fx-text-fill: rgba(140,140,140,0.7); -fx-font-style: italic;");

        welcome.getChildren().addAll(logoWrap, title, subtitle, separator, hint);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(welcome);
    }

    private void openInNewWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("Harmonie - " + title);
            Scene scene = new Scene(content, 1100, 700);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/style.css")).toExternalForm());
            newStage.setScene(scene);

            try {
                newStage.getIcons().add(new Image(
                        Objects.requireNonNull(getClass().getResourceAsStream("/assets/harmonie.png"))));
            } catch (Exception e) {
                System.err.println("Could not load icon: " + e.getMessage());
            }

            newStage.show();
            newStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading: " + fxmlPath);
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateThemeButtonText();
        applyTheme();
        layoutSlots(animatedCenter.get());
    }

    private void updateThemeButtonText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(isDarkMode ? "☀" : "☾");
        }
    }

    private void applyTheme() {
        if (root == null) return;

        if (isDarkMode) {
            root.getStyleClass().remove("light-mode");
            if (!contentArea.getChildren().isEmpty()) {
                Node currentContent = contentArea.getChildren().get(0);
                if (currentContent instanceof Parent p) {
                    p.getStyleClass().remove("light-mode");
                }
            }
        } else {
            if (!root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().add("light-mode");
            }
            if (!contentArea.getChildren().isEmpty()) {
                Node currentContent = contentArea.getChildren().get(0);
                if (currentContent instanceof Parent p) {
                    if (!p.getStyleClass().contains("light-mode")) {
                        p.getStyleClass().add("light-mode");
                    }
                }
            }
        }
    }

    private void buildNavWheel() {
        wheelPane = new Pane();
        wheelPane.setPickOnBounds(false);
        navWheelContainer.getChildren().add(wheelPane);

        wheelPane.getChildren().clear();
        slots.clear();

        for (int i = 0; i < VISIBLE_COUNT; i++) {
            ImageView iv = new ImageView();
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("nav-wheel-icon");

            Label lbl = new Label();
            lbl.getStyleClass().add("nav-wheel-label");

            VBox box = new VBox(iv, lbl);
            box.getStyleClass().add("nav-wheel-item");

            WheelSlot s = new WheelSlot(box, iv, lbl);
            slots.add(s);
            wheelPane.getChildren().add(box);

            box.setOnMouseClicked(e -> onSlotClicked(s));
        }

        navWheelContainer.widthProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));
        navWheelContainer.heightProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));

        Platform.runLater(() -> layoutSlots(animatedCenter.get()));
    }

    private void installWheelHoverBehavior() {
        double hiddenY = 300;
        double moveUp  = 35;

        wheelZone.setTranslateY(hiddenY);
        wheelZone.setOpacity(0.18);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(180), wheelZone);
        slideUp.setToY(hiddenY - moveUp);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(180), wheelZone);
        slideDown.setToY(hiddenY);

        wheelZone.setOnMouseEntered(e -> {
            wheelZone.setOpacity(1.0);
            slideDown.stop();
            slideUp.playFromStart();
        });

        wheelZone.setOnMouseExited(e -> {
            wheelZone.setOpacity(0.18);
            slideUp.stop();
            slideDown.playFromStart();
        });
    }

    private int mod(int a, int n) {
        int r = a % n;
        return (r < 0) ? (r + n) : r;
    }

    private void layoutSlots(double centerIndexFrac) {
        double w = navWheelContainer.getWidth();
        double h = navWheelContainer.getHeight();
        if (w <= 0 || h <= 0) return;

        double centerX = w / 2.0;
        double centerY = h - CENTER_Y_PADDING - 10;

        int n = navItems.size();
        int half = VISIBLE_COUNT / 2;

        double startAngle = Math.toRadians(ARC_START_DEG);
        double endAngle   = Math.toRadians(ARC_END_DEG);
        double stepAngle  = (VISIBLE_COUNT <= 1) ? 0 : (endAngle - startAngle) / (VISIBLE_COUNT - 1);

        int base = (int) Math.floor(centerIndexFrac);
        double frac = centerIndexFrac - base;

        for (int slot = 0; slot < VISIBLE_COUNT; slot++) {
            WheelSlot s = slots.get(slot);

            double t = slot - frac;

            if (t < 0 || t > (VISIBLE_COUNT - 1)) {
                s.box.setVisible(false);
                s.box.setMouseTransparent(true);
                continue;
            }
            s.box.setVisible(true);
            s.box.setMouseTransparent(false);

            double angle = startAngle + t * stepAngle;

            double x = centerX + RADIUS * Math.sin(angle);
            double y = centerY - RADIUS * Math.cos(angle);

            int offset = slot - half;
            s.offsetFromCenter = offset;

            int itemIndex = mod(base + offset, n);
            NavItem item = navItems.get(itemIndex);
            s.itemIndex = itemIndex;

            s.icon.setImage(getIcon(item.baseIconName, isDarkMode));
            s.label.setText(item.label);

            int dist = Math.abs(offset);
            double opacity = (dist == 0) ? 1.0 : Math.max(0.45, 1.0 - dist * 0.12);
            s.box.setOpacity(opacity);

            s.box.getStyleClass().remove("nav-wheel-button-active");
            if (offset == 0) {
                s.box.getStyleClass().add("nav-wheel-button-active");
            }

            s.box.relocate(x - SLOT_W / 2.0, y - SLOT_H / 2.0);
        }
    }

    private void startRoll(double from, double to) {
        rollFrom = from;
        rollTo = to;
        rollStartNs = System.nanoTime();

        if (rollTimer != null) rollTimer.stop();
        rollTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                double t = (now - rollStartNs) / 1_000_000_000.0;
                double p = Math.min(1.0, t / rollDurationSec);

                double eased = 1 - Math.pow(1 - p, 3);

                double v = rollFrom + (rollTo - rollFrom) * eased;
                animatedCenter.set(v);
                layoutSlots(v);

                if (p >= 1.0) {
                    stop();
                    int n = navItems.size();
                    currentIndex = mod((int) Math.round(rollTo), n);
                    animatedCenter.set(currentIndex);
                    layoutSlots(currentIndex);
                }
            }
        };
        rollTimer.start();
    }

    private void onSlotClicked(WheelSlot s) {
        int step = s.offsetFromCenter;
        if (step == 0) {
            NavItem item = navItems.get(s.itemIndex);
            if (item.fxmlPath != null) {
                openInNewWindow(item.fxmlPath, item.label);
            }
            return;
        }

        double from = animatedCenter.get();
        int base = (int) Math.floor(from);
        double to = base + step;
        startRoll(from, to);
    }
}
