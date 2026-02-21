package controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Role;
import models.User;
import services.UserService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminJournalController implements Initializable {

    @FXML
    private VBox studentListContainer;

    @FXML
    private VBox emptyState;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStudents();
    }

    private void loadStudents() {
        try {
            List<User> students = userService.getByRole(Role.ETUDIANT);
            studentListContainer.getChildren().clear();

            if (students.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                studentListContainer.setVisible(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                studentListContainer.setVisible(true);

                for (User student : students) {
                    studentListContainer.getChildren().add(createStudentRow(student));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createStudentRow(User student) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.getStyleClass().add("card");

        Label nameLabel = new Label("👤  " + student.getFullName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button rapportBtn = new Button("Rapport d'étudiant");
        rapportBtn.getStyleClass().add("primary-button");
        // TODO: implement rapport action

        row.getChildren().addAll(nameLabel, spacer, rapportBtn);
        return row;
    }
}
