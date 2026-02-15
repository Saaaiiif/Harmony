package com.example.harmony;

import com.example.harmony.interfaces.ThemeAware;
import com.example.harmony.interfaces.WheelCyclable;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class FrontLayoutController implements ThemeAware, WheelCyclable {

    // Content view root (styling only)
    @FXML private BorderPane root;

    // Page content container (optional)
    @FXML private StackPane contentArea;

    // Nav wheel
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;

    private Stage stage;

    // Wheel constants
    private static final double SLOT_W = 60;
    private static final double SLOT_H = 60;
    private static final double RADIUS = 300;
    private static final double ARC_START_DEG = -30;
    private static final double ARC_END_DEG = 30;

    private static final double CENTER_Y_PADDING = 20;

    private static final int VISIBLE_COUNT = 9; // odd number

    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);
    private AnimationTimer rollTimer;
    private double rollTarget = 0;
    private long rollStartNs;
    private double rollFrom, rollTo;
    private final double rollDurationSec = 0.7;

    private int currentIndex = 0;
    private Pane wheelPane;

    private Integer pendingNavIndex = null;

    private static class NavItem {
        final String label;
        final String baseIconName;

        NavItem(String label, String baseIconName) {
            this.label = label;
            this.baseIconName = baseIconName;
        }
    }

    private final List<NavItem> navItems = List.of(
            new NavItem("Home", "homepage-6104"),
            new NavItem("Quick Search", "search-interface-symbol"),
            new NavItem("Settings", "settings-5666"),
            new NavItem("About Us", "information-6255"),
            new NavItem("Calendar", "calendar-11015"),
            new NavItem("Courses", "book-13427"),
            new NavItem("Health", "black-hospital-cross-10726"),
            new NavItem("Breathe", "cooling-symbol-3341"),
            new NavItem("Pomodoro", "time-2624"),
            new NavItem("Scenes", "photos-10614")
    );

    private static class WheelSlot {
        final VBox box;
        final ImageView icon;
        final Label label;
        int itemIndex;
        int offsetFromCenter;

        WheelSlot(VBox box, ImageView icon, Label label) {
            this.box = box;
            this.icon = icon;
            this.label = label;
        }
    }

    private final List<WheelSlot> slots = new ArrayList<>(VISIBLE_COUNT);
    private final Map<String, Image> iconCache = new HashMap<>();

    private boolean isDarkModeNow() {
        RootLayoutController rc = SceneTransitionUtil.getRootController();
        // If rootController isn't set yet, fall back to LIGHT (safe default)
        return rc != null && rc.isDarkMode();
    }

    private Image getIcon(String base, boolean dark) {
        String key = base + (dark ? "-dark" : "-light");
        return iconCache.computeIfAbsent(key, k ->
                new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/" + base + (dark ? "-dark.png" : "-light.png"))
                ))
        );
    }

    @FXML
    public void initialize() {
        buildNavWheel();

        Platform.runLater(() -> {
            // Sync wheel icons with current theme
            layoutSlots(currentIndex);

            installWheelHoverBehavior();

            // If theme changes later (via Root), you can call rebuildWheelIcons()
            // from Root after switching content. For now, this keeps behavior stable.
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        // No title-bar drag logic here anymore (RootLayout handles window drag).
    }

    private void buildNavWheel() {
        wheelPane = new Pane();
        wheelPane.setPickOnBounds(false);

        navWheelContainer.getChildren().clear();
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
    }

    private void installWheelHoverBehavior() {
        double hiddenY = 300;
        double moveUp = 35;

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
    @Override
    public void onThemeChanged() {
        layoutSlots(animatedCenter.get());
    }

    private void layoutSlots(double centerIndexFrac) {
        double w = navWheelContainer.getWidth();
        double h = navWheelContainer.getHeight();
        if (w <= 0 || h <= 0) return;

        boolean dark = isDarkModeNow();

        double centerX = w / 2.0;
        double centerY = h - CENTER_Y_PADDING - 10;

        int n = navItems.size();
        int half = VISIBLE_COUNT / 2;

        double startAngle = Math.toRadians(ARC_START_DEG);
        double endAngle = Math.toRadians(ARC_END_DEG);
        double stepAngle = (VISIBLE_COUNT <= 1) ? 0 : (endAngle - startAngle) / (VISIBLE_COUNT - 1);

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
            s.icon.setImage(getIcon(item.baseIconName, dark));
            s.label.setText(item.label);

            int dist = Math.abs(offset);
            double opacity = (dist == 0) ? 1.0 : Math.max(0.45, 1.0 - dist * 0.12);
            s.box.setOpacity(opacity);

            s.box.getStyleClass().remove("nav-wheel-button-active");
            if (offset == 0) s.box.getStyleClass().add("nav-wheel-button-active");

            s.box.relocate(x - SLOT_W / 2.0, y - SLOT_H / 2.0);
        }
    }

    private void onSlotClicked(WheelSlot s) {
        int step = s.offsetFromCenter;

        if (step == 0) {
            handleNavigation(s.itemIndex);
            return;
        }

        pendingNavIndex = s.itemIndex; // <-- remember what user wanted

        double from = animatedCenter.get();
        int base = (int) Math.floor(from);
        double to = base + step;

        startRoll(from, to);
    }


    private void startRoll(double from, double to) {
        rollFrom = from;
        rollTo = to;
        rollStartNs = System.nanoTime();

        if (rollTimer != null) rollTimer.stop();

        rollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
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

                    if (pendingNavIndex != null) {
                        int idx = pendingNavIndex;
                        pendingNavIndex = null;
                        handleNavigation(idx);
                    }
                }

            }
        };

        rollTimer.start();
    }

    private void handleNavigation(int index) {
        try {
            switch (index) {
                case 5: // Courses
                    SceneTransitionUtil.changeContent(
                            "/com/example/harmony/courses-layout.fxml",
                            SceneTransitionUtil.TransitionType.FADE,
                            CoursesLayoutController.class
                    );
                    break;

                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int mod(int a, int n) {
        int r = a % n;
        return (r < 0) ? (r + n) : r;
    }
    public void bindTheme(BooleanProperty darkMode) {
        darkMode.addListener((obs, oldV, newV) -> onThemeChanged());
    }

    @Override
    public void cycleWheel(int step) {
        rollWheelByStep(step);
    }

    private void rollWheelByStep(int step) {
        // start from wherever the animation currently is
        double from = animatedCenter.get();

        // compute the next slot relative to the nearest slot
        int base = (int) Math.round(from);
        double to = base + step;

        // if an animation is running, stop and retarget immediately
        if (rollTimer != null) rollTimer.stop();

        rollTarget = to;

        int n = navItems.size();
        int newCenterIndex = mod((int) Math.round(to), n);
        pendingNavIndex = newCenterIndex;

        startRoll(from, to);
    }
}
