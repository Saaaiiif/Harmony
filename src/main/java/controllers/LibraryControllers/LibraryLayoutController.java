package controllers.LibraryControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import interfaces.ThemeAware;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import models.UserModels.Session;
import services.LibraryServices.LibraryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;

public class LibraryLayoutController implements ThemeAware {

    @FXML private Button myCoursesBtn;
    @FXML private TilePane publishedCoursesContainer;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;
    @FXML private BorderPane root;

    // Recommended section
    @FXML private VBox       recommendedSection;
    @FXML private HBox       recommendedContainer;   // horizontal strip — cards added here
    @FXML private Label      recommendedTitle;
    @FXML private ScrollPane recommendedScrollPane;



    // All-courses swappable wrapper
    @FXML private VBox coursesWrapper;          // setViewMode() swaps its single child

    // Filters / controls
    @FXML private ComboBox<String> sortFilter;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ImageView        libraryHeaderIcon;
    @FXML private Button           gridViewBtn;
    @FXML private Button           listViewBtn;
    @FXML private ScrollPane       libraryScrollPane;

    /** List-view container built in code — added to coursesWrapper when in list mode */
    private final VBox listContainer = new VBox(8);

    private boolean muteSubjectEvents = false;
    private boolean applyQueued       = false;
    private boolean isGridView        = true;
    private AccueilController accueilController;

    private final LibraryService libraryService = new LibraryService();

    // ─────────────────────────────────────────────────────────────────────────
    // initialize
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        root.getStyleClass().add("light-mode");

        myCoursesBtn.setOnAction(e -> {
            if (accueilController != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/views/LibraryViews/courses-layout.fxml"));
                    Parent view = loader.load();
                    CoursesLayoutController ctrl = loader.getController();
                    ctrl.setAccueilController(accueilController);
                    accueilController.setContent(view);
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        });

        gridViewBtn.setOnAction(e -> setViewMode(true));
        listViewBtn.setOnAction(e -> setViewMode(false));
        gridViewBtn.getStyleClass().add("view-toggle-btn-active");

        Platform.runLater(() -> {
            sortFilter.setItems(FXCollections.observableArrayList("Newest", "Most Saved", "Relevant"));
            sortFilter.setValue("Newest");
            sortFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
            loadSubjectFilter();
            loadPublishedCourses();
            loadRecommendedCourses();
            updateHeaderIcon();

            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) applyFilters();
            });
        });
    }

    public void setAccueilController(AccueilController accueilController) {
        this.accueilController = accueilController;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommended courses — horizontal sliding strip
    // ─────────────────────────────────────────────────────────────────────────

    private void loadRecommendedCourses() {
        try {
            int userId = Session.getInstance().getUser().getUser_id();
            var rows = libraryService.getRecommendedCourses(userId, 20);

            recommendedContainer.getChildren().clear();

            if (rows.isEmpty()) {
                recommendedSection.setVisible(false);
                recommendedSection.setManaged(false);
                return;
            }

            // Determine label: personalised vs popular fallback
            boolean hasSaves = libraryService.userHasSavedCourses(userId);
            recommendedTitle.setText(hasSaves ? "Recommended for You" : "Popular Courses");

            for (var r : rows) {
                recommendedContainer.getChildren().add(
                        makeRecommendedCard(r.id(), r.title(), r.subjectName(), r.coverImagePath(), r.saves())
                );
            }

            recommendedSection.setVisible(true);
            recommendedSection.setManaged(true);

            // Set up edge fades once the section is visible and laid out


            // Redirect vertical mouse-wheel events to horizontal scrolling on the strip
            recommendedScrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
                if (e.getDeltaY() != 0) {
                    double delta = -e.getDeltaY() * 3;
                    double newVal = recommendedScrollPane.getHvalue()
                            + delta / (recommendedContainer.getWidth() - recommendedScrollPane.getViewportBounds().getWidth());
                    recommendedScrollPane.setHvalue(Math.max(0, Math.min(1, newVal)));
                    e.consume();
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            recommendedSection.setVisible(false);
            recommendedSection.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View mode — swaps child of coursesWrapper, never touches recommended strip
    // ─────────────────────────────────────────────────────────────────────────

    private void setViewMode(boolean grid) {
        isGridView = grid;
        gridViewBtn.getStyleClass().remove("view-toggle-btn-active");
        listViewBtn.getStyleClass().remove("view-toggle-btn-active");
        if (grid) gridViewBtn.getStyleClass().add("view-toggle-btn-active");
        else      listViewBtn.getStyleClass().add("view-toggle-btn-active");

        coursesWrapper.getChildren().clear();
        if (grid) {
            coursesWrapper.getChildren().add(publishedCoursesContainer);
        } else {
            listContainer.setPadding(new Insets(8, 24, 24, 24));
            coursesWrapper.getChildren().add(listContainer);
        }
        applyFilters();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subject filter
    // ─────────────────────────────────────────────────────────────────────────

    private void queueApplyFilters() {
        if (applyQueued) return;
        applyQueued = true;
        Platform.runLater(() -> { applyQueued = false; applyFilters(); });
    }

    private void loadSubjectFilter() {
        try {
            var subjects = libraryService.listPublishedSubjects();

            ObservableList<String> allItems = FXCollections.observableArrayList(subjects);
            javafx.collections.transformation.FilteredList<String> filtered =
                    new javafx.collections.transformation.FilteredList<>(allItems, s -> true);

            subjectFilter.setItems(filtered);
            subjectFilter.setEditable(true);
            subjectFilter.setPromptText("All Subjects");

            subjectFilter.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(String s) { return s == null ? "" : s; }
                @Override public String fromString(String s) {
                    if (s == null || s.isBlank()) return null;
                    String typed = s.trim();
                    for (String item : allItems) {
                        if (item.equalsIgnoreCase(typed)) return item;
                    }
                    return null;
                }
            });

            subjectFilter.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            });

            subjectFilter.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item);
                }
            });

            subjectFilter.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (muteSubjectEvents) return;
                String q = (newVal == null) ? "" : newVal.trim().toLowerCase();
                filtered.setPredicate(s -> q.isEmpty() || s.toLowerCase().contains(q));
                if (!subjectFilter.isShowing() && !q.isEmpty()) subjectFilter.show();
                if (q.isEmpty()) {
                    muteSubjectEvents = true;
                    subjectFilter.setValue(null);
                    muteSubjectEvents = false;
                    queueApplyFilters();
                }
            });

            subjectFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (muteSubjectEvents) return;
                muteSubjectEvents = true;
                subjectFilter.getEditor().setText(newVal == null ? "" : newVal);
                muteSubjectEvents = false;
                queueApplyFilters();
            });

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtering / loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPublishedCourses() {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText();
        String subject = subjectFilter.getValue();

        if (publishedCoursesContainer == null) return;
        publishedCoursesContainer.getChildren().clear();
        listContainer.getChildren().clear();

        try {
            String sort = sortFilter != null ? sortFilter.getValue() : "Newest";
            var rows = libraryService.searchPublishedCourses(keyword, subject, sort);

            if (rows.isEmpty()) {
                VBox empty = makeEmptyState("No courses match your search.");
                if (isGridView) publishedCoursesContainer.getChildren().add(empty);
                else            listContainer.getChildren().add(empty);
                return;
            }

            for (var r : rows) {
                if (isGridView) {
                    publishedCoursesContainer.getChildren().add(
                            makePublishedCourseCard(r.id(), r.title(), r.subjectName(), r.coverImagePath(), r.saves()));
                } else {
                    listContainer.getChildren().add(
                            makePublishedCourseListItem(r.id(), r.title(), r.subjectName(), r.coverImagePath(), r.saves()));
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Overlays left and right LinearGradient fades on the recommended strip.
     * The gradient color matches the page background so it blends seamlessly.
     * Fades update their opacity as the user scrolls so:
     *   - left  fade is invisible at HValue=0  (already at start)
     *   - right fade is invisible at HValue=1  (already at end)
     */

    /** Smaller card used exclusively in the horizontal recommended strip. */
    private StackPane makeRecommendedCard(int courseId, String title, String subjectName, String coverImagePath, int saves) {
        final double W = 160, H = 95, RADIUS = 14;

        VBox cardBody = new VBox(0);
        cardBody.getStyleClass().add("course-card");
        cardBody.setAlignment(Pos.TOP_CENTER);
        cardBody.setMinWidth(W);
        cardBody.setPrefWidth(W);
        cardBody.setMaxWidth(W);

        Image img = loadCoverImage(coverImagePath);
        if (img != null) {
            // Rectangle with ImagePattern gives true rounded corners on the image itself —
            // ImageView.setClip() is ignored when CSS redraws the node, so this is the
            // only reliable approach in JavaFX.
            javafx.scene.shape.Rectangle imgRect = new javafx.scene.shape.Rectangle(W, H);
            imgRect.setArcWidth(RADIUS * 2);
            imgRect.setArcHeight(RADIUS * 2);
            imgRect.setFill(new javafx.scene.paint.ImagePattern(img));
            // Clip the bottom corners back to straight by wrapping in a StackPane
            // whose bottom half hides the arc — we extend the rect height slightly.
            javafx.scene.shape.Rectangle imgRectFull = new javafx.scene.shape.Rectangle(W, H + RADIUS);
            imgRectFull.setArcWidth(RADIUS * 2);
            imgRectFull.setArcHeight(RADIUS * 2);
            imgRectFull.setFill(new javafx.scene.paint.ImagePattern(img));
            StackPane imageHolder = new StackPane(imgRectFull);
            imageHolder.setMinSize(W, H);
            imageHolder.setPrefSize(W, H);
            imageHolder.setMaxSize(W, H);
            imageHolder.setClip(new javafx.scene.shape.Rectangle(W, H));
            cardBody.getChildren().add(imageHolder);
        } else {
            StackPane placeholder = new StackPane();
            placeholder.getStyleClass().add("course-card-image-placeholder");
            placeholder.setPrefSize(W, H);
            Label icon = new Label("🎓");
            icon.setStyle("-fx-font-size: 24px;");
            placeholder.getChildren().add(icon);
            cardBody.getChildren().add(placeholder);
        }

        VBox info = new VBox(2);
        info.setPadding(new Insets(8, 10, 8, 10));

        Label lbl = new Label(title != null ? title : "");
        lbl.getStyleClass().add("course-title");
        lbl.setStyle("-fx-font-size: 12px;");
        lbl.setWrapText(false);
        lbl.setMaxWidth(W - 20);
        lbl.setEllipsisString("…");
        info.getChildren().add(lbl);

        if (subjectName != null && !subjectName.trim().isEmpty()) {
            Label sub = new Label(subjectName.trim());
            sub.getStyleClass().add("course-subtitle");
            sub.setStyle("-fx-font-size: 10px;");
            info.getChildren().add(sub);
        }
        cardBody.getChildren().add(info);

        Label savesBadge = new Label("🔖 " + saves);
        savesBadge.getStyleClass().add("saves-badge");
        savesBadge.setStyle("-fx-font-size: 9px; -fx-padding: 2 6 2 6;");
        StackPane.setAlignment(savesBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(savesBadge, new Insets(0, 6, 6, 0));

        StackPane wrapper = new StackPane(cardBody, savesBadge);
        wrapper.setOnMouseClicked(e -> openCourse(courseId, title, subjectName));
        return wrapper;
    }

    private StackPane makePublishedCourseCard(int courseId, String title, String subjectName, String coverImagePath, int saves) {
        VBox cardBody = new VBox(0);
        cardBody.getStyleClass().add("course-card");
        cardBody.setAlignment(Pos.TOP_CENTER);

        Image img = loadCoverImage(coverImagePath);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(260);
            iv.setFitHeight(150);
            iv.setPreserveRatio(false);
            iv.getStyleClass().add("course-card-image");
            cardBody.getChildren().add(iv);
        } else {
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

        Label lbl = new Label(title != null ? title : "");
        lbl.getStyleClass().add("course-title");
        info.getChildren().add(lbl);

        if (subjectName != null && !subjectName.trim().isEmpty()) {
            Label sub = new Label(subjectName.trim());
            sub.getStyleClass().add("course-subtitle");
            info.getChildren().add(sub);
        }

        cardBody.getChildren().add(info);

        Label savesBadge = new Label("🔖 " + saves);
        savesBadge.getStyleClass().add("saves-badge");
        StackPane.setAlignment(savesBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(savesBadge, new Insets(0, 10, 10, 0));

        StackPane wrapper = new StackPane(cardBody, savesBadge);
        wrapper.setOnMouseClicked(e -> openCourse(courseId, title, subjectName));
        return wrapper;
    }

    private HBox makePublishedCourseListItem(int courseId, String title, String subjectName, String coverImagePath, int saves) {
        StackPane thumb = new StackPane();
        thumb.setMinSize(56, 56);
        thumb.setPrefSize(56, 56);
        thumb.setMaxSize(56, 56);

        Image img = loadCoverImage(coverImagePath);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(56);
            iv.setFitHeight(56);
            iv.setPreserveRatio(false);
            thumb.getChildren().add(iv);
        } else {
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

        Label savesBadge = new Label("🔖 " + saves);
        savesBadge.getStyleClass().add("saves-badge");

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("action-button");
        openBtn.setOnAction(e -> { e.consume(); openCourse(courseId, title, subjectName); });

        HBox row = new HBox(14, thumb, info, spacer, savesBadge, openBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("course-list-item");
        row.setPrefWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            openCourse(courseId, title, subjectName);
        });
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void openCourse(int courseId, String title, String subjectName) {
        if (accueilController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/LibraryViews/course-details.fxml"));
            Parent view = loader.load();
            CourseDetailsController ctrl = loader.getController();
            ctrl.setAccueilController(accueilController);
            ctrl.setOrigin(CourseDetailsController.Origin.LIBRARY);
            ctrl.setCourse(courseId, title, subjectName);
            accueilController.setContent(view);
        } catch (Exception ex) { ex.printStackTrace(); }
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

    private Image loadCoverImage(String filename) {
        if (filename == null || filename.isBlank()) return null;
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("C:/wamp64/www/covers/" + filename);
            if (p.toFile().exists()) return new Image(p.toUri().toString(), 260, 150, false, true);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    @Override public void onThemeChanged() { updateHeaderIcon(); }

    private boolean isDarkModeNow() { return false; }

    private void updateHeaderIcon() {
        boolean dark = isDarkModeNow();
        String path = "/book-13427-" + (dark ? "dark" : "light") + ".png";
        try {
            libraryHeaderIcon.setImage(new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) { e.printStackTrace(); }
    }
}