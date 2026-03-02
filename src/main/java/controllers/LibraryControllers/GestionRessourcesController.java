package controllers.LibraryControllers;

import com.example.harmony.DB;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import utils.UiPopups;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GestionRessourcesController {

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private TableView<ReportedCourse>            reportedTable;
    @FXML private TableColumn<ReportedCourse, String>  colCourse;
    @FXML private TableColumn<ReportedCourse, String>  colSubject;
    @FXML private TableColumn<ReportedCourse, String>  colAuthor;
    @FXML private TableColumn<ReportedCourse, Integer> colReports;
    @FXML private TableColumn<ReportedCourse, String>  colStatus;
    @FXML private TableColumn<ReportedCourse, Void>    colActions;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label            reportedCountLabel;
    @FXML private VBox             emptyState;

    // ── Data ─────────────────────────────────────────────────────────────────

    private final ObservableList<ReportedCourse> allCourses = FXCollections.observableArrayList();
    private FilteredList<ReportedCourse> filteredCourses;

    // ── Model ─────────────────────────────────────────────────────────────────

    public static class ReportedCourse {
        private final int    courseId;
        private final String title;
        private final String subject;
        private final String author;
        private final int    reportCount;
        private       String status;

        public ReportedCourse(int courseId, String title, String subject,
                              String author, int reportCount, String status) {
            this.courseId    = courseId;
            this.title       = title;
            this.subject     = subject;
            this.author      = author;
            this.reportCount = reportCount;
            this.status      = status;
        }

        public int    getCourseId()    { return courseId; }
        public String getTitle()       { return title; }
        public String getSubject()     { return subject != null ? subject : "—"; }
        public String getAuthor()      { return author  != null ? author  : "—"; }
        public int    getReportCount() { return reportCount; }
        public String getStatus()      { return status; }
        public void   setStatus(String s) { this.status = s; }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        reportedTable.getStyleClass().add("light-mode");
        setupColumns();
        setupFilters();
        Platform.runLater(this::loadData);
    }

    private void setupColumns() {

        // Course title — course-title class
        colCourse.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colCourse.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label lbl = new Label(v);
                lbl.getStyleClass().add("course-title");
                lbl.setStyle("-fx-font-size: 13px;");
                setGraphic(lbl); setText(null);
            }
        });

        // Subject — course-name class
        colSubject.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubject()));
        colSubject.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label lbl = new Label(v);
                lbl.getStyleClass().add("course-name");
                setGraphic(lbl); setText(null);
            }
        });

        // Author — course-name class
        colAuthor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuthor()));
        colAuthor.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label lbl = new Label(v);
                lbl.getStyleClass().add("course-name");
                setGraphic(lbl); setText(null);
            }
        });

        // Report count — saves-badge class (matches library)
        colReports.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getReportCount()).asObject());
        colReports.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label badge = new Label("🚩 " + v);
                badge.getStyleClass().add("saves-badge");
                setGraphic(badge); setText(null);
            }
        });

        // Status badge
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label lbl = new Label(v);
                lbl.getStyleClass().add("Publié".equals(v) ? "gr-status-published" : "gr-status-unpublished");
                setGraphic(lbl); setText(null);
            }
        });

        // Actions — pure action-button class, no inline styles
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button unpublishBtn = new Button("Dépublier");
            private final Button republishBtn = new Button("Republier");
            private final HBox   box          = new HBox(8, unpublishBtn, republishBtn);

            {
                box.setAlignment(Pos.CENTER);
                unpublishBtn.getStyleClass().add("action-button");
                republishBtn.getStyleClass().add("action-button");

                unpublishBtn.setOnAction(e -> {
                    ReportedCourse course = getTableView().getItems().get(getIndex());
                    handleUnpublish(course);
                });
                republishBtn.setOnAction(e -> {
                    ReportedCourse course = getTableView().getItems().get(getIndex());
                    handleRepublish(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                ReportedCourse course = getTableView().getItems().get(getIndex());
                boolean published = "Publié".equals(course.getStatus());
                unpublishBtn.setVisible(published);
                unpublishBtn.setManaged(published);
                republishBtn.setVisible(!published);
                republishBtn.setManaged(!published);
                setGraphic(box);
            }
        });
    }

    private void setupFilters() {
        filterStatus.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Publié", "Dépublié"
        ));
        filterStatus.setValue("Tous les statuts");

        filteredCourses = new FilteredList<>(allCourses, c -> true);
        reportedTable.setItems(filteredCourses);

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = filterStatus.getValue();

        filteredCourses.setPredicate(c -> {
            boolean matchSearch = search.isEmpty()
                    || c.getTitle().toLowerCase().contains(search)
                    || c.getAuthor().toLowerCase().contains(search)
                    || c.getSubject().toLowerCase().contains(search);
            boolean matchStatus = "Tous les statuts".equals(status)
                    || status == null
                    || c.getStatus().equals(status);
            return matchSearch && matchStatus;
        });

        updateEmptyState();
    }

    // ── DB ────────────────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadData(); }

    private void loadData() {
        allCourses.clear();

        String sql = """
            SELECT
                c.id,
                c.title,
                s.name AS subject_name,
                CONCAT(u.user_prenom, ' ', u.user_nom) AS author_name,
                COUNT(r.id) AS report_count,
                COALESCE(c.is_published, 1) AS published
            FROM courses c
            LEFT JOIN subject s ON s.id = c.subjectid
            LEFT JOIN user    u ON u.user_id = c.userid
            INNER JOIN course_reports r ON r.course_id = c.id
            GROUP BY c.id, c.title, s.name, author_name, c.is_published
            ORDER BY report_count DESC
        """;

        List<ReportedCourse> rows = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new ReportedCourse(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("subject_name"),
                        rs.getString("author_name"),
                        rs.getInt("report_count"),
                        rs.getInt("published") == 1 ? "Publié" : "Dépublié"
                ));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors du chargement : " + ex.getMessage());
        }

        allCourses.setAll(rows);
        reportedCountLabel.setText(String.valueOf(rows.size()));
        updateEmptyState();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleUnpublish(ReportedCourse course) {
        boolean ok = UiPopups.confirm(
                getOwnerStage(),
                "Dépublier « " + course.getTitle() + " » ?\n\nLe cours ne sera plus visible par les utilisateurs.",
                false, getClass()
        );
        if (ok) setPublished(course, false);
    }

    private void handleRepublish(ReportedCourse course) {
        boolean ok = UiPopups.confirm(
                getOwnerStage(),
                "Republier « " + course.getTitle() + " » ?\n\nLe cours redeviendra visible par les utilisateurs.",
                false, getClass()
        );
        if (ok) setPublished(course, true);
    }

    private void setPublished(ReportedCourse course, boolean publish) {
        String sql = "UPDATE courses SET is_published = ? WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publish ? 1 : 0);
            ps.setInt(2, course.getCourseId());
            ps.executeUpdate();
            course.setStatus(publish ? "Publié" : "Dépublié");
            reportedTable.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible de modifier le statut : " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateEmptyState() {
        boolean empty = filteredCourses.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
    }

    private Stage getOwnerStage() {
        if (reportedTable != null && reportedTable.getScene() != null)
            return (Stage) reportedTable.getScene().getWindow();
        return null;
    }

    private void showError(String msg) {
        UiPopups.showError(getOwnerStage(), msg, false, getClass());
    }
}