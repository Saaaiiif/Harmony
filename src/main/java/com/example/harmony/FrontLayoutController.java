package com.example.harmony;

import controllers.Forum.BackOffice.ForumBackDashboardController;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;


import controllers.Forum.ForumHomeController;

import java.util.*;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


public class FrontLayoutController {

//    @FXML private StackPane wheelZone;
    @FXML private HBox titleBar;
    @FXML private ImageView logoImage;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private BorderPane root;
    @FXML private StackPane contentArea;
//    @FXML private StackPane navWheelContainer;

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    private boolean isDarkMode = false; // default LIGHT

    private static final double SLOT_W = 60;
    private static final double SLOT_H = 60;
    private static final double RADIUS = 300;
    private static final double ARC_START_DEG = -30;
    private static final double ARC_END_DEG = 30;
    private static final double ICON_HALF = 22;
    private static final double CENTER_Y_PADDING = 20;
    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);
    private Timeline rollAnim;
    private final List<NavItem> navItems = List.of(
            new NavItem("Home", "homepage-6104"),
            new NavItem("Forum", "forum"),
            new NavItem("Back Office", "settings-5666"),
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

    private AnimationTimer rollTimer;
    private long rollStartNs;
    private double rollFrom, rollTo;
    private double rollDurationSec = 0.7;

    private int currentIndex = 0;
    private Pane wheelPane;
    private static final int VISIBLE_COUNT = 9; // odd number
    private static class WheelSlot {
        final VBox box;
        final ImageView icon;
        final Label label;
        int itemIndex;
        int offsetFromCenter; // <-- add this

        WheelSlot(VBox box, ImageView icon, Label label) {
            this.box = box; this.icon = icon; this.label = label;
        }
    }
    private final List<WheelSlot> slots = new ArrayList<>(VISIBLE_COUNT);

    private final Map<String, Image> iconCache = new HashMap<>();

    private Image getIcon(String base, boolean dark) {
        String key = base + (dark ? "-dark" : "-light");
        return iconCache.computeIfAbsent(key, k -> new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/" + base + (dark ? "-dark.png" : "-light.png"))
        )));
    }


    public void setContent(Parent page){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }


    // 🔥 Méthode pour ouvrir les catégories
    public void openForumHome(){

        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Forum/ForumHome.fxml")
            );

            Parent page = loader.load();

            ForumHomeController controller = loader.getController();

            // 🔥 ON PASSE LE CONTROLLER PRINCIPAL
            controller.setFrontLayoutController(this);

            setContent(page);

        }catch(Exception e){
            e.printStackTrace();
        }
    }





    @FXML
    public void initialize() {
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
        logoImage.setImage(logo);

        themeToggleButton.setOnAction(e -> toggleTheme());

        minimizeButton.setOnAction(e -> {
            if (stage != null) stage.setIconified(true);
        });

        closeButton.setOnAction(e -> {
            SceneTransitionUtil.shutdown();
            Platform.exit();
        });


        updateThemeButtonText();
//        buildNavWheel();
        Platform.runLater(() -> {
            animatedCenter.set(currentIndex);
            //layoutSlots(animatedCenter.get());
            //installWheelHoverBehavior();
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

        // Apply initial theme to actual scene root
        applyTheme();
        //rebuildWheel();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateThemeButtonText();
        applyTheme();     // <-- change here
       // layoutSlots(animatedCenter.get());
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
                    p.getStyleClass().add("light-mode");
                }
            }
        }
    }

//    private void buildNavWheel() {
//        wheelPane = new Pane();
//        wheelPane.setPickOnBounds(false);
//        navWheelContainer.getChildren().add(wheelPane);
//
//        wheelPane.getChildren().clear();
//        slots.clear();
//
//        for (int i = 0; i < VISIBLE_COUNT; i++) {
//            ImageView iv = new ImageView();
//            iv.setFitWidth(32);
//            iv.setFitHeight(32);
//            iv.setPreserveRatio(true);
//            iv.getStyleClass().add("nav-wheel-icon");
//
//            Label lbl = new Label();
//            lbl.getStyleClass().add("nav-wheel-label");
//
//            VBox box = new VBox(iv, lbl);
//            box.getStyleClass().add("nav-wheel-item");
//
//            WheelSlot s = new WheelSlot(box, iv, lbl);
//            slots.add(s);
//            wheelPane.getChildren().add(box);
//
//            // click uses the current mapped itemIndex
//            box.setOnMouseClicked(e -> onSlotClicked(s));
//
//        }
//
//        navWheelContainer.widthProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));
//        navWheelContainer.heightProperty().addListener((obs, o, n) -> layoutSlots(animatedCenter.get()));
//
//        Platform.runLater(() -> layoutSlots(animatedCenter.get()));
//    }

//    private void rebuildWheel() {
//        layoutSlots(animatedCenter.get());
//    }



    private int mod(int a, int n) {
        int r = a % n;
        return (r < 0) ? (r + n) : r;
    }




    private static class NavItem {
        final String label;
        final String baseIconName;

        NavItem(String label, String baseIconName) {
            this.label = label;
            this.baseIconName = baseIconName;
        }
    }

//    private void installWheelHoverBehavior() {
//        double hiddenY = 300;
//        double moveUp  = 35;
//
//        wheelZone.setTranslateY(hiddenY);
//        wheelZone.setOpacity(0.18);   // low when hidden
//
//        TranslateTransition slideUp = new TranslateTransition(Duration.millis(180), wheelZone);
//        slideUp.setToY(hiddenY - moveUp);
//
//        TranslateTransition slideDown = new TranslateTransition(Duration.millis(180), wheelZone);
//        slideDown.setToY(hiddenY);
//
//        wheelZone.setOnMouseEntered(e -> {
//            wheelZone.setOpacity(1.0);   // fully visible on hover
//            slideDown.stop();
//            slideUp.playFromStart();
//        });
//
//        wheelZone.setOnMouseExited(e -> {
//            wheelZone.setOpacity(0.18);  // fade again
//            slideUp.stop();
//            slideDown.playFromStart();
//        });
//    }




//    private void layoutSlots(double centerIndexFrac) {
//        double w = navWheelContainer.getWidth();
//        double h = navWheelContainer.getHeight();
//        if (w <= 0 || h <= 0) return;
//
//        double centerX = w / 2.0;
//        double centerY = h - CENTER_Y_PADDING - 10;
//
//        int n = navItems.size();
//        int half = VISIBLE_COUNT / 2;
//
//        double startAngle = Math.toRadians(ARC_START_DEG);
//        double endAngle   = Math.toRadians(ARC_END_DEG);
//        double stepAngle  = (VISIBLE_COUNT <= 1) ? 0 : (endAngle - startAngle) / (VISIBLE_COUNT - 1);
//
//        int base = (int) Math.floor(centerIndexFrac);
//        double frac = centerIndexFrac - base;
//
//        for (int slot = 0; slot < VISIBLE_COUNT; slot++) {
//            WheelSlot s = slots.get(slot);
//
//            // Increasing centerIndexFrac moves the wheel left (anti-clockwise)
//            double t = slot - frac;
//
//            // Hide when out of arc range (prevents endpoint overlap)
//            if (t < 0 || t > (VISIBLE_COUNT - 1)) {
//                s.box.setVisible(false);
//                s.box.setMouseTransparent(true);
//                continue;
//            }
//            s.box.setVisible(true);
//            s.box.setMouseTransparent(false);
//
//            double angle = startAngle + t * stepAngle;
//
//            double x = centerX + RADIUS * Math.sin(angle);
//            double y = centerY - RADIUS * Math.cos(angle);
//
//            int offset = slot - half;
//            s.offsetFromCenter = offset;
//
//            int itemIndex = mod(base + offset, n);
//            NavItem item = navItems.get(itemIndex);
//            s.itemIndex = itemIndex;
//
//            s.icon.setImage(getIcon(item.baseIconName, isDarkMode));
//            s.label.setText(item.label);
//
//            // Opacity: centered one is strongest
//            int dist = Math.abs(offset);
//            double opacity = (dist == 0) ? 1.0 : Math.max(0.45, 1.0 - dist * 0.12);
//            s.box.setOpacity(opacity);
//
//            // Optional: apply your existing active class (for icon scale/background)
//            s.box.getStyleClass().remove("nav-wheel-button-active");
//            if (offset == 0) {
//                s.box.getStyleClass().add("nav-wheel-button-active");
//            }
//
//            s.box.relocate(x - SLOT_W / 2.0, y - SLOT_H / 2.0);
//        }
//    }





//    private void startRoll(double from, double to) {
//        rollFrom = from;
//        rollTo = to;
//        rollStartNs = System.nanoTime();
//
//        if (rollTimer != null) rollTimer.stop();
//        rollTimer = new AnimationTimer() {
//            @Override public void handle(long now) {
//                double t = (now - rollStartNs) / 1_000_000_000.0;
//                double p = Math.min(1.0, t / rollDurationSec);
//
//                // ease-out
//                double eased = 1 - Math.pow(1 - p, 3);
//
//                double v = rollFrom + (rollTo - rollFrom) * eased;
//                animatedCenter.set(v);
//                layoutSlots(v);
//
//                if (p >= 1.0) {
//                    stop();
//                    int n = navItems.size();
//                    currentIndex = mod((int) Math.round(rollTo), n);
//                    animatedCenter.set(currentIndex);
//                    layoutSlots(currentIndex);
//                }
//            }
//        };
//        rollTimer.start();
//    }

    private void onSlotClicked(WheelSlot s) {


        //---------------------------------------------forum-----------------------------------------------------
        NavItem clickedItem = navItems.get(s.itemIndex);

        if(clickedItem.label.equals("Forum")){
            System.out.println("Forum clicked !");
            // ici navigation vers page forum
            loadPage("/Forum/ForumHome.fxml");
        }

        if(clickedItem.label.equals("Back Office")){
            loadPage("/Forum/ForumBackOffice/ForumBackDashboard.fxml");
        }
        //---------------------------------------------------------------------------------------------------------


        int step = s.offsetFromCenter;   // rightmost = +4, leftmost = -4 (for VISIBLE_COUNT=9)
        if (step == 0) return;

        double from = animatedCenter.get();

        // IMPORTANT: layoutSlots() uses base = floor(centerIndexFrac) to decide what's centered,
        // so use the same base here to avoid off-by-one / "random" landing.
        int base = (int) Math.floor(from);

        // Roll to an exact integer index so the final snap is always correct.
        double to = base + step;

        //startRoll(from, to);



    }

    private void loadPage(String fxmlPath){
        try{

            System.out.println("Loading FXML: " + fxmlPath);

            var resource = getClass().getResource(fxmlPath);

            if(resource == null){
                System.out.println("❌ FXML NOT FOUND: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent page = loader.load();

            Object controller = loader.getController();

            if(controller instanceof controllers.Forum.ForumHomeController forumController){
                forumController.setFrontLayoutController(this);
            }

            if(controller instanceof ForumBackDashboardController backController){
                backController.setFrontLayoutController(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);

        }catch(Exception e){
            e.printStackTrace();
        }
    }


//----------------------------------------back----------------------
public void goHome(){
    contentArea.getChildren().clear();
}
    @FXML
    public void openBackOffice(){
        loadPage("/Forum/ForumBackOffice/ForumBackDashboard.fxml");
    }


}
