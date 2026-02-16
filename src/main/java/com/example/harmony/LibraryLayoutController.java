package com.example.harmony;

import com.example.harmony.services.LibraryService;
import com.example.harmony.util.UiPopups;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class LibraryLayoutController {

    @FXML private Button myCoursesBtn;
    @FXML private TilePane publishedCoursesContainer;
    @FXML private StackPane wheelZone;
    @FXML private StackPane navWheelContainer;

    private final LibraryService libraryService = new LibraryService();

    @FXML
    private void initialize() {
        Platform.runLater(this::loadPublishedCourses);

        myCoursesBtn.setOnAction(e -> {
            try {
                SceneTransitionUtil.changeContent(
                        "/com/example/harmony/courses-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        CoursesLayoutController.class
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void loadPublishedCourses() {
        if (publishedCoursesContainer == null) return;

        publishedCoursesContainer.getChildren().clear();

        try {
            var rows = libraryService.listPublishedCourses();

            if (rows.isEmpty()) {
                publishedCoursesContainer.getChildren().add(makeEmptyState("No published courses yet"));
                return;
            }

            for (var r : rows) {
                publishedCoursesContainer.getChildren().add(
                        makePublishedCourseCard(r.id(), r.title(), r.subjectName())
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            publishedCoursesContainer.getChildren().add(makeEmptyState("Failed to load published courses"));
        }
    }

    private StackPane makePublishedCourseCard(int courseId, String title, String subjectName) {
        Label lbl = new Label(title == null ? "" : title);
        lbl.getStyleClass().add("course-title");

        VBox cardBody = new VBox(6, lbl);
        cardBody.setPadding(new Insets(16));
        cardBody.setAlignment(Pos.CENTER_LEFT);
        cardBody.getStyleClass().add("course-card");
        if (subjectName != null && !subjectName.trim().isEmpty()) {
            Label sub = new Label(subjectName.trim());
            sub.getStyleClass().add("nav-wheel-label");
            cardBody.getChildren().add(sub);
        }

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
            c.setCourse(courseId, title, subjectName);
        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(
                    (myCoursesBtn != null && myCoursesBtn.getScene() != null) ? (javafx.stage.Stage) myCoursesBtn.getScene().getWindow() : null,
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

    
}
