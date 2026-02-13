package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class FrontLayoutController {

    public static FrontLayoutController instance;

    @FXML private StackPane wheelZone;
    @FXML private HBox titleBar;
    @FXML private ImageView logoImage;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private BorderPane root;
    @FXML private StackPane contentArea; // C'est ici que tes pages s'affichent !
    @FXML private StackPane navWheelContainer;

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isDarkMode = false;

    private static final double SLOT_W = 60, SLOT_H = 60, RADIUS = 300;
    private static final double ARC_START_DEG = -30, ARC_END_DEG = 30;
    private static final double CENTER_Y_PADDING = 20;
    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);

    // 🔥 Ta liste d'icônes dans la roue.
    // J'ai mis "haltere" comme base. Ajoute "haltere-light.png" et "haltere-dark.png" dans resources.
    private final List<NavItem> navItems = List.of(
            new NavItem("Activité", "haltere", "/AccueilActivite.fxml"),
            new NavItem("Gestion 2", "search-interface-symbol", null),
            new NavItem("Gestion 3", "settings-5666", null),
            new NavItem("Gestion 4", "calendar-11015", null),
            new NavItem("Gestion 5", "time-2624", null),
            new NavItem("Gestion 6", "photos-10614", null)
    );

    private AnimationTimer rollTimer;
    private long rollStartNs;
    private double rollFrom, rollTo;
    private double rollDurationSec = 0.7;
    private int currentIndex = 0;
    private Pane wheelPane;
    private static final int VISIBLE_COUNT = 5;

    private static class WheelSlot {
        final VBox box; final ImageView icon; final Label label;
        int itemIndex; int offsetFromCenter;
        WheelSlot(VBox box, ImageView icon, Label label) { this.box = box; this.icon = icon; this.label = label; }
    }

    private final List<WheelSlot> slots = new ArrayList<>(VISIBLE_COUNT);
    private final Map<String, Image> iconCache = new HashMap<>();

    public FrontLayoutController() {
        instance = this;
    }

    @FXML
    public void initialize() {
        // Ajoute un try-catch pour le logo au cas où il manque
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo non trouvé, on continue sans.");
        }

        themeToggleButton.setOnAction(e -> toggleTheme());
        minimizeButton.setOnAction(e -> { if (stage != null) stage.setIconified(true); });
        closeButton.setOnAction(e -> Platform.exit());

        buildNavWheel();
        Platform.runLater(() -> {
            animatedCenter.set(currentIndex);
            layoutSlots(animatedCenter.get());
            installWheelHoverBehavior();

            // Charge la page d'Activité au démarrage
            loadPage(navItems.get(0).fxmlPath);
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
        titleBar.setOnMouseDragged(event -> {
            if (this.stage != null) {
                this.stage.setX(event.getScreenX() - xOffset);
                this.stage.setY(event.getScreenY() - yOffset);
            }
        });
        applyTheme();
    }

    // 🔥 LA MÉTHODE MAGIQUE POUR CHANGER LE MILIEU SANS CASSER LA ROUE
    public void loadPage(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isEmpty()) {
            System.out.println("Page en construction...");
            return;
        }
        try {
            Parent newPage = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(newPage);
            applyTheme();
        } catch (IOException e) {
            System.err.println("❌ Erreur de chargement de la page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeToggleButton.setText(isDarkMode ? "☀" : "☾");
        applyTheme();
        layoutSlots(animatedCenter.get());
    }

    private void applyTheme() {
        if (root == null) return;
        if (isDarkMode) {
            root.getStyleClass().remove("light-mode");
            if (!contentArea.getChildren().isEmpty()) {
                contentArea.getChildren().get(0).getStyleClass().remove("light-mode");
            }
        } else {
            if (!root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().add("light-mode");
            }
            if (!contentArea.getChildren().isEmpty()) {
                contentArea.getChildren().get(0).getStyleClass().add("light-mode");
            }
        }
    }

    private Image getIcon(String base, boolean dark) {
        try {
            String path = "/" + base + (dark ? "-dark.png" : "-light.png");
            return iconCache.computeIfAbsent(path, k -> new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
        } catch (Exception e) {
            return null; // Retourne null si l'image n'est pas encore créée
        }
    }

    private void buildNavWheel() {
        wheelPane = new Pane(); wheelPane.setPickOnBounds(false); navWheelContainer.getChildren().add(wheelPane);
        for (int i = 0; i < VISIBLE_COUNT; i++) {
            ImageView iv = new ImageView(); iv.setFitWidth(32); iv.setFitHeight(32);
            Label lbl = new Label(); lbl.getStyleClass().add("nav-wheel-label");
            VBox box = new VBox(iv, lbl); box.getStyleClass().add("nav-wheel-item");
            WheelSlot s = new WheelSlot(box, iv, lbl);
            slots.add(s); wheelPane.getChildren().add(box);
            box.setOnMouseClicked(e -> onSlotClicked(s));
        }
        navWheelContainer.widthProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));
        navWheelContainer.heightProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));
    }

    private void layoutSlots(double centerIndexFrac) {
        double centerX = navWheelContainer.getWidth() / 2.0;
        double centerY = navWheelContainer.getHeight() - CENTER_Y_PADDING - 10;
        int n = navItems.size(); int half = VISIBLE_COUNT / 2;
        double stepAngle = (VISIBLE_COUNT <= 1) ? 0 : (Math.toRadians(ARC_END_DEG) - Math.toRadians(ARC_START_DEG)) / (VISIBLE_COUNT - 1);
        int base = (int) Math.floor(centerIndexFrac); double frac = centerIndexFrac - base;

        for (int slot = 0; slot < VISIBLE_COUNT; slot++) {
            WheelSlot s = slots.get(slot);
            double t = slot - frac;
            if (t < 0 || t > (VISIBLE_COUNT - 1)) { s.box.setVisible(false); continue; }
            s.box.setVisible(true);
            double angle = Math.toRadians(ARC_START_DEG) + t * stepAngle;
            int offset = slot - half;
            s.offsetFromCenter = offset;
            int itemIndex = mod(base + offset, n);
            s.itemIndex = itemIndex;

            NavItem item = navItems.get(itemIndex);
            Image img = getIcon(item.baseIconName, isDarkMode);
            if(img != null) s.icon.setImage(img);
            s.label.setText(item.label);
            s.box.relocate(centerX + RADIUS * Math.sin(angle) - SLOT_W / 2.0, centerY - RADIUS * Math.cos(angle) - SLOT_H / 2.0);

            s.box.getStyleClass().remove("nav-wheel-button-active");
            if (offset == 0) s.box.getStyleClass().add("nav-wheel-button-active");
            s.box.setOpacity((Math.abs(offset) == 0) ? 1.0 : Math.max(0.45, 1.0 - Math.abs(offset) * 0.12));
        }
    }

    private void startRoll(double from, double to) {
        rollFrom = from; rollTo = to; rollStartNs = System.nanoTime();
        if (rollTimer != null) rollTimer.stop();
        rollTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                double p = Math.min(1.0, ((now - rollStartNs) / 1_000_000_000.0) / rollDurationSec);
                double v = rollFrom + (rollTo - rollFrom) * (1 - Math.pow(1 - p, 3));
                animatedCenter.set(v); layoutSlots(v);
                if (p >= 1.0) {
                    stop();
                    currentIndex = mod((int) Math.round(rollTo), navItems.size());
                    animatedCenter.set(currentIndex); layoutSlots(currentIndex);

                    loadPage(navItems.get(currentIndex).fxmlPath);
                }
            }
        };
        rollTimer.start();
    }

    private void onSlotClicked(WheelSlot s) {
        if (s.offsetFromCenter == 0) return;
        startRoll(animatedCenter.get(), (int) Math.floor(animatedCenter.get()) + s.offsetFromCenter);
    }

    private int mod(int a, int n) { int r = a % n; return (r < 0) ? (r + n) : r; }

    private void installWheelHoverBehavior() {
        double hiddenY = 300; double moveUp  = 35;
        wheelZone.setTranslateY(hiddenY); wheelZone.setOpacity(0.18);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(180), wheelZone); slideUp.setToY(hiddenY - moveUp);
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(180), wheelZone); slideDown.setToY(hiddenY);

        wheelZone.setOnMouseEntered(e -> { wheelZone.setOpacity(1.0); slideDown.stop(); slideUp.playFromStart(); });
        wheelZone.setOnMouseExited(e -> { wheelZone.setOpacity(0.18); slideUp.stop(); slideDown.playFromStart(); });
    }

    private static class NavItem {
        final String label; final String baseIconName; final String fxmlPath;
        NavItem(String label, String baseIconName, String fxmlPath) {
            this.label = label; this.baseIconName = baseIconName; this.fxmlPath = fxmlPath;
        }
    }
}