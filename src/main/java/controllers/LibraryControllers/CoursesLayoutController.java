package controllers.LibraryControllers;

import com.example.harmony.DB;
import com.example.harmony.SessionManager;
import controllers.UserControlleers.modifControl.AccueilController;
import interfaces.WheelCyclable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import models.UserModels.Session;
import services.LibraryServices.CourseService;
import interfaces.ThemeAware;
import services.LibraryServices.ImageGenerationService;
import utils.UiPopups;
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
import models.LibraryModels.SubjectRow;
public class CoursesLayoutController{

    @FXML private BorderPane root;
    @FXML private TilePane coursesContainer;
    @FXML private Button addCourseButton;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;
    @FXML private Button libraryBtn;
    @FXML private ImageView coursesHeaderIcon;
    @FXML private Button gridViewBtn;
    @FXML private Button listViewBtn;
    @FXML private ScrollPane coursesScrollPane;
    private final VBox listContainer = new VBox(8);


    private boolean isGridView = true;

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
    private AccueilController accueilController;
    private double rollTarget = 0;
    private final DoubleProperty animatedCenter = new SimpleDoubleProperty(0);
    private AnimationTimer rollTimer;

    private long rollStartNs;
    private double rollFrom, rollTo;
    private final double rollDurationSec = 0.7;

    private int currentIndex = 5;
    private Pane wheelPane;
    private final ImageGenerationService imageGenerationService = new ImageGenerationService();



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
        root.getStyleClass().add("light-mode");
        addCourseButton.setOnAction(e -> addNewCourse());



        Platform.runLater(() -> {
            animatedCenter.set(currentIndex);


            loadCourses();
            updateHeaderIcon();
        });

        libraryBtn.setOnAction(e -> {
            if (accueilController != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/views/LibraryViews/library-layout.fxml")
                    );
                    Parent view = loader.load();
                    LibraryLayoutController ctrl = loader.getController();
                    ctrl.setAccueilController(accueilController); // ← ADD THIS LINE
                    accueilController.setContent(view);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });


        gridViewBtn.setOnAction(e -> setViewMode(true));
        listViewBtn.setOnAction(e -> setViewMode(false));
        gridViewBtn.getStyleClass().add("view-toggle-btn-active"); // default


    }
    public void setAccueilController(AccueilController accueilController) {
        this.accueilController = accueilController;
    }
    private void setViewMode(boolean grid) {
        isGridView = grid;
        gridViewBtn.getStyleClass().remove("view-toggle-btn-active");
        listViewBtn.getStyleClass().remove("view-toggle-btn-active");
        if (grid) gridViewBtn.getStyleClass().add("view-toggle-btn-active");
        else {
            listContainer.setPadding(new Insets(24));
            listContainer.getChildren().clear();
        }
        if (grid) coursesScrollPane.setContent(coursesContainer);
        else      coursesScrollPane.setContent(listContainer);
        loadCourses();
    }
    private HBox makeCourseListItem(int courseId, String title, String subjectName, String coverImagePath, boolean isSaved) {
        StackPane thumb = new StackPane();
        thumb.setMinSize(56, 56);
        thumb.setPrefSize(56, 56);
        thumb.setMaxSize(56, 56);
        thumb.setStyle("-fx-background-radius: 8;");

        if (coverImagePath != null && !coverImagePath.isBlank()) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get("C:/wamp64/www/covers/" + coverImagePath);
                if (p.toFile().exists()) {
                    ImageView iv = new ImageView(new Image(p.toUri().toString(), 56, 56, false, true));
                    iv.setFitWidth(56);
                    iv.setFitHeight(56);
                    iv.setPreserveRatio(false);
                    thumb.getChildren().add(iv);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (thumb.getChildren().isEmpty()) {
            thumb.getStyleClass().add("course-card-image-placeholder");
            Label icon = new Label("🎓");
            icon.setStyle("-fx-font-size: 22px;");
            thumb.getChildren().add(icon);
        }

        Label titleLbl = new Label(title != null ? title : "");
        titleLbl.getStyleClass().add("course-title");
        titleLbl.setStyle("-fx-font-size: 15px;");

        Label subLbl = new Label(subjectName != null ? subjectName : "");
        subLbl.getStyleClass().add("course-subtitle");

        VBox info = new VBox(4, titleLbl, subLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row;

        if (isSaved) {
            Label savedBadge = new Label("🔖 In Library");
            savedBadge.getStyleClass().add("saves-badge");
            row = new HBox(14, thumb, info, spacer, savedBadge);
        } else {
            Button deleteBtn = new Button("✕");
            deleteBtn.getStyleClass().add("course-delete-btn");
            deleteBtn.setOnAction(e -> {
                e.consume();
                confirmAndDeleteCourse(courseId, title);
            });
            row = new HBox(14, thumb, info, spacer, deleteBtn);
        }

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("course-list-item");
        row.setPrefWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                openCourse(courseId, title, subjectName);
            }
        });
        if (!isSaved) {
            row.setOnContextMenuRequested(e ->
                    showCourseContextMenu(row, courseId, title, subjectName, e.getScreenX(), e.getScreenY())
            );
        }
        return row;
    }

    private void refreshCourses() {
        loadCourses();
    }


    private void updateHeaderIcon() {
        boolean dark = isDarkModeNow();
        String path = "/book-13427-" + (dark ? "dark" : "light") + ".png";
        try {
            coursesHeaderIcon.setImage(new Image(
                    getClass().getResourceAsStream(path)
            ));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean isDarkModeNow() {
        return false;
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

        final File[] selectedImage = {null};

        Button pickImageBtn = new Button("Choose cover image…");
        pickImageBtn.getStyleClass().add("action-button");

        Label imageNameLabel = new Label("No image selected");
        imageNameLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");

        ToggleButton autoGenToggle = new ToggleButton("✨ Auto generate");
        autoGenToggle.getStyleClass().add("auto-gen-toggle");


        pickImageBtn.disableProperty().bind(autoGenToggle.selectedProperty());

        autoGenToggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            if (isOn) imageNameLabel.setText("Will be generated on create");
            else imageNameLabel.setText("No image selected");
        });

        HBox imageRow = new HBox(10, pickImageBtn, autoGenToggle);
        imageRow.setAlignment(Pos.CENTER_LEFT);

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
            public String toString(SubjectRow s) { return (s == null) ? "" : s.name(); }
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
                if (s.name().equalsIgnoreCase(q)) { subjectBox.setValue(s); return; }
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

        Label courseNameLabel = new Label("Course name");
        courseNameLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
        Label subjectLabel = new Label("Subject");
        subjectLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
        Label coverImageLabel = new Label("Cover image");
        coverImageLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
        Label filesLabel = new Label("Files");
        filesLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");

        VBox box = new VBox(
                10,
                header,
                courseNameLabel,
                courseNameField,
                subjectLabel,
                subjectBox,
                coverImageLabel,
                imageRow,
                imageNameLabel,
                pickFilesBtn,
                filesLabel,
                filesList,
                buttons
        );
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.CENTER_LEFT);

        Stage popup = UiPopups.buildModalNoTitleBar(owner, box, 460, 640, false, isDarkModeNow(), getClass());
        popup.setTitle("Add course");

        pickImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select cover image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );
            File f = fc.showOpenDialog(popup);
            if (f != null) {
                selectedImage[0] = f;
                imageNameLabel.setText(f.getName());
                autoGenToggle.setSelected(false);
            }
        });

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

            createBtn.disableProperty().unbind();   // ← unbind before manually setting
            createBtn.setDisable(true);
            imageNameLabel.setText("Generating image…");

            Thread worker = new Thread(() -> {
                File coverFile = selectedImage[0];

                if (autoGenToggle.isSelected() && coverFile == null) {
                    try {
                        String prompt = "Minimalist modern logo for a course called \"" + title +
                                "\" about " + subjectText + ", flat design, clean, professional, no text";
                        byte[] imageBytes = imageGenerationService.generateCourseImage(title, subjectText);
                        if (imageBytes != null) {
                            java.nio.file.Path coversDir = java.nio.file.Paths.get("C:/wamp64/www/covers");
                            java.nio.file.Files.createDirectories(coversDir);
                            String filename = "cover_gen_" + System.currentTimeMillis() + ".png";
                            java.nio.file.Path dest = coversDir.resolve(filename);
                            java.nio.file.Files.write(dest, imageBytes);
                            coverFile = dest.toFile();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                final File finalCover = coverFile;
                Platform.runLater(() -> {
                    try {
                        CourseService.CreateCourseRequest req = new CourseService.CreateCourseRequest(
                                title,
                                subjectText,
                                Session.getInstance().getUser().getUser_id(),   // ← logged-in user
                                new ArrayList<>(filesList.getItems()),
                                finalCover
                        );
                        courseService.createCourse(req);
                        popup.close();
                        loadCourses();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        createBtn.disableProperty().bind(titleInvalid.or(subjectInvalid)); // ← re-bind on failure
                        imageNameLabel.setText("Generation failed, try again.");
                        UiPopups.showError(popup, "Failed to create course.", isDarkModeNow(), getClass());
                    }
                });
            });

            worker.setDaemon(true);
            worker.start();
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
        listContainer.getChildren().clear();

        int currentUserId = Session.getInstance().getUser().getUser_id();

        try (Connection conn = DB.getConnection()) {
            if (conn == null) {
                coursesContainer.getChildren().add(makeEmptyState("No database connection"));
                return;
            }

            // own courses + saved courses merged, no duplicates
            String sql = """
            SELECT c.id, c.title, s.name AS subject_name, c.cover_image_path,
                   CASE WHEN c.userid = ? THEN 0 ELSE 1 END AS is_saved
            FROM courses c
            LEFT JOIN subject s ON s.id = c.subjectid
            WHERE c.userid = ?
               OR c.id IN (SELECT course_id FROM saved_courses WHERE user_id = ?)
            ORDER BY is_saved ASC, c.id DESC
        """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, currentUserId);
                ps.setInt(3, currentUserId);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean hasAny = false;
                    while (rs.next()) {
                        hasAny = true;
                        int id = rs.getInt("id");
                        String title = rs.getString("title");
                        String subjectName = rs.getString("subject_name");
                        String coverImagePath = rs.getString("cover_image_path");
                        boolean isSaved = rs.getInt("is_saved") == 1;

                        if (isGridView) {
                            coursesContainer.getChildren().add(
                                    makeCourseCard(id, title, subjectName, coverImagePath, isSaved)
                            );
                        } else {
                            listContainer.getChildren().add(
                                    makeCourseListItem(id, title, subjectName, coverImagePath, isSaved)
                            );
                        }
                    }

                    if (!hasAny) {
                        if (isGridView) coursesContainer.getChildren().add(makeEmptyState("No courses yet"));
                        else listContainer.getChildren().add(makeEmptyState("No courses yet"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            coursesContainer.getChildren().add(makeEmptyState("Failed to load courses"));
        }
    }

    private StackPane makeCourseCard(int courseId, String title, String subjectName, String coverImagePath, boolean isSaved) {
        VBox cardBody = new VBox(0);
        cardBody.getStyleClass().add("course-card");
        cardBody.setAlignment(Pos.TOP_CENTER);

        if (coverImagePath != null && !coverImagePath.isBlank()) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get("C:/wamp64/www/covers/" + coverImagePath);
                if (p.toFile().exists()) {
                    ImageView iv = new ImageView(new Image(p.toUri().toString(), 260, 150, false, true));
                    iv.setFitWidth(260);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);
                    iv.getStyleClass().add("course-card-image");
                    cardBody.getChildren().add(iv);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (cardBody.getChildren().isEmpty()) {
            StackPane placeholder = new StackPane();
            placeholder.getStyleClass().add("course-card-image-placeholder");
            placeholder.setPrefSize(260, 150);
            Label icon = new Label("🎓");
            icon.setStyle("-fx-font-size: 36px;");
            placeholder.getChildren().add(icon);
            cardBody.getChildren().add(placeholder);
        }

        VBox info = new VBox(4);
        info.setPadding(new Insets(12, 16, 12, 16));

        Label lbl = new Label(title);
        lbl.getStyleClass().add("course-title");
        info.getChildren().add(lbl);

        if (subjectName != null && !subjectName.trim().isEmpty()) {
            Label sub = new Label(subjectName.trim());
            sub.getStyleClass().add("course-subtitle");
            info.getChildren().add(sub);
        }

        cardBody.getChildren().add(info);

        StackPane wrapper = new StackPane(cardBody);

        if (isSaved) {
            // saved courses from library: show badge, no delete button
            Label savedBadge = new Label("🔖 In Library");
            savedBadge.getStyleClass().add("saves-badge");
            StackPane.setAlignment(savedBadge, Pos.TOP_LEFT);
            StackPane.setMargin(savedBadge, new Insets(6, 0, 0, 8));
            wrapper.getChildren().add(savedBadge);
        } else {
            // own courses: show delete button
            Button deleteBtn = new Button("✕");
            deleteBtn.getStyleClass().add("course-delete-btn");
            StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(deleteBtn, new Insets(6, 6, 0, 0));
            deleteBtn.visibleProperty().bind(wrapper.hoverProperty());
            deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());
            deleteBtn.setOnAction(e -> {
                e.consume();
                confirmAndDeleteCourse(courseId, title);
            });
            wrapper.getChildren().add(deleteBtn);
        }

        wrapper.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                openCourse(courseId, title, subjectName);
            }
        });
        if (!isSaved) {
            wrapper.setOnContextMenuRequested(e ->
                    showCourseContextMenu(wrapper, courseId, title, subjectName, e.getScreenX(), e.getScreenY())
            );
        }

        return wrapper;
    }


    private void openCourse(int courseId, String title, String subjectName) {
        if (accueilController == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/LibraryViews/course-details.fxml")
            );
            Parent view = loader.load();
            CourseDetailsController ctrl = loader.getController();
            ctrl.setAccueilController(accueilController);
            ctrl.setOrigin(CourseDetailsController.Origin.COURSES);
            ctrl.setCourse(courseId, title, subjectName);
            accueilController.setContent(view);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Open course: " + title + " subject: " + subjectName);
        }
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
            // clean up saved_courses first to avoid orphan rows
            try (Connection conn = DB.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM saved_courses WHERE course_id = ?")) {
                ps.setInt(1, courseId);
                ps.executeUpdate();
            }
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









    private void handleNavigation(int index) {
        try {
            switch (index) {
                case 5:
                    break;

                case 0:
                    SceneTransitionUtil.changeContent(
                            "/views/LibraryViews/front-layout.fxml",
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

    // ─────────────────────────────────────────────────────────────────────────
    // Right-click context menu for owned courses
    // ─────────────────────────────────────────────────────────────────────────

    private void showCourseContextMenu(javafx.scene.Node anchor, int courseId,
                                       String title, String subjectName,
                                       double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("course-context-menu");

        MenuItem renameItem   = new MenuItem("✏  Rename");
        MenuItem regenItem    = new MenuItem("🎨  Regenerate picture");
        MenuItem changeImgItem = new MenuItem("🖼  Change picture");
        MenuItem subjectItem  = new MenuItem("📂  Change subject");
        MenuItem deleteItem   = new MenuItem("🗑  Delete");

        renameItem.getStyleClass().add("context-menu-item");
        regenItem.getStyleClass().add("context-menu-item");
        changeImgItem.getStyleClass().add("context-menu-item");
        subjectItem.getStyleClass().add("context-menu-item");
        deleteItem.getStyleClass().add("context-menu-item-danger");

        renameItem.setOnAction(e    -> renameCourse(courseId, title));
        regenItem.setOnAction(e     -> regenerateCoverImage(courseId, title, subjectName));
        changeImgItem.setOnAction(e -> changeCoverImage(courseId));
        subjectItem.setOnAction(e   -> changeCourseSubject(courseId, title, subjectName));
        deleteItem.setOnAction(e    -> confirmAndDeleteCourse(courseId, title));

        menu.getItems().addAll(renameItem, regenItem, changeImgItem, subjectItem,
                new SeparatorMenuItem(), deleteItem);

        // Apply app stylesheet — must be done via the skin's scene after show
        menu.setOnShown(e -> {
            try {
                javafx.scene.Scene menuScene = menu.getSkin().getNode().getScene();
                if (menuScene != null) {
                    String css = getClass().getResource("/views/LibraryViews/styles.css").toExternalForm();
                    if (!menuScene.getStylesheets().contains(css))
                        menuScene.getStylesheets().add(css);
                }
            } catch (Exception ignored) {}
        });

        menu.show(anchor, screenX, screenY);
    }

    private void renameCourse(int courseId, String currentTitle) {
        Stage owner = getOwnerStage();
        UiPopups.prompt(owner, "Rename Course", "New name:", currentTitle, isDarkModeNow(), getClass())
                .ifPresent(newTitle -> {
                    String t = newTitle.trim();
                    if (t.isEmpty()) return;
                    try (Connection conn = DB.getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "UPDATE courses SET title = ? WHERE id = ?")) {
                        ps.setString(1, t);
                        ps.setInt(2, courseId);
                        ps.executeUpdate();
                        loadCourses();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        UiPopups.showError(owner, "Failed to rename course.", isDarkModeNow(), getClass());
                    }
                });
    }

    private void regenerateCoverImage(int courseId, String title, String subjectName) {
        Stage owner = getOwnerStage();
        boolean ok = UiPopups.confirm(owner,
                "Regenerate the cover image for \"" + title + "\"?",
                isDarkModeNow(), getClass());
        if (!ok) return;

        Thread worker = new Thread(() -> {
            try {
                byte[] imageBytes = imageGenerationService.generateCourseImage(title, subjectName);
                if (imageBytes == null) throw new Exception("No image returned");

                java.nio.file.Path coversDir = java.nio.file.Paths.get("C:/wamp64/www/covers");
                java.nio.file.Files.createDirectories(coversDir);
                String filename = "cover_gen_" + System.currentTimeMillis() + ".png";
                java.nio.file.Path dest = coversDir.resolve(filename);
                java.nio.file.Files.write(dest, imageBytes);

                try (Connection conn = DB.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE courses SET cover_image_path = ? WHERE id = ?")) {
                    ps.setString(1, filename);
                    ps.setInt(2, courseId);
                    ps.executeUpdate();
                }
                Platform.runLater(this::loadCourses);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        UiPopups.showError(owner, "Failed to regenerate image.", isDarkModeNow(), getClass())
                );
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void changeCoverImage(int courseId) {
        Stage owner = getOwnerStage();
        FileChooser fc = new FileChooser();
        fc.setTitle("Select cover image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fc.showOpenDialog(owner);
        if (file == null) return;

        try {
            java.nio.file.Path coversDir = java.nio.file.Paths.get("C:/wamp64/www/covers");
            java.nio.file.Files.createDirectories(coversDir);
            String filename = "cover_" + System.currentTimeMillis()
                    + file.getName().substring(file.getName().lastIndexOf('.'));
            java.nio.file.Files.copy(file.toPath(), coversDir.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            try (Connection conn = DB.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE courses SET cover_image_path = ? WHERE id = ?")) {
                ps.setString(1, filename);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
            loadCourses();
        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(owner, "Failed to update cover image.", isDarkModeNow(), getClass());
        }
    }

    private void changeCourseSubject(int courseId, String title, String currentSubjectName) {
        Stage owner = getOwnerStage();

        ObservableList<SubjectRow> allSubjects;
        try {
            allSubjects = FXCollections.observableArrayList(courseService.listSubjects());
        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(owner, "Failed to load subjects.", isDarkModeNow(), getClass());
            return;
        }

        Label header = new Label("Change subject for \"" + title + "\"");
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px;");

        Label subjectLabel = new Label("Subject");
        subjectLabel.getStyleClass().add("recommended-title");
        subjectLabel.setStyle("-fx-font-size: 13px;");

        // TextField + ListView avoids ComboBox popup positioning bugs inside modals
        TextField searchField = new TextField();
        searchField.setPromptText("Search subjects\u2026");
        searchField.getStyleClass().add("search-field");

        FilteredList<SubjectRow> filtered = new FilteredList<>(allSubjects, s -> true);
        ListView<SubjectRow> listView = new ListView<>(filtered);
        listView.setPrefHeight(160);
        listView.getStyleClass().add("subject-list-view");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SubjectRow s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.name());
            }
        });

        // Pre-select current subject
        allSubjects.stream()
                .filter(s -> s.name().equalsIgnoreCase(currentSubjectName))
                .findFirst()
                .ifPresent(s -> {
                    listView.getSelectionModel().select(s);
                    listView.scrollTo(s);
                });

        searchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase();
            filtered.setPredicate(s -> q.isEmpty() || s.name().toLowerCase().contains(q));
        });

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("action-button");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(10, header, subjectLabel, searchField, listView, buttons);
        box.setPadding(new Insets(18));

        Stage popup = UiPopups.buildModalNoTitleBar(owner, box, 380, 360, false, isDarkModeNow(), getClass());

        cancelBtn.setOnAction(e -> popup.close());
        saveBtn.setOnAction(e -> {
            SubjectRow selected = listView.getSelectionModel().getSelectedItem();
            String typed = searchField.getText() == null ? "" : searchField.getText().trim();

            // If nothing selected but user typed something, resolve or create the subject
            if (selected == null && !typed.isEmpty()) {
                boolean exists = allSubjects.stream().anyMatch(s -> s.name().equalsIgnoreCase(typed));
                if (!exists) {
                    boolean ok = UiPopups.confirm(popup,
                            "Subject \"" + typed + "\" doesn't exist. Create it?",
                            isDarkModeNow(), getClass());
                    if (!ok) return;
                    try (Connection conn = DB.getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO subject (name) VALUES (?)",
                                 java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, typed);
                        ps.executeUpdate();
                        try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) selected = new SubjectRow(keys.getInt(1), typed);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        UiPopups.showError(popup, "Failed to create subject.", isDarkModeNow(), getClass());
                        return;
                    }
                } else {
                    selected = allSubjects.stream()
                            .filter(s -> s.name().equalsIgnoreCase(typed))
                            .findFirst().orElse(null);
                }
            }

            if (selected == null) {
                UiPopups.showWarning(popup, "Please select or enter a subject.", isDarkModeNow(), getClass());
                return;
            }

            final int subjectId = selected.id();
            try (Connection conn = DB.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE courses SET subjectid = ? WHERE id = ?")) {
                ps.setInt(1, subjectId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
                popup.close();
                loadCourses();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Failed to change subject.", isDarkModeNow(), getClass());
            }
        });

        // Double-click on list item saves immediately
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null)
                saveBtn.fire();
        });

        popup.showAndWait();
    }







}