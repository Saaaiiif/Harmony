package com.example.harmony;

import com.example.harmony.interfaces.WheelCyclable;
import com.example.harmony.services.CourseService;
import com.example.harmony.interfaces.ThemeAware;
import com.example.harmony.util.UiPopups;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.collections.FXCollections;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import com.example.harmony.model.SubjectRow;
public class CoursesLayoutController implements ThemeAware, WheelCyclable {

    @FXML private BorderPane root;
    @FXML private TilePane coursesContainer;
    @FXML private Button addCourseButton;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;
    @FXML private Button libraryBtn;

    private Stage stage;
    private boolean isDarkMode = false;

    private final CourseService courseService = new CourseService();
    private static final double SLOT_W = 60;
    private static final double SLOT_H = 60;
    private static final double RADIUS = 300;
    private static final double ARC_START_DEG = -30;
    private static final double ARC_END_DEG = 30;
    private static final double CENTER_Y_PADDING = 20;

    private static final int VISIBLE_COUNT = 9;

    private Integer pendingNavIndex = null;

    private double rollTarget = 0;
    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);
    private AnimationTimer rollTimer;

    private long rollStartNs;
    private double rollFrom, rollTo;
    private final double rollDurationSec = 0.7;

    private int currentIndex = 5;
    private Pane wheelPane;

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
        addCourseButton.setOnAction(e -> addNewCourse());

        buildNavWheel();

        Platform.runLater(() -> {
            animatedCenter.set(currentIndex);
            layoutSlots(animatedCenter.get());
            installWheelHoverBehavior();
            loadCourses();
        });

        libraryBtn.setOnAction(e -> {
            try {
                SceneTransitionUtil.changeContent(
                        "/com/example/harmony/library-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        LibraryLayoutController.class
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });



    }
    private boolean isDarkModeNow() {
        RootLayoutController rc = SceneTransitionUtil.getRootController();
        return rc != null && rc.isDarkMode();
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void addNewCourse() {
        Stage owner = getOwnerStage();
        if (owner == null) return;

        Label header = new Label("Add course");
        header.getStyleClass().add("section-title");

        TextField courseNameField = new TextField();
        courseNameField.setPromptText("Course name");

        ComboBox<SubjectRow> subjectBox = new ComboBox<>();
        subjectBox.setEditable(true);
        subjectBox.setPromptText("Subject");

        ListView<File> filesList = new ListView<>();
        filesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filesList.setPrefHeight(130);

        Button pickFilesBtn = new Button("Add files...");
        pickFilesBtn.getStyleClass().add("action-button");

        Button createBtn = new Button("Create");
        createBtn.getStyleClass().add("action-button");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");

        ObservableList<SubjectRow> allSubjects;
        try {
            allSubjects = FXCollections.observableArrayList(courseService.listSubjects());
        } catch (Exception ex) {
            ex.printStackTrace();
            allSubjects = FXCollections.observableArrayList();
        }
        final ObservableList<SubjectRow> allSubjectsFinal = allSubjects;

        FilteredList<SubjectRow> filtered = new FilteredList<>(allSubjectsFinal, s -> true);
        subjectBox.setItems(filtered);

        subjectBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(SubjectRow s) {
                return (s == null) ? "" : s.name();
            }

            @Override
            public SubjectRow fromString(String text) {
                if (text == null) return null;
                String q = text.trim();
                if (q.isEmpty()) return null;

                for (SubjectRow s : allSubjectsFinal) {
                    if (s.name().equalsIgnoreCase(q)) return s;
                }
                return null;
            }
        });

        subjectBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String q = (newText == null) ? "" : newText.trim().toLowerCase();
            SubjectRow sel = subjectBox.getValue();
            if (sel != null && sel.name().equalsIgnoreCase(newText)) return;
            filtered.setPredicate(s -> q.isEmpty() || s.name().toLowerCase().contains(q));
            if (!subjectBox.isShowing()) subjectBox.show();
        });

        subjectBox.getEditor().setOnAction(e -> {
            String t = subjectBox.getEditor().getText();
            if (t == null) return;
            String q = t.trim();
            if (q.isEmpty()) return;

            for (SubjectRow s : allSubjectsFinal) {
                if (s.name().equalsIgnoreCase(q)) {
                    subjectBox.setValue(s);
                    return;
                }
            }
            subjectBox.setValue(null);
        });

        BooleanBinding titleInvalid = Bindings.createBooleanBinding(
                () -> courseNameField.getText() == null || courseNameField.getText().trim().isEmpty(),
                courseNameField.textProperty()
        );

        BooleanBinding subjectInvalid = Bindings.createBooleanBinding(
                () -> subjectBox.getEditor().getText() == null || subjectBox.getEditor().getText().trim().isEmpty(),
                subjectBox.getEditor().textProperty()
        );

        createBtn.disableProperty().bind(titleInvalid.or(subjectInvalid));

        HBox buttons = new HBox(10, createBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(
                10,
                header,
                new Label("Course name"),
                courseNameField,
                new Label("Subject"),
                subjectBox,
                pickFilesBtn,
                new Label("Files"),
                filesList,
                buttons
        );
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.CENTER_LEFT);

        Stage popup = UiPopups.buildModalNoTitleBar(owner, box, 460, 560, false, isDarkModeNow(), getClass());
        popup.setTitle("Add course");

        pickFilesBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select files for this course");
            List<File> picked = fc.showOpenMultipleDialog(popup);
            if (picked != null && !picked.isEmpty()) filesList.getItems().addAll(picked);
        });

        cancelBtn.setOnAction(e -> popup.close());

        createBtn.setOnAction(e -> {
            String title = (courseNameField.getText() == null) ? "" : courseNameField.getText().trim();
            if (title.isEmpty()) {
                UiPopups.showWarning(popup, "Course name is required.", isDarkModeNow(), getClass());
                courseNameField.requestFocus();
                return;
            }

            String subjectText = subjectBox.getEditor().getText() == null ? "" : subjectBox.getEditor().getText().trim();
            if (subjectText.isEmpty()) {
                UiPopups.showWarning(popup, "Subject is required.", isDarkModeNow(), getClass());
                subjectBox.requestFocus();
                return;
            }

            try {
                CourseService.CreateCourseRequest req =
                        new CourseService.CreateCourseRequest(
                                title,
                                subjectText,
                                null,
                                new ArrayList<>(filesList.getItems())
                        );

                courseService.createCourse(req);

                popup.close();
                loadCourses();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Failed to create course.", isDarkModeNow(), getClass());
            }
        });

        popup.showAndWait();
    }


    private Stage getOwnerStage() {
        if (stage != null) return stage;
        if (addCourseButton != null && addCourseButton.getScene() != null) {
            return (Stage) addCourseButton.getScene().getWindow();
        }
        return null;
    }





    private void loadCourses() {
        if (coursesContainer == null) return;

        coursesContainer.getChildren().clear();

        try (Connection conn = DB.getConnection()) {
            if (conn == null) {
                coursesContainer.getChildren().add(makeEmptyState("No database connection"));
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement("""
        SELECT c.id,
               c.title,
               s.name AS subject_name
        FROM courses c
        LEFT JOIN subject s ON s.id = c.subjectid
        """);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String subjectName = rs.getString("subject_name");
                    coursesContainer.getChildren().add(makeCourseCard(id, title, subjectName));
                }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            coursesContainer.getChildren().add(makeEmptyState("Failed to load courses"));
        }
    }

    private StackPane makeCourseCard(int courseId, String title, String subjectName) {

        Label lbl = new Label(title);
        lbl.getStyleClass().add("course-title");

        VBox cardBody = new VBox(10, lbl);
        cardBody.setPadding(new Insets(16));
        cardBody.setAlignment(Pos.CENTER_LEFT);
        cardBody.getStyleClass().add("course-card");

        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("course-delete-btn");

        StackPane wrapper = new StackPane(cardBody, deleteBtn);
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(deleteBtn, new Insets(6, 6, 0, 0));

        deleteBtn.visibleProperty().bind(wrapper.hoverProperty());
        deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());

        deleteBtn.setOnAction(e -> {
            e.consume();
            confirmAndDeleteCourse(courseId, title);
        });
        wrapper.setOnMouseClicked(e -> {
            if (e.getTarget() == deleteBtn) return;
            openCourse(courseId, title, subjectName);
        });
        return wrapper;
    }

    private void openCourse(int courseId, String title, String subjectName) {
        try {
            CourseDetailsController c = SceneTransitionUtil.changeContent(
                    "/com/example/harmony/course-details.fxml",
                    SceneTransitionUtil.TransitionType.FADE,
                    CourseDetailsController.class
            );
            c.setCourse(courseId, title, subjectName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Open course: " + title + " subject=" + subjectName);

    }


    private void confirmAndDeleteCourse(int courseId, String title) {
        Stage owner = getOwnerStage();

        boolean ok = UiPopups.confirm(
                owner,
                "Delete \"" + title + "\"?\n\nThis will remove the course and all its files.",
                isDarkModeNow(),
                getClass()
        );
        if (!ok) return;

        try {
            courseService.deleteCourse(courseId);
            loadCourses();
        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(owner, "Failed to delete course.", isDarkModeNow(), getClass());
        }
    }







    private VBox makeEmptyState(String message) {
        Label lbl = new Label(message);
        lbl.getStyleClass().add("empty-state-label");

        VBox box = new VBox(lbl);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
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

    private void layoutSlots(double centerIndexFrac) {
        double w = navWheelContainer.getWidth();
        double h = navWheelContainer.getHeight();
        if (w <= 0 || h <= 0) return;

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
            s.icon.setImage(getIcon(item.baseIconName, isDarkModeNow()));
            s.label.setText(item.label);

            int dist = Math.abs(offset);
            double opacity = (dist == 0) ? 1.0 : Math.max(0.45, 1.0 - dist * 0.12);
            s.box.setOpacity(opacity);

            s.box.getStyleClass().remove("nav-wheel-button-active");
            if (offset == 0) s.box.getStyleClass().add("nav-wheel-button-active");

            s.box.relocate(x - SLOT_W / 2.0, y - SLOT_H / 2.0);
        }
    }

    @Override
    public void onThemeChanged() {
        System.out.println("Library onThemeChanged called");
        layoutSlots(animatedCenter.get());
    }


    private void onSlotClicked(WheelSlot s) {
        int step = s.offsetFromCenter;

        if (step == 0) {
            handleNavigation(s.itemIndex);
            return;
        }

        pendingNavIndex = s.itemIndex;

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
                case 5:
                    break;

                case 0:
                    SceneTransitionUtil.changeContent(
                            "/com/example/harmony/front-layout.fxml",
                            SceneTransitionUtil.TransitionType.FADE,
                            FrontLayoutController.class
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
    @Override
    public void cycleWheel(int step) {
        rollWheelByStep(step);
    }

    private void rollWheelByStep(int step) {
        double from = animatedCenter.get();
        int base = (int) Math.round(from);
        double to = base + step;
        if (rollTimer != null) rollTimer.stop();

        rollTarget = to;

        int n = navItems.size();
        int newCenterIndex = mod((int) Math.round(to), n);
        pendingNavIndex = newCenterIndex;

        startRoll(from, to);
    }




}
