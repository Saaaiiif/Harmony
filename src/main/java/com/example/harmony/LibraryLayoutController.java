package com.example.harmony;

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

public class LibraryLayoutController {

    @FXML private Button myCoursesBtn;
    @FXML private TilePane publishedCoursesContainer;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;

    // NEW
    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;

    private final LibraryService libraryService = new LibraryService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            loadSubjectFilter();   // populate ComboBox first
            loadPublishedCourses(); // then load all courses

            // Live filtering: react to every keystroke in the search bar
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

            // Live filtering: react to subject selection
            subjectFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
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
    }

    private void loadSubjectFilter() {
        try {
            var subjects = libraryService.listPublishedSubjects();
            // "All Subjects" sentinel at the top
            var items = FXCollections.<String>observableArrayList();
            items.add(null); // represents "All Subjects" (shows promptText)
            items.addAll(subjects);
            subjectFilter.setItems(items);
            // Render null as the prompt text label
            subjectFilter.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item == null ? "All Subjects" : item);
                }
            });
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void applyFilters() {
        String keyword = searchField.getText();
        String subject = subjectFilter.getValue();
        if (publishedCoursesContainer == null) return;
        publishedCoursesContainer.getChildren().clear();
        try {
            var rows = libraryService.searchPublishedCourses(keyword, subject);
            if (rows.isEmpty()) {
                publishedCoursesContainer.getChildren().add(makeEmptyState("No courses match your search."));
                return;
            }
            for (var r : rows) {
                publishedCoursesContainer.getChildren().add(
                        makePublishedCourseCard(r.id(), r.title(), r.subjectName(), r.coverImagePath())
                );
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadPublishedCourses() {
        applyFilters(); // delegates — searchField is blank, subjectFilter is null → returns all
    }

    // --- unchanged helpers below ---

    private StackPane makePublishedCourseCard(int courseId, String title, String subjectName, String coverImagePath) {
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

        StackPane wrapper = new StackPane(cardBody);
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
