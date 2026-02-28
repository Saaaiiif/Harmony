package com.example.harmony;

import com.example.harmony.interfaces.ThemeAware;
import com.example.harmony.services.LibraryService;
import com.example.harmony.util.UiPopups;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class LibraryLayoutController implements ThemeAware {

    @FXML private Button myCoursesBtn;
    @FXML private TilePane publishedCoursesContainer;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;

    // NEW
    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ImageView libraryHeaderIcon;
    @FXML private Button gridViewBtn;
    @FXML private Button listViewBtn;
    @FXML private ScrollPane libraryScrollPane;
    private final VBox listContainer = new VBox(8);


    private boolean isGridView = true;


    private final LibraryService libraryService = new LibraryService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            loadSubjectFilter();
            loadPublishedCourses();
            updateHeaderIcon();

            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        });

        myCoursesBtn.setOnAction(e -> {
            try {
                SceneTransitionUtil.changeContent(
                        "/com/example/harmony/courses-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        CoursesLayoutController.class
                );
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        gridViewBtn.setOnAction(e -> setViewMode(true));
        listViewBtn.setOnAction(e -> setViewMode(false));
        gridViewBtn.getStyleClass().add("view-toggle-btn-active"); // default

    }
    private void setViewMode(boolean grid) {
        isGridView = grid;
        gridViewBtn.getStyleClass().remove("view-toggle-btn-active");
        listViewBtn.getStyleClass().remove("view-toggle-btn-active");
        if (grid) gridViewBtn.getStyleClass().add("view-toggle-btn-active");
        else      listViewBtn.getStyleClass().add("view-toggle-btn-active");

        if (grid) libraryScrollPane.setContent(publishedCoursesContainer);
        else {
            listContainer.setPadding(new Insets(24));
            libraryScrollPane.setContent(listContainer);
        }
        applyFilters();
    }

    private void refreshCourses() {
        applyFilters();
    }

    @Override
    public void onThemeChanged() {
        updateHeaderIcon();
    }
    private boolean isDarkModeNow() {
        RootLayoutController rc = SceneTransitionUtil.getRootController();
        return rc != null && rc.isDarkMode();
    }

    private void updateHeaderIcon() {
        boolean dark = isDarkModeNow();
        String path = "/book-13427-" + (dark ? "dark" : "light") + ".png";
        try {
            libraryHeaderIcon.setImage(new Image(
                    getClass().getResourceAsStream(path)
            ));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSubjectFilter() {
        try {
            var subjects = libraryService.listPublishedSubjects();

            javafx.collections.ObservableList<String> allItems =
                    FXCollections.observableArrayList();
            allItems.addAll(subjects);

            javafx.collections.transformation.FilteredList<String> filtered =
                    new javafx.collections.transformation.FilteredList<>(allItems, s -> true);

            subjectFilter.setItems(filtered);
            subjectFilter.setEditable(true);
            subjectFilter.setPromptText("All Subjects");

            subjectFilter.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(String s) { return s == null ? "" : s; }
                @Override public String fromString(String s) {
                    if (s == null || s.isBlank()) return null;
                    return allItems.stream()
                            .filter(i -> i.equalsIgnoreCase(s.trim()))
                            .findFirst()
                            .orElse(null);
                }
            });

            subjectFilter.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(""); setGraphic(null); return; }
                    setText(item);
                }
            });

            subjectFilter.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item == null || empty ? "" : item);
                }
            });

            // typing filters the dropdown list
            subjectFilter.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                // if the user just selected an item, don't re-filter
                String current = subjectFilter.getValue();
                if (current != null && current.equals(newVal)) return;

                String q = newVal == null ? "" : newVal.trim().toLowerCase();
                filtered.setPredicate(s -> q.isEmpty() || s.toLowerCase().contains(q));

                if (!subjectFilter.isShowing() && !q.isEmpty()) subjectFilter.show();

                // if field is cleared, reset filter and reload all courses
                if (q.isEmpty()) {
                    subjectFilter.setValue(null);
                    applyFilters();
                }
            });

            // selecting an item from dropdown applies filter
            subjectFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                filtered.setPredicate(s -> true); // reset dropdown list to full
                applyFilters();
            });

            // pressing Enter commits the typed subject if it matches
            subjectFilter.getEditor().setOnAction(e -> {
                String typed = subjectFilter.getEditor().getText();
                if (typed == null || typed.isBlank()) {
                    subjectFilter.setValue(null);
                } else {
                    String match = allItems.stream()
                            .filter(s -> s.equalsIgnoreCase(typed.trim()))
                            .findFirst().orElse(null);
                    subjectFilter.setValue(match);
                }
                subjectFilter.hide();
                applyFilters();
            });

            // clicking X or clearing manually
            subjectFilter.getEditor().setOnMouseClicked(e -> {
                if (subjectFilter.getEditor().getText().isBlank()) {
                    filtered.setPredicate(s -> true);
                    subjectFilter.show();
                }
            });

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void applyFilters() {
        String keyword = searchField.getText();
        String subject = subjectFilter.getValue();
        if (publishedCoursesContainer == null) return;

        publishedCoursesContainer.getChildren().clear();
        listContainer.getChildren().clear(); // ← add this

        try {
            var rows = libraryService.searchPublishedCourses(keyword, subject);
            if (rows.isEmpty()) {
                if (isGridView)
                    publishedCoursesContainer.getChildren().add(makeEmptyState("No courses match your search."));
                else
                    listContainer.getChildren().add(makeEmptyState("No courses match your search."));
                return;
            }
            for (var r : rows) {
                if (isGridView) {
                    publishedCoursesContainer.getChildren().add(
                            makePublishedCourseCard(r.id(), r.title(), r.subjectName(), r.coverImagePath(), r.saves())
                    );
                } else {
                    listContainer.getChildren().add(
                            makePublishedCourseListItem(r.id(), r.title(), r.subjectName(), r.coverImagePath(), r.saves())
                    );
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
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
        openBtn.setOnAction(e -> {
            e.consume();
            openCourse(courseId, title, subjectName);
        });

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

    private void loadPublishedCourses() {
        applyFilters(); // delegates — searchField is blank, subjectFilter is null → returns all
    }

    // --- unchanged helpers below ---

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

    private void openCourse(int courseId, String title, String subjectName) {
        try {
            CourseDetailsController c = SceneTransitionUtil.changeContent(
                    "/com/example/harmony/course-details.fxml",
                    SceneTransitionUtil.TransitionType.FADE,
                    CourseDetailsController.class
            );
            c.setOrigin(CourseDetailsController.Origin.LIBRARY);
            c.setCourse(courseId, title, subjectName);
        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(
                    myCoursesBtn != null && myCoursesBtn.getScene() != null
                            ? (javafx.stage.Stage) myCoursesBtn.getScene().getWindow() : null,
                    "Failed to open course.",
                    true,
                    getClass()
            );
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

    private Image loadCoverImage(String filename) {
        if (filename == null || filename.isBlank()) return null;
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("C:/wamp64/www/covers/" + filename);
            if (p.toFile().exists()) return new Image(p.toUri().toString(), 260, 150, false, true);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }



}
