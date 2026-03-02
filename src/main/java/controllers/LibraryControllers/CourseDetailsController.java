package controllers.LibraryControllers;

import com.example.harmony.DB;
import com.example.harmony.SessionManager;
import controllers.UserControlleers.modifControl.AccueilController;
import interfaces.ThemeAware;
import javafx.fxml.FXMLLoader;
import models.LibraryModels.SubjectRow;
import models.UserModels.Session;
import services.LibraryServices.CourseService;
import utils.UiPopups;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javax.imageio.ImageIO;
import java.io.*;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import services.LibraryServices.ReportService;
import models.UserModels.Session;

import org.fxmisc.richtext.model.StyledDocument;
import services.LibraryServices.LibraryService;
import services.LibraryServices.SuggestionsService;

public class CourseDetailsController implements ThemeAware {
    @FXML private BorderPane root;
    @FXML private Label courseTitle;
    @FXML private Button backBtn;
    @FXML private Button createNoteBtn;
    @FXML private TilePane filesContainer;
    @FXML private NavWheelController navWheelController;
    @FXML private Button addFilesBtn;
    @FXML private Label courseSubject;
    @FXML private ToggleButton publishToggle;
    @FXML private Label publishLabel;
    @FXML private Button saveToLibraryBtn;
    @FXML private Button saveAsLibraryCopyBtn;
    @FXML private Button reportBtn;

    // ── Suggestions strips ────────────────────────────────────────────────────
    @FXML private VBox        booksSection;
    @FXML private HBox        booksContainer;
    @FXML private ScrollPane  booksScrollPane;
    @FXML private VBox        videosSection;
    @FXML private HBox        videosContainer;
    @FXML private ScrollPane  videosScrollPane;

    // ── View toggle ───────────────────────────────────────────────────────────
    @FXML private Button gridViewBtn;
    @FXML private Button listViewBtn;
    @FXML private VBox   filesWrapper;
    private boolean isGridView = true;
    private final VBox listContainer = new VBox(8);
    private boolean isOwner = true;
    private final ReportService reportService = new ReportService();

    private final CourseService courseService = new CourseService();
    private final SuggestionsService suggestionsService = new SuggestionsService();

    private int courseId;
    private AccueilController accueilController;
    private final AtomicBoolean internalToggleChange = new AtomicBoolean(false);
    private volatile boolean createNoteArmed = false;

    private static final org.fxmisc.richtext.model.Codec<String> CSS_CODEC =
            org.fxmisc.richtext.model.Codec.STRING_CODEC;

    private org.fxmisc.richtext.model.Codec<org.fxmisc.richtext.model.StyledDocument<String, String, String>>
    getDocCodec(org.fxmisc.richtext.InlineCssTextArea area) {

        org.reactfx.util.Tuple2<
                org.fxmisc.richtext.model.Codec<String>,
                org.fxmisc.richtext.model.Codec<org.fxmisc.richtext.model.StyledSegment<String, String>>
                > codecs = area.getStyleCodecs()
                .orElseThrow(() -> new IllegalStateException("Style codecs not available"));

        return org.fxmisc.richtext.model.ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
    }

    public enum Origin { COURSES, LIBRARY }

    private Origin origin = Origin.COURSES;

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public void setCourse(int courseId, String title, String subjectName) {
        this.courseId = courseId;
        courseTitle.setText(title == null ? "Course title" : title);
        String s = (subjectName == null) ? "" : subjectName.trim();
        courseSubject.setText(s);


        // ── ownership check first ─────────────────────────────────
        int currentUserId = Session.getInstance().getUser().getUser_id();
        try (var conn = DB.getConnection();
             var ps = conn.prepareStatement("SELECT userid FROM courses WHERE id = ?")) {
            ps.setInt(1, courseId);
            try (var rs = ps.executeQuery()) {
                isOwner = rs.next() && rs.getInt("userid") == currentUserId;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            isOwner = false;
        }

        applyOwnershipUi();
        if (isOwner) {
            installInlineEdit();
        }

        // ── saved to library state ────────────────────────────────
        if (saveToLibraryBtn != null && !isOwner) {
            try {
                LibraryService libraryService =
                        new LibraryService();
                boolean alreadySaved = libraryService.isCourseSaved(currentUserId, courseId);
                saveToLibraryBtn.setText(alreadySaved ? "✔ Saved — click to remove" : "🔖 Save to Library");
                saveToLibraryBtn.setDisable(false); // ← always enabled so they can toggle
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        refreshFiles(); // ← AFTER isOwner is set
        setupReportButton(); // ← AFTER origin and isOwner are both set
        loadSuggestions(courseTitle.getText(), courseSubject.getText());

        // ── publish toggle ────────────────────────────────────────
        if (publishToggle != null) {
            try {
                boolean published = courseService.isCoursePublished(courseId);
                publishToggle.setSelected(published);
            } catch (Exception ex) {
                ex.printStackTrace();
                publishToggle.setSelected(false);
            }
        }
    }
    private void applyOwnershipUi() {
        if (publishLabel != null) {
            publishLabel.setVisible(isOwner);
            publishLabel.setManaged(isOwner);
        }
        if (publishToggle != null) {
            publishToggle.setVisible(isOwner);
            publishToggle.setManaged(isOwner);
        }
        if (createNoteBtn != null) {
            createNoteBtn.setVisible(isOwner);
            createNoteBtn.setManaged(isOwner);
        }
        if (addFilesBtn != null) {
            addFilesBtn.setVisible(isOwner);
            addFilesBtn.setManaged(isOwner);
        }
        if (saveToLibraryBtn != null) {
            saveToLibraryBtn.setVisible(!isOwner);
            saveToLibraryBtn.setManaged(!isOwner);
        }
        if (saveAsLibraryCopyBtn != null) {
            saveAsLibraryCopyBtn.setVisible(!isOwner);
            saveAsLibraryCopyBtn.setManaged(!isOwner);
        }
    }

    @FXML
    private void initialize() {
        root.getStyleClass().add("light-mode");
        if (navWheelController != null) navWheelController.setActiveIndex(5);
        if (navWheelController != null) navWheelController.setOnNavigate(this::handleNavigation);
        wirePublishToggle();

        // Grid / List toggle
        gridViewBtn.getStyleClass().add("view-toggle-btn-active");
        gridViewBtn.setOnAction(e -> setViewMode(true));
        listViewBtn.setOnAction(e -> setViewMode(false));

        backBtn.setOnAction(e -> {
            if (accueilController == null) return;

            try {
                String fxml;
                boolean fromLibrary = (origin == Origin.LIBRARY);

                if (fromLibrary) {
                    fxml = "/views/LibraryViews/library-layout.fxml";
                } else {
                    fxml = "/views/LibraryViews/courses-layout.fxml";
                }

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                Parent view = loader.load();

                if (fromLibrary) {
                    LibraryLayoutController ctrl = loader.getController();
                    ctrl.setAccueilController(accueilController);
                } else {
                    CoursesLayoutController ctrl = loader.getController();
                    ctrl.setAccueilController(accueilController);
                }

                accueilController.setContent(view);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        addFilesBtn.setOnAction(e -> onAddFiles());

        createNoteArmed = false;
        Platform.runLater(() -> createNoteArmed = true);

        if (createNoteBtn != null) {
            createNoteBtn.setFocusTraversable(false);
            createNoteBtn.setOnAction(e -> {
                if (!createNoteArmed) return;
                onCreateNote();
            });
        }
        if (saveToLibraryBtn != null) {
            saveToLibraryBtn.setOnAction(e -> saveToUserLibrary());
        }

        if (saveAsLibraryCopyBtn != null) {
            saveAsLibraryCopyBtn.setOnAction(e -> saveAsLibraryCopy());
        }
    }

    private void showReportDialog() {
        ChoiceBox<String> reasonBox = new ChoiceBox<>();
        reasonBox.getItems().addAll(
                "Inappropriate content",
                "Misleading information",
                "Spam or advertisement",
                "Plagiarized content",
                "Other"
        );
        reasonBox.setValue("Inappropriate content");
        reasonBox.setMaxWidth(Double.MAX_VALUE);
        reasonBox.getStyleClass().add("subject-filter");

        TextArea detailsArea = new TextArea();
        detailsArea.setPromptText("Additional details (optional)…");
        detailsArea.setPrefRowCount(3);
        detailsArea.setWrapText(true);

        Label reasonLabel = new Label("Reason");
        reasonLabel.getStyleClass().add("recommended-title");
        reasonLabel.setStyle("-fx-font-size: 13px;");

        Label detailsLabel = new Label("Details");
        detailsLabel.getStyleClass().add("recommended-title");
        detailsLabel.setStyle("-fx-font-size: 13px;");

        VBox dialogContent = new VBox(8, reasonLabel, reasonBox, detailsLabel, detailsArea);
        dialogContent.setPadding(new Insets(4, 0, 0, 0));

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Report Course");
        dialog.setHeaderText("Report \"" + courseTitle.getText() + "\"");
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Apply the same styled popup used everywhere in the app
        Stage owner = (Stage) root.getScene().getWindow();
        dialog.initOwner(owner);
        UiPopups.styleDialog(dialog, isDarkModeNow(), getClass());

        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(confirmed -> {
            if (!confirmed) return;
            try {
                int userId = Session.getInstance().getUser().getUser_id();
                boolean submitted = reportService.submitReport(
                        userId,
                        courseId,
                        reasonBox.getValue(),
                        detailsArea.getText()
                );
                if (submitted) {
                    reportBtn.setText("⚑ Reported");
                    reportBtn.setDisable(true);
                    reportBtn.getStyleClass().remove("report-btn");
                    reportBtn.getStyleClass().add("report-btn-done");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void setupReportButton() {
        // Only show in Library view, and never on the user's own courses
        if (origin != Origin.LIBRARY || isOwner) {
            reportBtn.setVisible(false);
            reportBtn.setManaged(false);
            return;
        }

        try {
            int userId = Session.getInstance().getUser().getUser_id();
            boolean alreadyReported = reportService.hasReported(userId, courseId);
            if (alreadyReported) {
                reportBtn.setText("⚑ Reported");
                reportBtn.setDisable(true);
                reportBtn.getStyleClass().setAll("action-button", "report-btn-done");
            } else {
                reportBtn.getStyleClass().setAll("action-button", "report-btn");
                reportBtn.setOnAction(e -> showReportDialog());
            }

            reportBtn.setVisible(true);
            reportBtn.setManaged(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            reportBtn.setVisible(false);
            reportBtn.setManaged(false);
        }
    }
    public void setAccueilController(AccueilController accueilController) {
        this.accueilController = accueilController;
    }
    private void saveToUserLibrary() {
        int currentUserId = Session.getInstance().getUser().getUser_id();
        Stage owner = (Stage) root.getScene().getWindow();

        try {
            LibraryService libraryService =
                    new LibraryService();

            boolean alreadySaved = libraryService.isCourseSaved(currentUserId, courseId);

            if (alreadySaved) {
                boolean ok = UiPopups.confirm(
                        owner,
                        "Remove this course from your library?",
                        isDarkModeNow(),
                        getClass()
                );
                if (!ok) return;
                libraryService.unsaveCourse(currentUserId, courseId);
                saveToLibraryBtn.setText("🔖 Save to Library");
            } else {
                libraryService.saveCourse(currentUserId, courseId);
                saveToLibraryBtn.setText("✔ Saved — click to remove");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(owner, "Failed to update library.", isDarkModeNow(), getClass());
        }
    }


    private void onAddFiles() {
        Stage owner = (Stage) root.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Select files");

        List<File> selected = fc.showOpenMultipleDialog(owner);
        if (selected == null || selected.isEmpty()) return;

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (File file : selected) {
                    courseService.uploadCourseFile(courseId, file.toPath().toFile());
                }
                return null;
            }
        };

        uploadTask.setOnSucceeded(ev -> refreshFiles());
        uploadTask.setOnFailed(ev -> {
            uploadTask.getException().printStackTrace();
            UiPopups.showError(owner, "Upload failed", isDarkModeNow(), getClass());
        });

        Thread t = new Thread(uploadTask, "upload-files");
        t.setDaemon(true);
        t.start();
    }

    private void handleNavigation(int index) {
        try {
            switch (index) {
                case 0 -> SceneTransitionUtil.changeContent(
                        "/views/LibraryViews/front-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        FrontLayoutController.class
                );
                case 5 -> SceneTransitionUtil.changeContent(
                        "/views/LibraryViews/courses-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        CoursesLayoutController.class
                );
                default -> { }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onThemeChanged() {
        if (navWheelController != null) navWheelController.onThemeChanged();
    }

    private boolean isDarkModeNow() {
        return false;
    }

    private void setViewMode(boolean grid) {
        isGridView = grid;
        gridViewBtn.getStyleClass().remove("view-toggle-btn-active");
        listViewBtn.getStyleClass().remove("view-toggle-btn-active");
        if (grid) gridViewBtn.getStyleClass().add("view-toggle-btn-active");
        else      listViewBtn.getStyleClass().add("view-toggle-btn-active");

        filesWrapper.getChildren().clear();
        if (grid) {
            filesWrapper.getChildren().add(filesContainer);
        } else {
            listContainer.setPadding(new Insets(8, 24, 24, 24));
            filesWrapper.getChildren().add(listContainer);
        }
        refreshFiles();
    }

    private void refreshFiles() {
        try {
            List<CourseService.CourseFileRow> files = courseService.listCourseFiles(courseId);
            filesContainer.getChildren().clear();
            listContainer.getChildren().clear();

            if (files.isEmpty()) {
                Label empty = new Label("No files yet");
                empty.getStyleClass().add("empty-state-label");
                if (isGridView) filesContainer.getChildren().add(empty);
                else            listContainer.getChildren().add(empty);
                return;
            }

            for (CourseService.CourseFileRow f : files) {
                if (isGridView) filesContainer.getChildren().add(makeFileCard(f));
                else            listContainer.getChildren().add(makeFileListItem(f));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private HBox makeFileListItem(CourseService.CourseFileRow f) {
        // Thumbnail (small)
        StackPane thumb = new StackPane();
        thumb.setMinSize(56, 56); thumb.setPrefSize(56, 56); thumb.setMaxSize(56, 56);
        thumb.setAlignment(javafx.geometry.Pos.CENTER);
        thumb.getStyleClass().add("course-card-image-placeholder");

        String ext = f.originalName() != null && f.originalName().contains(".")
                ? f.originalName().substring(f.originalName().lastIndexOf(".") + 1).toUpperCase()
                : "FILE";

        // Use custom note icon for note file types; emoji fallback for everything else
        String fn = f.originalName() == null ? "" : f.originalName().toLowerCase();
        boolean isNoteFile = fn.endsWith(".rtfx") || fn.endsWith(".txt") || fn.endsWith(".md")
                || (f.mimeType() != null && f.mimeType().startsWith("text"));
        javafx.scene.Node iconNode;
        if (isNoteFile) {
            try {
                java.io.InputStream stream = getClass().getResourceAsStream("/note.png");
                if (stream != null) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(stream));
                    iv.setFitWidth(34);
                    iv.setFitHeight(34);
                    iv.setPreserveRatio(true);
                    iv.setOpacity(0.3);
                    StackPane.setAlignment(iv, javafx.geometry.Pos.CENTER);
                    iconNode = iv;
                } else {
                    Label fb = new Label("📝");
                    fb.setStyle("-fx-font-size: 22px;");
                    iconNode = fb;
                }
            } catch (Exception _ex) {
                Label fb = new Label("📝");
                fb.setStyle("-fx-font-size: 22px;");
                iconNode = fb;
            }
        } else {
            Label extLbl = new Label(fileIcon(f.originalName(), f.mimeType()));
            extLbl.setStyle("-fx-font-size: 22px;");
            iconNode = extLbl;
        }
        thumb.getChildren().add(iconNode);

        // Info
        Label nameLbl = new Label(stripExt(f.originalName() == null ? "" : f.originalName()));
        nameLbl.getStyleClass().add("course-title");
        nameLbl.setStyle("-fx-font-size: 14px;");

        String size = f.sizeBytes() > 0
                ? (f.sizeBytes() < 1024 * 1024
                ? (f.sizeBytes() / 1024) + " KB"
                : String.format("%.1f MB", f.sizeBytes() / (1024.0 * 1024)))
                : "";
        Label metaLbl = new Label(ext + (size.isEmpty() ? "" : "  •  " + size));
        metaLbl.getStyleClass().add("course-subtitle");

        VBox info = new VBox(3, nameLbl, metaLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("action-button");
        openBtn.setOnAction(e -> {
            e.consume();
            String n = f.originalName() == null ? "" : f.originalName().toLowerCase();
            boolean isNote = n.endsWith(".rtfx") || n.endsWith(".txt") || n.endsWith(".md")
                    || (f.mimeType() != null && f.mimeType().startsWith("text"));
            if (isNote) openNoteEditor(f);
            else        openFilePopup(f);
        });

        HBox row = new HBox(14, thumb, info, spacer, openBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("course-list-item");
        row.setPrefWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            String n = f.originalName() == null ? "" : f.originalName().toLowerCase();
            boolean isNote = n.endsWith(".rtfx") || n.endsWith(".txt") || n.endsWith(".md")
                    || (f.mimeType() != null && f.mimeType().startsWith("text"));
            if (isNote) openNoteEditor(f);
            else        openFilePopup(f);
        });
        return row;
    }

    private String fileIcon(String name, String mime) {
        if (name == null) return "📄";
        String n = name.toLowerCase();
        if (n.endsWith(".pdf"))  return "📕";
        if (n.endsWith(".rtfx") || n.endsWith(".txt") || n.endsWith(".md")) return "📝";
        if (n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".jpeg")) return "🖼";
        if (n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".avi")) return "🎬";
        if (n.endsWith(".mp3") || n.endsWith(".wav")) return "🎵";
        if (n.endsWith(".zip") || n.endsWith(".rar")) return "🗜";
        if (n.endsWith(".ppt") || n.endsWith(".pptx")) return "📊";
        if (n.endsWith(".doc") || n.endsWith(".docx")) return "📃";
        if (n.endsWith(".xls") || n.endsWith(".xlsx")) return "📈";
        return "📄";
    }

    private StackPane makeFileCard(CourseService.CourseFileRow f) {
        final double thumbW = 210;
        final double thumbH = 130;
        final double cardW = 240;

        StackPane thumbHost = new StackPane();
        thumbHost.setMinSize(thumbW, thumbH);
        thumbHost.setPrefSize(thumbW, thumbH);
        thumbHost.setMaxSize(thumbW, thumbH);

        Label loading = new Label("Loading...");
        loading.getStyleClass().add("nav-wheel-label");
        thumbHost.getChildren().setAll(loading);

        Label name = new Label(stripExt(f.originalName() == null ? "" : f.originalName()));
        name.getStyleClass().add("course-title");
        name.setWrapText(true);
        name.setMaxWidth(cardW);

        VBox cardBody = new VBox(10, thumbHost, name);
        cardBody.setPadding(new Insets(12));
        cardBody.setAlignment(Pos.TOP_LEFT);
        cardBody.getStyleClass().add("course-card");
        cardBody.setMinWidth(cardW);
        cardBody.setPrefWidth(cardW);
        cardBody.setMaxWidth(cardW);

        StackPane wrapper = new StackPane(cardBody);
        wrapper.setMinWidth(cardW);
        wrapper.setPrefWidth(cardW);
        wrapper.setMaxWidth(cardW);

        TilePane.setMargin(wrapper, new Insets(8));

        wrapper.setOnMouseClicked(e -> {
            String n = f.originalName() == null ? "" : f.originalName().toLowerCase();
            boolean isNote = n.endsWith(".rtfx") || n.endsWith(".txt") || n.endsWith(".md")
                    || (f.mimeType() != null && f.mimeType().startsWith("text"));
            if (isNote) openNoteEditor(f);
            else openFilePopup(f);
        });

        Button xBtn = new Button("X");
        xBtn.getStyleClass().add("action-button");
        xBtn.setFocusTraversable(false);
        xBtn.visibleProperty().bind(wrapper.hoverProperty());
        xBtn.managedProperty().bind(xBtn.visibleProperty());
        xBtn.setOnAction(ev -> {
            ev.consume();
            confirmAndDeleteFromCard(f.id());
        });

        if (isOwner) {                    // ← wrap in isOwner check
            wrapper.getChildren().add(xBtn);
            StackPane.setAlignment(xBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(xBtn, new Insets(8));
        }


        Task<Node> thumbTask = new Task<>() {
            @Override
            protected Node call() throws Exception {
                String mime = f.mimeType();
                byte[] data = courseService.loadCourseFileBytes(f.id());

                if (mime != null && mime.startsWith("image")) {
                    Image img = new Image(new ByteArrayInputStream(data), thumbW, thumbH, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(thumbW);
                    iv.setFitHeight(thumbH);
                    return iv;
                }

                if ("application/pdf".equalsIgnoreCase(mime)) {
                    Image img = renderPdfPageAsFxImage(data, 0, 110, thumbW);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(thumbW);
                    iv.setFitHeight(thumbH);
                    return iv;
                }

                String lowerName = f.originalName() == null ? "" : f.originalName().toLowerCase();
                boolean isTxt = lowerName.endsWith(".txt") || lowerName.endsWith(".md");
                boolean isTextMime = mime != null && mime.startsWith("text");

                if (isTxt || isTextMime) {
                    String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    Label note = new Label(makeSnippet(text, 160));
                    note.setWrapText(true);
                    note.setMaxWidth(thumbW - 20);
                    note.setMaxHeight(thumbH);
                    note.getStyleClass().add("nav-wheel-label");
                    return note;
                }

                // .rtfx note file — show the note icon as card thumbnail
                if (lowerName.endsWith(".rtfx")) {
                    try {
                        java.io.InputStream stream = getClass().getResourceAsStream("/note.png");
                        if (stream != null) {
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                                    new javafx.scene.image.Image(stream));
                            iv.setFitWidth(thumbW * 0.55);
                            iv.setFitHeight(thumbH * 0.55);
                            iv.setPreserveRatio(true);
                            iv.setOpacity(0.3);
                            StackPane.setAlignment(iv, javafx.geometry.Pos.CENTER);
                            return iv;
                        }
                    } catch (Exception _ignored) {}
                }

                Label no = new Label("No preview");
                no.getStyleClass().add("nav-wheel-label");
                return no;
            }
        };

        thumbTask.setOnSucceeded(e -> thumbHost.getChildren().setAll(thumbTask.getValue()));
        thumbTask.setOnFailed(e -> {
            Label fail = new Label("No preview");
            fail.getStyleClass().add("nav-wheel-label");
            thumbHost.getChildren().setAll(fail);
        });

        Thread t = new Thread(thumbTask, "thumb-" + f.id());
        t.setDaemon(true);
        t.start();

        return wrapper;
    }



    private void confirmAndDeleteFromCard(long fileId) {
        Stage owner = (Stage) root.getScene().getWindow();

        if (!UiPopups.confirm(owner, "Delete this file?", isDarkModeNow(), getClass())) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                courseService.deleteCourseFile(fileId);
                return null;
            }
        };

        task.setOnSucceeded(e -> refreshFiles());
        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            UiPopups.showError(owner, "Delete failed", isDarkModeNow(), getClass());
        });

        Thread t = new Thread(task, "delete-file-" + fileId);
        t.setDaemon(true);
        t.start();
    }


    private void openFilePopup(CourseService.CourseFileRow f) {
        Stage owner = (Stage) root.getScene().getWindow();

        Label title = new Label(f.originalName());
        title.getStyleClass().add("section-title");

        StackPane previewHost = new StackPane();
        Label loading = new Label("Loading...");
        loading.getStyleClass().add("nav-wheel-label");
        previewHost.getChildren().setAll(loading);

        ScrollPane previewScroll = new ScrollPane(previewHost);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");

        Button download = new Button("Download");
        download.getStyleClass().add("action-button");
        Button rename = new Button("Rename");
        rename.getStyleClass().add("action-button");
        Button delete = new Button("Delete");
        delete.getStyleClass().add("action-button");
        Button close = new Button("Close");
        close.getStyleClass().add("action-button");

        HBox actions;
        if (isOwner) {
            actions = new HBox(10, download, rename, delete, close);
        } else {
            actions = new HBox(10, download, close);
        }
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox rootBox = new VBox(12, title, previewScroll, actions);
        rootBox.setPadding(new Insets(18));
        Stage popup = UiPopups.buildModalNoTitleBar(
                owner,
                rootBox,
                980, 760,
                true,
                isDarkModeNow(),
                getClass()
        );

        close.setOnAction(e -> popup.close());

        download.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save file");
            fc.setInitialFileName(f.originalName());
            File out = fc.showSaveDialog(popup);
            if (out == null) return;
            try {
                courseService.downloadCourseFile(f.id(), out.toPath());
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Download failed", isDarkModeNow(), getClass());            }
        });

        rename.setOnAction(e -> {
            String original = f.originalName() == null ? "" : f.originalName();
            int dot = original.lastIndexOf('.');
            String base;
            String ext;
            if (dot > 0 && dot < original.length() - 1) {
                base = original.substring(0, dot);
                ext = original.substring(dot);
            } else {
                base = original;
                ext = "";
            }

            TextInputDialog d = new TextInputDialog(base);
            d.initOwner(popup);
            d.setTitle("Rename file");
            d.setHeaderText(null);
            d.setContentText("New name:");

            UiPopups.styleDialog(d, isDarkModeNow(), getClass());
            d.setOnShown(ev2 -> {
                TextField editor = d.getEditor();
                editor.requestFocus();
                editor.selectAll();
            });

            Optional<String> r = d.showAndWait();

            if (r.isEmpty()) return;

            String newBase = r.get().trim();
            if (newBase.isEmpty()) {
                UiPopups.showWarning(popup, "Name required", isDarkModeNow(), getClass());                return;
            }

            String newFullName = newBase + ext;
            try {
                courseService.renameCourseFile(f.id(), newFullName);
                popup.close();
                refreshFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Rename failed", isDarkModeNow(), getClass());            }
        });

        delete.setOnAction(e -> {
            if (!UiPopups.confirm(owner, "Delete this file?", isDarkModeNow(), getClass())) return;

            try {
                courseService.deleteCourseFile(f.id());
                popup.close();
                refreshFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(owner, "Delete failed", isDarkModeNow(), getClass());            }
        });

        Task<Node> previewTask = new Task<>() {
            @Override
            protected Node call() throws Exception {
                String mime = f.mimeType();
                byte[] data = courseService.loadCourseFileBytes(f.id());

                if (mime != null && mime.startsWith("image/")) {
                    Image img = new Image(new ByteArrayInputStream(data));
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(900);
                    return iv;
                }

                if ("application/pdf".equalsIgnoreCase(mime)) {
                    return renderPdfAllPagesToVBox(data, 220, 900);
                }

                String lowerName = (f.originalName() == null) ? "" : f.originalName().toLowerCase();
                boolean isTxt = lowerName.endsWith(".txt") || lowerName.endsWith(".md");
                boolean isTextMime = (mime != null && mime.startsWith("text/"));
                if (isTxt || isTextMime) {
                    String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    TextArea ta = new TextArea(text);
                    ta.setWrapText(true);
                    ta.setEditable(false);
                    ta.setStyle("-fx-background-color: transparent;");
                    return ta;
                }

                Label no = new Label("No embedded preview for: " + mime);
                no.getStyleClass().add("nav-wheel-label");
                return no;
            }
        };

        previewTask.setOnSucceeded(e -> previewHost.getChildren().setAll(previewTask.getValue()));
        previewTask.setOnFailed(e -> {
            previewTask.getException().printStackTrace();
            Label fail = new Label("Preview failed");
            fail.getStyleClass().add("nav-wheel-label");
            previewHost.getChildren().setAll(fail);
        });

        Thread t = new Thread(previewTask, "preview-" + f.id());
        t.setDaemon(true);
        t.start();

        popup.showAndWait();
    }


    private Image renderPdfPageAsFxImage(byte[] pdfBytes, int pageIndex, float dpi, double fitWidth) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer r = new PDFRenderer(doc);
            var bim = r.renderImageWithDPI(pageIndex, dpi);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()), fitWidth, 0, true, true);
        }
    }

    private Node renderPdfAllPagesToVBox(byte[] pdfBytes, float dpi, double fitWidth) throws Exception {
        VBox pages = new VBox(10);
        pages.setFillWidth(true);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer r = new PDFRenderer(doc);
            int count = doc.getNumberOfPages();

            for (int i = 0; i < count; i++) {
                var bim = r.renderImageWithDPI(i, dpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", baos);

                Image img = new Image(new ByteArrayInputStream(baos.toByteArray()), fitWidth, 0, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(fitWidth);

                pages.getChildren().add(iv);
            }
        }

        return pages;
    }

    private void installInlineEdit() {
        enableLabelEditOnDblClick(courseTitle, "Course name", newValue -> {
            if (newValue.isBlank()) return false;
            try {
                courseService.renameCourse(courseId, newValue.trim());
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        });

        try {
            enableSubjectEditOnDblClick(courseSubject, courseService.listSubjects());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    private void enableLabelEditOnDblClick(
            Label label,
            String prompt,
            java.util.function.Function<String, Boolean> onCommit
    ) {
        label.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;

            Parent p = label.getParent();
            if (!(p instanceof Pane pane)) return;

            int idx = pane.getChildren().indexOf(label);
            if (idx < 0) return;

            TextField tf = new TextField(label.getText());
            tf.setPromptText(prompt);
            tf.getStyleClass().addAll(label.getStyleClass());

            Runnable commit = () -> {
                String v = tf.getText() == null ? "" : tf.getText().trim();
                if (!onCommit.apply(v)) {
                    Stage owner = (label.getScene() == null) ? null : (Stage) label.getScene().getWindow();
                    UiPopups.showWarning(owner, "Invalid value", isDarkModeNow(), getClass());
                    return;
                }
                label.setText(v);
                pane.getChildren().set(idx, label);
            };

            tf.setOnAction(ev -> commit.run());
            tf.focusedProperty().addListener((obs, was, isNow) -> { if (!isNow) commit.run(); });

            pane.getChildren().set(idx, tf);
            Platform.runLater(tf::requestFocus);
        });
    }
    private void enableSubjectEditOnDblClick(Label subjectLabel, java.util.List<SubjectRow> subjects) {

        subjectLabel.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;

            Parent p = subjectLabel.getParent();
            if (!(p instanceof Pane pane)) return;

            int idx = pane.getChildren().indexOf(subjectLabel);
            if (idx < 0) return;

            ObservableList<SubjectRow> items = javafx.collections.FXCollections.observableArrayList(subjects);
            FilteredList<SubjectRow> filtered = new FilteredList<>(items, s -> true);

            ComboBox<SubjectRow> cb = new ComboBox<>(filtered);
            cb.setEditable(true);
            cb.getStyleClass().addAll(subjectLabel.getStyleClass());
            cb.setPrefWidth(220);

            cb.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(SubjectRow s) {
                    return (s == null) ? "" : s.name();
                }
                @Override public SubjectRow fromString(String text) {
                    if (text == null) return null;
                    String q = text.trim();
                    if (q.isEmpty()) return null;
                    for (SubjectRow s : items) {
                        if (s.name().equalsIgnoreCase(q)) return s;
                    }
                    return null;
                }
            });

            cb.getEditor().setText(subjectLabel.getText());

            cb.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                String q = (newText == null) ? "" : newText.trim().toLowerCase();
                filtered.setPredicate(s -> q.isEmpty() || s.name().toLowerCase().contains(q));
                if (!cb.isShowing()) cb.show();
            });

            Runnable commit = () -> {
                String typed = cb.getEditor().getText() == null ? "" : cb.getEditor().getText().trim();
                if (typed.isEmpty() && cb.getValue() != null) typed = cb.getConverter().toString(cb.getValue()).trim();

                Stage owner = (subjectLabel.getScene() == null) ? null : (Stage) subjectLabel.getScene().getWindow();

                try {
                    courseService.updateCourseSubject(courseId, typed);
                    subjectLabel.setText(typed);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UiPopups.showError(owner, "Failed to update subject", isDarkModeNow(), getClass());
                } finally {
                    pane.getChildren().set(idx, subjectLabel);
                }
            };

            cb.setOnAction(ev -> commit.run());
            cb.focusedProperty().addListener((obs, was, isNow) -> { if (!isNow) commit.run(); });

            pane.getChildren().set(idx, cb);
            Platform.runLater(() -> {
                cb.requestFocus();
                cb.getEditor().selectAll();
                cb.show();
            });
        });
    }

    private void onCreateNote() {
        Stage owner = (Stage) root.getScene().getWindow();

        Task<CourseService.CourseFileRow> task = new Task<>() {
            @Override
            protected CourseService.CourseFileRow call() throws Exception {
                List<CourseService.CourseFileRow> files = courseService.listCourseFiles(courseId);
                String base = suggestNextCopyBase("note", ".rtfx", files);
                String fileName = ensureExt(base, ".rtfx");
                return new CourseService.CourseFileRow(-1L, fileName, "application/octet-stream", 0L, null);
            }
        };

        task.setOnSucceeded(ev -> openNoteEditor(task.getValue()));
        task.setOnFailed(ev -> {
            task.getException().printStackTrace();
            UiPopups.showError(owner, "Create note failed", isDarkModeNow(), getClass());
        });

        Thread th = new Thread(task, "create-note-ui");
        th.setDaemon(true);
        th.start();
    }

    private void openNewNoteDraft(Stage owner, String suggestedBaseName) {
        TextField nameField = new TextField(suggestedBaseName == null ? "note" : suggestedBaseName);
        nameField.setPromptText("Note name");

        InlineCssTextArea area = new InlineCssTextArea();
        area.setWrapText(true);

        Button save = new Button("Save");
        Button cancel = new Button("Cancel");
        save.getStyleClass().add("action-button");
        cancel.getStyleClass().add("action-button");

        VirtualizedScrollPane<InlineCssTextArea> scroll = new VirtualizedScrollPane<>(area);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox top = new HBox(10, new Label("Name:"), nameField);
        HBox actions = new HBox(10, save, cancel);
        nameField.setEditable(isOwner);

        actions.setAlignment(Pos.CENTER_RIGHT);


        VBox root = new VBox(12, top, scroll, actions);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("popup-root");
        if (!isDarkModeNow()) root.getStyleClass().add("light-mode");

        Stage popup = new Stage();
        popup.initOwner(owner);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setTitle("New note");
        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(java.util.Objects.requireNonNull(
                getClass().getResource("/views/LibraryViews/styles.css")
        ).toExternalForm());
        popup.setScene(scene);

        cancel.setOnAction(e -> popup.close());

        save.setOnAction(e -> {
            String base = nameField.getText() == null ? "" : nameField.getText().trim();
            if (base.isEmpty()) {
                UiPopups.showWarning(popup, "Name required", isDarkModeNow(), getClass());                return;
            }
            final String finalName = ensureExt(base, ".rtfx");

            Task<CourseService.CourseFileRow> t = new Task<>() {
                @Override protected CourseService.CourseFileRow call() throws Exception {
                    var codec = getDocCodec(area);
                    byte[] bytes;
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         DataOutputStream out = new DataOutputStream(baos)) {
                        codec.encode(out, area.getDocument());
                        out.flush();
                        bytes = baos.toByteArray();
                    }

                    java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("notes-");
                    java.nio.file.Path p = dir.resolve(finalName);
                    java.nio.file.Files.write(p, bytes);

                    courseService.uploadCourseFile(courseId, p.toFile());
                    List<CourseService.CourseFileRow> after = courseService.listCourseFiles(courseId);
                    CourseService.CourseFileRow best = null;
                    for (CourseService.CourseFileRow row : after) {
                        if (row.originalName() != null && row.originalName().equalsIgnoreCase(finalName)) {
                            if (best == null || row.id() > best.id()) best = row;
                        }
                    }
                    if (best == null) throw new IllegalStateException("Uploaded note not found: " + finalName);
                    return best;
                }
            };

            t.setOnSucceeded(ev2 -> {
                popup.close();
                refreshFiles();
                openNoteEditor(t.getValue());
            });
            t.setOnFailed(ev2 -> {
                t.getException().printStackTrace();
                UiPopups.showError(popup, "Save failed", isDarkModeNow(), getClass());            });

            Thread th = new Thread(t, "save-new-note");
            th.setDaemon(true);
            th.start();
        });

        popup.showAndWait();
    }

    private String makeSnippet(String s, int maxChars) {
        if (s == null) return "";
        String cleaned = s.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (cleaned.length() <= maxChars) return cleaned;
        return cleaned.substring(0, maxChars).trim() + "...";
    }

    private void openNoteEditor(CourseService.CourseFileRow f) {
        Stage owner = (Stage) root.getScene().getWindow();

        TextField nameField = new TextField(stripExt(f.originalName() == null ? "note.rtfx" : f.originalName()));
        nameField.setPromptText("Note name");
        nameField.setEditable(isOwner);

        final long[] currentFileId = { f.id() };
        final String[] currentOriginalName = { f.originalName() == null ? "note.rtfx" : f.originalName() };

        InlineCssTextArea area = new InlineCssTextArea();
        area.getStyleClass().add("note-editor");
        area.setWrapText(true);

        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().setAll(javafx.scene.text.Font.getFamilies());
        fontBox.setValue(javafx.scene.text.Font.getDefault().getFamily());
        if (!fontBox.getItems().contains(fontBox.getValue())) {
            fontBox.setValue(javafx.scene.text.Font.getDefault().getFamily());
        }

        Spinner<Integer> sizeSpinner = new Spinner<>(8, 72, 14);
        sizeSpinner.setEditable(true);

        Button alignLeft    = makeIconBtn("/align-left.png",                   "Align left");
        Button alignCenter  = makeIconBtn("/align-center.png",                 "Align center");
        Button alignRight   = makeIconBtn("/align-right.png",                  "Align right");
        Button alignJustify = makeIconBtn("/justify.png",                      "Justify");
        Button findBtn      = makeIconBtn("/search-interface-symbol-light.png","Find");

        Button save = new Button("Save");
        Button saveAsNew = new Button("Save as New");
        Button download = new Button("Download PDF");
        Button rename = new Button("Rename");
        Button delete = new Button("Delete");
        Button close = new Button("Close");
        save.getStyleClass().add("action-button");
        saveAsNew.getStyleClass().add("action-button");
        download.getStyleClass().add("action-button");
        rename.getStyleClass().add("action-button");
        delete.getStyleClass().add("action-button");
        close.getStyleClass().add("action-button");

        final java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean dirty = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean loading = new java.util.concurrent.atomic.AtomicBoolean(true);

        Runnable setBusyUi = () -> {
            boolean b = busy.get();
            save.setDisable(b);
            saveAsNew.setDisable(b);
            rename.setDisable(b);
            delete.setDisable(b);
            download.setDisable(b);
        };

        final String[] currentTextCss = { sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue())) };
        final String[] currentParCss = { sanitizeCss(alignCss("left")) };
        final javafx.scene.paint.Color[] currentColor = { null };

        Runnable applyFontToSelectionOrTyping = () -> {
            currentTextCss[0] = sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue(), currentColor[0]));
            int start = area.getSelection().getStart();
            int end = area.getSelection().getEnd();
            if (start != end) {
                area.setStyle(start, end, currentTextCss[0]);
                return;
            }
            int caret = area.getCaretPosition();
            if (area.getLength() == 0) {
                area.setStyle(0, 0, currentTextCss[0]);
                return;
            }
            if (caret < area.getLength()) {
                area.setStyle(caret, caret + 1, currentTextCss[0]);
            } else {
                area.setStyle(Math.max(0, caret - 1), caret, currentTextCss[0]);
            }
        };

        Runnable applyParagraphStyleToSelection = () -> {
            int startOffset = area.getSelection().getStart();
            int endOffset = area.getSelection().getEnd();
            int startPar = area.offsetToPosition(
                    startOffset,
                    org.fxmisc.richtext.model.TwoDimensional.Bias.Forward
            ).getMajor();
            int endPar = area.offsetToPosition(
                    endOffset,
                    org.fxmisc.richtext.model.TwoDimensional.Bias.Backward
            ).getMajor();
            if (endPar < startPar) {
                int t = startPar;
                startPar = endPar;
                endPar = t;
            }
            for (int p = startPar; p <= endPar; p++) {
                area.setParagraphStyle(p, currentParCss[0]);
            }
        };

        alignLeft.setOnAction(e -> {
            currentParCss[0] = sanitizeCss(alignCss("left"));
            applyParagraphStyleToSelection.run();
        });
        alignCenter.setOnAction(e -> {
            currentParCss[0] = sanitizeCss(alignCss("center"));
            applyParagraphStyleToSelection.run();
        });
        alignRight.setOnAction(e -> {
            currentParCss[0] = sanitizeCss(alignCss("right"));
            applyParagraphStyleToSelection.run();
        });
        alignJustify.setOnAction(e -> {
            currentParCss[0] = sanitizeCss(alignCss("justify"));
            applyParagraphStyleToSelection.run();
        });

        fontBox.setOnAction(e -> applyFontToSelectionOrTyping.run());
        sizeSpinner.valueProperty().addListener((obs, o, n) -> applyFontToSelectionOrTyping.run());
        area.plainTextChanges().subscribe(ch -> {
            String inserted = ch.getInserted();
            if (inserted == null || inserted.isEmpty()) return;
            int from = ch.getPosition();
            int to = from + inserted.length();
            area.setStyle(from, to, currentTextCss[0]);
            if (!loading.get()) dirty.set(true);
        });

        // ── Caret position listener: sync toolbar to the style under the caret ──────
        area.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (loading.get()) return;
            int pos = newPos.intValue();
            int len = area.getLength();
            if (len == 0) return;
            // Read style of the character just before caret (what the user "just typed")
            int readPos = (pos > 0) ? Math.min(pos - 1, len - 1) : 0;
            String css = area.getStyleOfChar(readPos);
            if (css == null || css.isBlank()) return;

            // Sync font family
            String fam = extractFontFamily(css);
            if (fam != null && fontBox.getItems().contains(fam)) {
                fontBox.setValue(fam);
            }
            // Sync font size
            Integer sz = extractFontSizePx(css);
            if (sz != null) {
                sizeSpinner.getValueFactory().setValue(sz);
            }
            // Sync color
            java.util.regex.Matcher cm = java.util.regex.Pattern
                    .compile("-fx-fill:\s*(#[0-9a-fA-F]{6})").matcher(css);
            if (cm.find()) {
                currentColor[0] = javafx.scene.paint.Color.web(cm.group(1));
            } else {
                currentColor[0] = null;
            }
            // Update the active CSS so next typed character inherits this style
            currentTextCss[0] = sanitizeCss(css);
        });

        final String initialName = nameField.getText();
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!loading.get() && !java.util.Objects.equals(o, n)) dirty.set(true);
        });

        findBtn.setOnAction(e -> {
            Optional<String> r = UiPopups.prompt(
                    owner,
                    "Find",
                    "Text:",
                    "",
                    isDarkModeNow(),
                    getClass()
            );

            if (r.isEmpty()) return;

            String q = r.get();
            if (q == null || q.isBlank()) return;

            String hay = area.getText() == null ? "" : area.getText();
            int start = Math.max(area.getCaretPosition(), 0);
            int idx = hay.indexOf(q, start);
            if (idx < 0 && start > 0) idx = hay.indexOf(q);

            if (idx >= 0) {
                area.requestFocus();
                area.selectRange(idx, idx + q.length());
            } else {
                UiPopups.showInfo(owner, "Not found", isDarkModeNow(), getClass());
            }
        });
        // Word-style font color: "A" label + swatch bar underneath
        javafx.scene.paint.Color[] swatchColor = { javafx.scene.paint.Color.RED };
        javafx.scene.shape.Rectangle colorSwatch = new javafx.scene.shape.Rectangle(16, 4);
        colorSwatch.setFill(swatchColor[0]);
        colorSwatch.setArcWidth(2); colorSwatch.setArcHeight(2);

        Label colorIcon = new Label("A");
        colorIcon.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox colorBtnContent = new VBox(1, colorIcon, colorSwatch);
        colorBtnContent.setAlignment(Pos.CENTER);

        Button colorBtn = new Button();
        colorBtn.setGraphic(colorBtnContent);
        colorBtn.setTooltip(new Tooltip("Font color  |  right-click to pick"));
        colorBtn.getStyleClass().add("action-button");
        colorBtn.setStyle("-fx-padding: 4 8 4 8;");

        if (!isOwner) {
            fontBox.setDisable(true);
            sizeSpinner.setDisable(true);
            alignLeft.setDisable(true);
            alignCenter.setDisable(true);
            alignRight.setDisable(true);
            alignJustify.setDisable(true);
            colorBtn.setDisable(true);
        }

        // Left-click: apply current swatch color to selection
        colorBtn.setOnAction(ev -> {
            currentColor[0] = swatchColor[0];
            currentTextCss[0] = sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue(), currentColor[0]));
            int s = area.getSelection().getStart(), en = area.getSelection().getEnd();
            if (s != en) area.setStyle(s, en, currentTextCss[0]);
            area.requestFocus();
        });

        // Right-click: open color picker popup
        colorBtn.setOnContextMenuRequested(ev -> {
            javafx.scene.control.ColorPicker picker = new javafx.scene.control.ColorPicker(swatchColor[0]);
            picker.setPrefWidth(210);

            Label pickerLabel = new Label("Font color");
            pickerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Button applyColorBtn = new Button("Apply");
            applyColorBtn.getStyleClass().add("action-button");
            Button clearColorBtn = new Button("Clear color");
            clearColorBtn.getStyleClass().add("action-button");

            HBox colorBtns = new HBox(8, applyColorBtn, clearColorBtn);
            colorBtns.setAlignment(Pos.CENTER_RIGHT);

            VBox colorContent = new VBox(10, pickerLabel, picker, colorBtns);
            colorContent.setPadding(new Insets(14));

            Stage colorStage = UiPopups.buildModalNoTitleBar(
                    (Stage) colorBtn.getScene().getWindow(),
                    colorContent, 260, 165, false, isDarkModeNow(), getClass()
            );

            applyColorBtn.setOnAction(e2 -> {
                swatchColor[0] = picker.getValue();
                colorSwatch.setFill(swatchColor[0]);
                currentColor[0] = swatchColor[0];
                currentTextCss[0] = sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue(), currentColor[0]));
                int s = area.getSelection().getStart(), en = area.getSelection().getEnd();
                if (s != en) area.setStyle(s, en, currentTextCss[0]);
                colorStage.close();
                area.requestFocus();
            });

            clearColorBtn.setOnAction(e2 -> {
                swatchColor[0] = javafx.scene.paint.Color.RED;
                colorSwatch.setFill(swatchColor[0]);
                currentColor[0] = null;
                currentTextCss[0] = sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue(), null));
                colorStage.close();
                area.requestFocus();
            });

            colorStage.showAndWait();
        });

        HBox topRow = new HBox(
                10,
                new Label("Name:"), nameField,
                new Region(),
                new Label("Font:"), fontBox,
                new Label("Size:"), sizeSpinner,
                alignLeft, alignCenter, alignRight, alignJustify,
                colorBtn,
                findBtn
        );
        HBox.setHgrow(topRow.getChildren().get(2), Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions;
        if (isOwner) {
            actions = new HBox(10, save, saveAsNew, download, rename, delete, close);
        } else {
            actions = new HBox(10, download, close);
            area.setEditable(false);   // ← read-only for non-owners
        }
        actions.setAlignment(Pos.CENTER_RIGHT);

        VirtualizedScrollPane<InlineCssTextArea> scroll = new VirtualizedScrollPane<>(area);
        VBox root = new VBox(12, topRow, scroll, actions);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("popup-root");
        if (!isDarkModeNow()) root.getStyleClass().add("light-mode");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Stage popup = UiPopups.buildModalNoTitleBar(owner, root, 1150, 740, true, isDarkModeNow(), getClass());
        final boolean isNewNote = currentFileId[0] <= 0;
        if (isNewNote) {
            rename.setDisable(true);
            delete.setDisable(true);
            download.setDisable(true);
            loading.set(false);
        }

        close.setOnAction(e -> popup.fireEvent(
                new javafx.stage.WindowEvent(popup, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)
        ));
        if (!isNewNote) {
            Task<Void> loadTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    byte[] data = courseService.loadCourseFileBytes(currentFileId[0]);
                    String name = (currentOriginalName[0] == null ? "" : currentOriginalName[0]).toLowerCase();
                    Platform.runLater(() -> {
                        try {
                            if (name.endsWith(".rtfx")) {
                                org.fxmisc.richtext.model.Codec<StyledDocument<String, String, String>> codec =
                                        getDocCodec(area);
                                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
                                    var doc = codec.decode(in);
                                    if (doc != null) {
                                        area.replace(0, area.getLength(), doc);
                                        if (area.getLength() > 0) {
                                            String css = area.getStyleAtPosition(0);
                                            if (css != null && !css.trim().isEmpty()) {
                                                currentTextCss[0] = css.trim();
                                                String fam = extractFontFamily(css);
                                                Integer sz = extractFontSizePx(css);
                                                if (fam != null && fontBox.getItems().contains(fam)) {
                                                    fontBox.setValue(fam);
                                                }
                                                if (sz != null) {
                                                    sizeSpinner.getValueFactory().setValue(sz);
                                                }
                                                // Restore color
                                                java.util.regex.Matcher ccm = java.util.regex.Pattern
                                                        .compile("-fx-fill:\\s*(#[0-9a-fA-F]{6})").matcher(css);
                                                if (ccm.find()) {
                                                    currentColor[0] = javafx.scene.paint.Color.web(ccm.group(1));
                                                } else {
                                                    currentColor[0] = null;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                                area.replaceText(text);
                                area.setStyle(0, area.getLength(), currentTextCss[0]);
                                for (int p = 0; p < area.getParagraphs().size(); p++) {
                                    area.setParagraphStyle(p, currentParCss[0]);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            area.replaceText("Failed to load note.");
                        } finally {
                            loading.set(false);
                        }
                    });
                    return null;
                }
            };

            loadTask.setOnFailed(e -> {
                loadTask.getException().printStackTrace();
                Platform.runLater(() -> {
                    loading.set(false);
                    UiPopups.showError(popup, "Failed to load note", isDarkModeNow(), getClass());                });
            });

            Thread lt = new Thread(loadTask, "load-note-" + currentFileId[0]);
            lt.setDaemon(true);
            lt.start();
        }

        save.setOnAction(e -> {
            if (!busy.compareAndSet(false, true)) return;
            Platform.runLater(setBusyUi);

            String base = nameField.getText() == null ? "" : nameField.getText().trim();
            if (base.isEmpty()) {
                busy.set(false);
                Platform.runLater(setBusyUi);
                UiPopups.showWarning(popup, "Name required", isDarkModeNow(), getClass());

                return;
            }

            final String finalFileName = ensureExt(base, ".rtfx");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    var codec = getDocCodec(area);
                    byte[] bytes;
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         DataOutputStream out = new DataOutputStream(baos)) {
                        codec.encode(out, area.getDocument());
                        out.flush();
                        bytes = baos.toByteArray();
                    }

                    java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("notes-");
                    java.nio.file.Path p = dir.resolve(finalFileName);
                    java.nio.file.Files.write(p, bytes);

                    long oldId = currentFileId[0];

                    courseService.uploadCourseFile(courseId, p.toFile());
                    java.util.List<CourseService.CourseFileRow> after = courseService.listCourseFiles(courseId);
                    long newId = -1;
                    for (CourseService.CourseFileRow row : after) {
                        if (row.originalName() != null &&
                                row.originalName().equalsIgnoreCase(finalFileName)) {
                            if (row.id() > newId) newId = row.id();
                        }
                    }
                    if (newId <= 0) newId = oldId;

                    if (oldId > 0 && newId != oldId) {
                        try { courseService.deleteCourseFile(oldId); } catch (Exception ignored) {}
                    }
                    currentFileId[0] = newId;
                    currentOriginalName[0] = finalFileName;
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                busy.set(false);
                setBusyUi.run();
                dirty.set(false);
                if (currentFileId[0] > 0) {
                    rename.setDisable(false);
                    delete.setDisable(false);
                    download.setDisable(false);
                }
                refreshFiles();
            });

            task.setOnFailed(ev -> {
                busy.set(false);
                setBusyUi.run();
                task.getException().printStackTrace();
                UiPopups.showError(popup, "Save failed", isDarkModeNow(), getClass());            });

            Thread th = new Thread(task, "save-note");
            th.setDaemon(true);
            th.start();
        });

        saveAsNew.setOnAction(e -> {
            if (!busy.compareAndSet(false, true)) return;
            Platform.runLater(setBusyUi);

            Task<String> suggestTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    String baseNow = nameField.getText() == null ? "" : nameField.getText().trim();
                    String base = baseNow.isBlank() ? stripExt(currentOriginalName[0]) : baseNow;
                    java.util.List<CourseService.CourseFileRow> files =
                            courseService.listCourseFiles(courseId);
                    return suggestNextCopyName(base, files);
                }
            };

            suggestTask.setOnSucceeded(ev -> {
                busy.set(false);
                setBusyUi.run();
                String suggested = suggestTask.getValue();
                TextInputDialog d = new TextInputDialog(suggested);
                d.initOwner(popup);
                d.setTitle("Save as new");
                d.setHeaderText(null);
                d.setContentText("New name:");
                UiPopups.styleDialog(d, isDarkModeNow(), getClass());
                Optional<String> r = d.showAndWait();

                if (r.isEmpty()) return;
                String newBase = r.get().trim();
                if (newBase.isEmpty()) {
                    UiPopups.showWarning(popup, "Name required", isDarkModeNow(), getClass());                    return;
                }
                final String newFileName = ensureExt(newBase, ".rtfx");
                if (!busy.compareAndSet(false, true)) return;
                setBusyUi.run();

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        var codec = getDocCodec(area);
                        byte[] bytes;
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                             DataOutputStream out = new DataOutputStream(baos)) {
                            codec.encode(out, area.getDocument());
                            out.flush();
                            bytes = baos.toByteArray();
                        }
                        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("notes-");
                        java.nio.file.Path p = dir.resolve(newFileName);
                        java.nio.file.Files.write(p, bytes);
                        courseService.uploadCourseFile(courseId, p.toFile());
                        return null;
                    }
                };

                task.setOnSucceeded(e2 -> {
                    busy.set(false);
                    setBusyUi.run();
                    nameField.setText(stripExt(newFileName));
                    refreshFiles();
                });

                task.setOnFailed(e2 -> {
                    busy.set(false);
                    setBusyUi.run();
                    task.getException().printStackTrace();
                    UiPopups.showError(popup, "Save as new failed", isDarkModeNow(), getClass());                });

                Thread th = new Thread(task, "save-as-new-note");
                th.setDaemon(true);
                th.start();
            });

            suggestTask.setOnFailed(ev -> {
                busy.set(false);
                setBusyUi.run();
                suggestTask.getException().printStackTrace();
                UiPopups.showError(popup, "Could not suggest name", isDarkModeNow(), getClass());            });

            Thread th = new Thread(suggestTask, "suggest-save-as-name");
            th.setDaemon(true);
            th.start();
        });

        download.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Export PDF");
            String baseName = nameField.getText() == null ? "note" : nameField.getText().trim();
            if (baseName.endsWith(".rtfx")) {
                baseName = baseName.substring(0, baseName.length() - 5);
            }
            fc.setInitialFileName(baseName + ".pdf");
            File out = fc.showSaveDialog(popup);
            if (out == null) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    byte[] pdfBytes = exportNoteToPdf(area);
                    java.nio.file.Files.write(out.toPath(), pdfBytes);
                    return null;
                }
            };

            task.setOnSucceeded(ev -> UiPopups.showInfo(popup, "PDF exported successfully", isDarkModeNow(), getClass()));
            task.setOnFailed(ev -> {
                task.getException().printStackTrace();
                UiPopups.showError(popup, "PDF export failed", isDarkModeNow(), getClass());            });

            Thread th = new Thread(task, "export-pdf");
            th.setDaemon(true);
            th.start();
        });

        rename.setOnAction(e -> {
            if (busy.get()) return;

            String currentBase = stripExt(currentOriginalName[0]);
            String initial = (currentBase == null || currentBase.isBlank()) ? "note" : currentBase;

            Optional<String> r = UiPopups.prompt(
                    popup,
                    "Rename note",
                    "New name:",
                    initial,
                    isDarkModeNow(),
                    getClass()
            );

            if (r.isEmpty()) return;

            String newBase = r.get().trim();
            if (newBase.isEmpty()) {
                UiPopups.showWarning(popup, "Name required", isDarkModeNow(), getClass());
                return;
            }

            String newFullName = ensureExt(newBase, ".rtfx");
            try {
                courseService.renameCourseFile(currentFileId[0], newFullName);
                nameField.setText(newBase);
                currentOriginalName[0] = newFullName;
                refreshFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Rename failed", isDarkModeNow(), getClass());
            }
        });


        delete.setOnAction(e -> {
            if (busy.get()) return;

            if (!UiPopups.confirm(popup, "Delete this note?", isDarkModeNow(), getClass())) return;

            try {
                courseService.deleteCourseFile(currentFileId[0]);
                popup.close();
                refreshFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                UiPopups.showError(popup, "Delete failed", isDarkModeNow(), getClass());
            }
        });


        popup.setOnCloseRequest(ev -> {
            if (busy.get()) {
                ev.consume();
                return;
            }
            if (!dirty.get()) return;

            ev.consume();

            CloseChoice c = askSaveBeforeClose(popup);

            if (c == CloseChoice.CANCEL) return;
            if (c == CloseChoice.DONT) {
                popup.close();
                return;
            }

            save.fire();
        });


        popup.showAndWait();
    }




    private String buildCss(String family, Integer size) {
        String fam = (family == null || family.isBlank()) ? javafx.scene.text.Font.getDefault().getFamily() : family;
        int sz = (size == null ? 14 : size);
        return "-fx-font-family: '" + fam.replace("'", "") + "'; -fx-font-size: " + sz + "px;";
    }

    private static String sanitizeCss(String css) {
        if (css == null) return "";
        return css.replace('\u0000', ' ').trim();
    }

    private static String buildTextCss(String family, Integer size) {
        return buildTextCss(family, size, null);
    }

    private static String buildTextCss(String family, Integer size, javafx.scene.paint.Color color) {
        String fam = (family == null || family.isBlank())
                ? javafx.scene.text.Font.getDefault().getFamily()
                : family;
        int sz = (size == null ? 14 : size);
        String css = "-fx-font-family: '" + fam.replace("'", "") + "'; -fx-font-size: " + sz + "px;";
        if (color != null) {
            css += " -fx-fill: " + toWebHex(color) + ";";
        }
        return css;
    }

    private static String toWebHex(javafx.scene.paint.Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private static String alignCss(String align) {
        return "-fx-text-alignment: " + align + ";";
    }

    // ── PDF export helpers ────────────────────────────────────────────────────

    /** Represents a single styled run of text within a paragraph. */
    private static class TextRun {
        final String text;
        final String fontFamily; // null → Helvetica fallback
        final float  fontSize;
        final boolean bold, italic;
        final float[] rgb;   // null → black
        TextRun(String t, String ff, float sz, boolean b, boolean i, float[] c) {
            text = t; fontFamily = ff; fontSize = sz; bold = b; italic = i; rgb = c;
        }
    }

    /** Parse inline CSS into a TextRun (text content supplied separately). */
    private static TextRun parseSegmentCss(String text, String css) {
        float size = 12f;
        boolean bold = false, italic = false;
        float[] rgb = null;
        String fontFamily = null;

        if (css != null && !css.isBlank()) {
            // font-family (quoted form: -fx-font-family: 'Arial';)
            java.util.regex.Matcher fm = java.util.regex.Pattern
                    .compile("-fx-font-family:\\s*'([^']+)'").matcher(css);
            if (fm.find()) {
                fontFamily = fm.group(1).trim();
            } else {
                java.util.regex.Matcher fm2 = java.util.regex.Pattern
                        .compile("-fx-font-family:\\s*([^;]+)").matcher(css);
                if (fm2.find()) fontFamily = fm2.group(1).replace("\"","").trim();
            }

            // font-size
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("-fx-font-size:\\s*([0-9.]+)px").matcher(css);
            if (m.find()) size = Float.parseFloat(m.group(1));

            // bold / italic via -fx-font-weight / -fx-font-style
            if (css.contains("-fx-font-weight: bold") || css.contains("-fx-font-weight:bold"))
                bold = true;
            if (css.contains("-fx-font-style: italic") || css.contains("-fx-font-style:italic"))
                italic = true;

            // color from -fx-fill: #rrggbb
            java.util.regex.Matcher cm = java.util.regex.Pattern
                    .compile("-fx-fill:\\s*(#[0-9a-fA-F]{6})").matcher(css);
            if (cm.find()) {
                String hex = cm.group(1);
                rgb = new float[]{
                        Integer.parseInt(hex.substring(1,3),16)/255f,
                        Integer.parseInt(hex.substring(3,5),16)/255f,
                        Integer.parseInt(hex.substring(5,7),16)/255f
                };
            }
        }
        return new TextRun(text, fontFamily, size, bold, italic, rgb);
    }

    /**
     * Resolve the best available PDF font for the given family + bold/italic flags.
     * Tries to embed the system TrueType font first; falls back to Helvetica variants.
     */
    private static final java.util.Map<String, org.apache.pdfbox.pdmodel.font.PDFont> FONT_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static org.apache.pdfbox.pdmodel.font.PDFont resolvePdfFont(
            org.apache.pdfbox.pdmodel.PDDocument doc, String family, boolean bold, boolean italic) {

        // Build a cache key
        String key = (family == null ? "Helvetica" : family) + "|" + bold + "|" + italic;
        if (FONT_CACHE.containsKey(key)) return FONT_CACHE.get(key);

        // Try to find a matching TrueType font file on the system
        if (family != null && !family.isBlank()) {
            try {
                // JavaFX Font lookup → get the actual file path via AWT
                java.awt.Font[] awtFonts = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getAllFonts();
                int awtStyle = (bold ? java.awt.Font.BOLD : 0) | (italic ? java.awt.Font.ITALIC : 0);
                java.awt.Font best = null;
                for (java.awt.Font f : awtFonts) {
                    if (f.getFamily().equalsIgnoreCase(family) && f.getStyle() == awtStyle) {
                        best = f; break;
                    }
                }
                // Fallback: any font of that family
                if (best == null) {
                    for (java.awt.Font f : awtFonts) {
                        if (f.getFamily().equalsIgnoreCase(family)) { best = f; break; }
                    }
                }
                if (best != null) {
                    // Derive and stream the font bytes via AWT font2D
                    java.io.File fontFile = findFontFile(best.getFontName(java.util.Locale.ENGLISH));
                    if (fontFile != null && fontFile.exists()) {
                        org.apache.pdfbox.pdmodel.font.PDFont embedded =
                                org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, fontFile);
                        FONT_CACHE.put(key, embedded);
                        return embedded;
                    }
                }
            } catch (Exception ignored) { /* fall through to standard14 */ }
        }

        // Fallback: standard 14 Helvetica variants
        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName name;
        if (bold && italic) name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
        else if (bold)      name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD;
        else if (italic)    name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE;
        else                name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
        org.apache.pdfbox.pdmodel.font.PDFont fallback =
                new org.apache.pdfbox.pdmodel.font.PDType1Font(name);
        FONT_CACHE.put(key, fallback);
        return fallback;
    }

    /** Try common OS font directories to find a .ttf/.otf file matching the AWT font name. */
    private static java.io.File findFontFile(String awtFontName) {
        String[] dirs = {
                System.getProperty("user.home") + "/AppData/Local/Microsoft/Windows/Fonts",
                "C:/Windows/Fonts",
                "/usr/share/fonts",
                "/Library/Fonts",
                System.getProperty("user.home") + "/Library/Fonts"
        };
        // Normalise: "Arial Bold" → "arialbd", try common patterns
        String nameLower = awtFontName.toLowerCase(java.util.Locale.ENGLISH).replace(" ", "");
        for (String dir : dirs) {
            java.io.File d = new java.io.File(dir);
            if (!d.isDirectory()) continue;
            for (java.io.File f : d.listFiles() != null ? d.listFiles() : new java.io.File[0]) {
                String fn = f.getName().toLowerCase(java.util.Locale.ENGLISH);
                if ((fn.endsWith(".ttf") || fn.endsWith(".otf")) &&
                        (fn.replace("-","").replace("_","").startsWith(nameLower.substring(0, Math.min(4, nameLower.length()))))) {
                    return f;
                }
            }
        }
        return null;
    }

    /** Convenience overload used by measureWidth / lineWidth helpers (no doc needed). */
    private static org.apache.pdfbox.pdmodel.font.PDFont resolvePdfFont(boolean bold, boolean italic) {
        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName name;
        if (bold && italic) name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
        else if (bold)      name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD;
        else if (italic)    name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE;
        else                name = org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(name);
    }

    /**
     * Collect all styled segments for a paragraph.
     * InlineCssTextArea stores per-character style; we merge consecutive chars
     * that share the same CSS into runs.
     */
    private static List<TextRun> buildRunsForParagraph(
            org.fxmisc.richtext.InlineCssTextArea area, int parIndex) {

        List<TextRun> runs = new java.util.ArrayList<>();

        // Compute the absolute start offset of this paragraph in the document
        int parStart = 0;
        for (int i = 0; i < parIndex; i++) {
            parStart += area.getParagraph(i).length() + 1; // +1 for the newline
        }
        String parText = area.getParagraph(parIndex).getText();
        int parLen = parText.length();

        if (parLen == 0) return runs;

        // Walk character by character, grouping consecutive chars with the same CSS into runs
        String currentCss = area.getStyleOfChar(parStart);
        int runStart = 0;

        for (int i = 1; i < parLen; i++) {
            String css = area.getStyleOfChar(parStart + i);
            if (!css.equals(currentCss)) {
                runs.add(parseSegmentCss(parText.substring(runStart, i), currentCss));
                runStart = i;
                currentCss = css;
            }
        }
        // flush last run
        runs.add(parseSegmentCss(parText.substring(runStart), currentCss));

        return runs;
    }

    /**
     * Measure the pixel width of a string in points for the given font+size.
     * Falls back gracefully for chars the font can't encode.
     */
    private static float measureWidth(String text, org.apache.pdfbox.pdmodel.font.PDFont font,
                                      float size) {
        try {
            return font.getStringWidth(text) / 1000f * size;
        } catch (Exception e) {
            // approximate: 0.5 em per character
            return text.length() * size * 0.5f;
        }
    }

    /**
     * Word-wrap a list of styled runs to fit within maxWidth points,
     * returning lines where each line is itself a list of runs.
     */
    private static List<List<TextRun>> wrapRuns(List<TextRun> runs, float maxWidth) {
        List<List<TextRun>> lines = new java.util.ArrayList<>();
        List<TextRun> currentLine = new java.util.ArrayList<>();
        float currentWidth = 0;

        for (TextRun run : runs) {
            org.apache.pdfbox.pdmodel.font.PDFont font = resolvePdfFont(run.bold, run.italic);
            // split run on spaces to word-wrap
            String[] words = run.text.split("(?<= )|(?= )"); // keep spaces attached
            StringBuilder buf = new StringBuilder();
            for (String word : words) {
                float ww = measureWidth(buf + word, font, run.fontSize);
                if (currentWidth + ww > maxWidth && currentWidth > 0 && !buf.isEmpty()) {
                    // flush buf as a run on current line
                    currentLine.add(new TextRun(buf.toString(), run.fontFamily, run.fontSize, run.bold, run.italic, run.rgb));
                    lines.add(currentLine);
                    currentLine = new java.util.ArrayList<>();
                    currentWidth = 0;
                    buf = new StringBuilder();
                    // start new buf with this word (trimmed leading space)
                    word = word.stripLeading();
                }
                buf.append(word);
                currentWidth += measureWidth(word, font, run.fontSize);
            }
            if (!buf.isEmpty()) {
                currentLine.add(new TextRun(buf.toString(), run.fontFamily, run.fontSize, run.bold, run.italic, run.rgb));
            }
        }
        if (!currentLine.isEmpty()) lines.add(currentLine);
        return lines;
    }

    /** Compute the total width of a wrapped line in points. */
    private static float lineWidth(List<TextRun> line) {
        float w = 0;
        for (TextRun r : line)
            w += measureWidth(r.text, resolvePdfFont(r.bold, r.italic), r.fontSize);
        return w;
    }

    /** Tallest font size on the line — used as line leading. */
    private static float lineLeading(List<TextRun> line) {
        float max = 12f;
        for (TextRun r : line) if (r.fontSize > max) max = r.fontSize;
        return max * 1.35f;
    }

    private byte[] exportNoteToPdf(org.fxmisc.richtext.InlineCssTextArea area) throws Exception {
        FONT_CACHE.clear(); // fresh per export so fonts are embedded in this document
        org.apache.pdfbox.pdmodel.PDDocument pdfDoc = new org.apache.pdfbox.pdmodel.PDDocument();

        float margin     = 50f;
        float pageH      = new org.apache.pdfbox.pdmodel.PDPage().getMediaBox().getHeight();
        float pageW      = new org.apache.pdfbox.pdmodel.PDPage().getMediaBox().getWidth();
        float usableW    = pageW - 2 * margin;
        float yStart     = pageH - margin;

        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        pdfDoc.addPage(page);
        org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page);

        float y = yStart;

        for (int p = 0; p < area.getParagraphs().size(); p++) {
            String parCss = area.getParagraph(p).getParagraphStyle();
            String align  = extractAlignmentFromCss(parCss);

            List<TextRun> runs = buildRunsForParagraph(area, p);

            // Empty paragraph → just advance one blank line
            if (runs.isEmpty()) {
                y -= 14.5f;
                if (y < margin) { cs.close(); page = new org.apache.pdfbox.pdmodel.PDPage(); pdfDoc.addPage(page);
                    cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page); y = yStart; }
                continue;
            }

            List<List<TextRun>> wrappedLines = wrapRuns(runs, usableW);
            if (wrappedLines.isEmpty()) wrappedLines.add(java.util.List.of(new TextRun("", null, 12f, false, false, null)));

            for (List<TextRun> line : wrappedLines) {
                float leading = lineLeading(line);
                y -= leading;

                if (y < margin) {
                    cs.close();
                    page = new org.apache.pdfbox.pdmodel.PDPage();
                    pdfDoc.addPage(page);
                    cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page);
                    y = yStart - leading;
                }

                // compute x start based on alignment
                float lw = lineWidth(line);
                float x = margin;
                if ("center".equals(align))  x = margin + Math.max(0, (usableW - lw) / 2f);
                else if ("right".equals(align)) x = margin + Math.max(0, usableW - lw);

                // draw each run on this line
                for (TextRun run : line) {
                    if (run.text.isEmpty()) continue;
                    org.apache.pdfbox.pdmodel.font.PDFont font = resolvePdfFont(pdfDoc, run.fontFamily, run.bold, run.italic);

                    // set color
                    if (run.rgb != null) {
                        cs.setNonStrokingColor(run.rgb[0], run.rgb[1], run.rgb[2]);
                    } else {
                        cs.setNonStrokingColor(0f, 0f, 0f); // black
                    }

                    // encode safely — skip chars the font can't handle
                    String safe = run.text.chars()
                            .filter(c -> {
                                try { font.encode(String.valueOf((char)c)); return true; }
                                catch (Exception ex) { return false; }
                            })
                            .collect(StringBuilder::new, (sb,c) -> sb.append((char)c), StringBuilder::append)
                            .toString();

                    if (!safe.isEmpty()) {
                        cs.beginText();
                        cs.setFont(font, run.fontSize);
                        cs.newLineAtOffset(x, y);
                        cs.showText(safe);
                        cs.endText();
                    }

                    x += measureWidth(run.text, font, run.fontSize);
                }
            }
        }

        cs.close();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        pdfDoc.save(baos);
        pdfDoc.close();
        return baos.toByteArray();
    }

    private static String extractAlignmentFromCss(String css) {
        if (css == null) return "left";
        String c = css.toLowerCase();
        if (c.contains("-fx-text-alignment: center")) return "center";
        if (c.contains("-fx-text-alignment: right")) return "right";
        if (c.contains("-fx-text-alignment: justify")) return "justify";
        return "left";
    }

    private String extractAlignment(String paragraphCss) {
        if (paragraphCss == null || paragraphCss.isEmpty()) return "left";

        if (paragraphCss.contains("-fx-text-alignment: center")) return "center";
        if (paragraphCss.contains("-fx-text-alignment: right")) return "right";
        if (paragraphCss.contains("-fx-text-alignment: justify")) return "justify";

        return "left";
    }

    private float calculateXOffset(String text, org.apache.pdfbox.pdmodel.font.PDFont font,
                                   float fontSize, float pageWidth, String alignment) throws Exception {
        if (alignment.equals("left")) {
            return 0;
        }

        float textWidth = font.getStringWidth(text) / 1000 * fontSize;

        if (alignment.equals("center")) {
            return (pageWidth - textWidth) / 2;
        }

        if (alignment.equals("right")) {
            return pageWidth - textWidth;
        }

        return 0;
    }


    private List<String> wrapText(String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float width) throws Exception {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testWidth > width && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static String stripExt(String name) {
        if (name == null) return "";
        String n = name.trim();
        int dot = n.lastIndexOf('.');
        return (dot > 0 ? n.substring(0, dot) : n);
    }

    private static String ensureExt(String base, String ext) {
        String b = (base == null ? "" : base.trim());
        if (b.isEmpty()) return ext.startsWith(".") ? ("note" + ext) : ("note." + ext);
        String e = ext == null ? "" : ext.trim();
        if (e.isEmpty()) return b;
        if (!e.startsWith(".")) e = "." + e;
        return b + e;
    }

    private Button makeIconBtn(String resourcePath, String tooltip) {
        Button btn = new Button();
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null) {
                ImageView iv = new ImageView(new Image(stream));
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
            } else {
                // fallback to tooltip text if image not found
                btn.setText(tooltip);
            }
        } catch (Exception e) {
            btn.setText(tooltip);
        }
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().add("action-button");
        btn.setStyle("-fx-padding: 5 8 5 8;");
        return btn;
    }

    private static boolean isBlankCss(String css) {
        return css == null || css.trim().isEmpty();
    }

    private static String extractFontFamily(String css) {
        if (css == null) return null;
        int i = css.indexOf("-fx-font-family");
        if (i < 0) return null;
        int q1 = css.indexOf("'", i);
        int q2 = (q1 >= 0) ? css.indexOf("'", q1 + 1) : -1;
        if (q1 >= 0 && q2 > q1) return css.substring(q1 + 1, q2).trim();
        int c = css.indexOf(":", i);
        int s = css.indexOf(";", i);
        if (c >= 0 && s > c) return css.substring(c + 1, s).replace("\"", "").trim();
        return null;
    }

    private static Integer extractFontSizePx(String css) {
        if (css == null) return null;
        int i = css.indexOf("-fx-font-size");
        if (i < 0) return null;
        int c = css.indexOf(":", i);
        int s = css.indexOf("px", i);
        if (c < 0 || s < 0 || s <= c) return null;
        String num = css.substring(c + 1, s).trim().replace(";", "");
        try { return Integer.parseInt(num); } catch (Exception e) { return null; }
    }
    private String suggestNextCopyName(String base, java.util.List<CourseService.CourseFileRow> files) {
        String cleanBase = stripExt(base == null ? "" : base.trim());
        if (cleanBase.isEmpty()) cleanBase = "note";

        java.util.Set<String> existing = new java.util.HashSet<>();
        for (CourseService.CourseFileRow r : files) {
            if (r.originalName() != null) existing.add(r.originalName().toLowerCase());
        }

        String candidate0 = ensureExt(cleanBase, ".rtfx");
        if (!existing.contains(candidate0.toLowerCase())) return cleanBase;

        int n = 2;
        while (true) {
            String candBase = cleanBase + " " + n;
            String candFull = ensureExt(candBase, ".rtfx");
            if (!existing.contains(candFull.toLowerCase())) return candBase;
            n++;
        }
    }

    private String suggestNextCopyBase(String base, String ext, List<CourseService.CourseFileRow> files) {
        String clean = stripExt(base == null ? "" : base.trim());
        if (clean.isEmpty()) clean = "note";

        java.util.Set<String> existing = new java.util.HashSet<>();
        for (CourseService.CourseFileRow r : files) {
            if (r.originalName() != null) existing.add(r.originalName().toLowerCase());
        }

        String c0 = ensureExt(clean, ext);
        if (!existing.contains(c0.toLowerCase())) return clean;

        int n = 2;
        while (true) {
            String candBase = clean + " " + n;
            String candFull = ensureExt(candBase, ext);
            if (!existing.contains(candFull.toLowerCase())) return candBase;
            n++;
        }
    }

    private enum CloseChoice { SAVE, DONT, CANCEL }

    private CloseChoice askSaveBeforeClose(Stage owner) {
        Label msg = new Label("You have unsaved changes. Save before closing?");
        msg.setWrapText(true);

        Button saveBtn = new Button("Save");
        Button dontBtn = new Button("Don't Save");
        Button cancelBtn = new Button("Cancel");

        saveBtn.getStyleClass().add("action-button");
        dontBtn.getStyleClass().add("action-button");
        cancelBtn.getStyleClass().add("action-button");

        HBox buttons = new HBox(10, saveBtn, dontBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, msg, buttons);
        content.setPadding(new Insets(18));

        final CloseChoice[] result = { CloseChoice.CANCEL };

        Stage dialog = UiPopups.buildModalNoTitleBar(owner, content, 520, 180, false, isDarkModeNow(), getClass());
        saveBtn.setDefaultButton(true);
        cancelBtn.setCancelButton(true);

        saveBtn.setOnAction(e -> { result[0] = CloseChoice.SAVE; dialog.close(); });
        dontBtn.setOnAction(e -> { result[0] = CloseChoice.DONT; dialog.close(); });
        cancelBtn.setOnAction(e -> { result[0] = CloseChoice.CANCEL; dialog.close(); });

        dialog.showAndWait();
        return result[0];
    }

    private void wirePublishToggle() {
        if (publishToggle == null) return;

        publishToggle.selectedProperty().addListener((obs, was, isNow) -> {
            if (internalToggleChange.get()) return;
            if (courseId <= 0) return;

            try {
                courseService.setCoursePublished(courseId, isNow);
            } catch (Exception ex) {
                ex.printStackTrace();
                internalToggleChange.set(true);
                try { publishToggle.setSelected(was); }
                finally { internalToggleChange.set(false); }
            }
        });
    }
    private void saveAsLibraryCopy() {
        int currentUserId = Session.getInstance().getUser().getUser_id();
        Stage owner = (Stage) root.getScene().getWindow();

        TextInputDialog dialog = new TextInputDialog(courseTitle.getText() + " (copy)");
        dialog.setTitle("Save as Copy");
        dialog.setHeaderText(null);
        dialog.setContentText("Name for your copy:");
        dialog.initOwner(owner);
        UiPopups.styleDialog(dialog, isDarkModeNow(), getClass());

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String newTitle = result.get().trim();
        if (newTitle.isEmpty()) {
            UiPopups.showWarning(owner, "Name is required.", isDarkModeNow(), getClass());
            return;
        }

        try (var conn = DB.getConnection()) {
            // copy the course row
            int newCourseId;
            try (var ps = conn.prepareStatement(
                    "INSERT INTO courses (title, subjectid, userid, cover_image_path) " +
                            "SELECT ?, subjectid, ?, cover_image_path FROM courses WHERE id = ?",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, newTitle);
                ps.setInt(2, currentUserId);
                ps.setInt(3, courseId);
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new Exception("No generated key for new course");
                    newCourseId = keys.getInt(1);
                }
            }

            // increment saves on the original course
            try (var ps = conn.prepareStatement(
                    "UPDATE courses SET saves = saves + 1 WHERE id = ?")) {
                ps.setInt(1, courseId);
                ps.executeUpdate();
            }

            // insert into saved_courses so the badge shows in Courses tab
            try (var ps = conn.prepareStatement(
                    "INSERT IGNORE INTO saved_courses (user_id, course_id) VALUES (?, ?)")) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }

            // copy all files
            List<CourseService.CourseFileRow> files = courseService.listCourseFiles(courseId);
            for (CourseService.CourseFileRow f : files) {
                byte[] data = courseService.loadCourseFileBytes(f.id());
                java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("course-copy-");
                java.nio.file.Path tmp = dir.resolve(f.originalName() != null ? f.originalName() : "file");
                java.nio.file.Files.write(tmp, data);
                courseService.uploadCourseFile(newCourseId, tmp.toFile());
            }

            saveAsLibraryCopyBtn.setText("✔ Copied");
            saveAsLibraryCopyBtn.setDisable(true);
            UiPopups.showInfo(owner, "Course copied to your courses!", isDarkModeNow(), getClass());
            setOrigin(Origin.COURSES);
            setCourse(newCourseId, newTitle, courseSubject.getText());

        } catch (Exception ex) {
            ex.printStackTrace();
            UiPopups.showError(owner, "Failed to copy course.", isDarkModeNow(), getClass());
        }
    }


    // =========================================================================
    //  SUGGESTIONS — Open Library + YouTube
    // =========================================================================

    private void loadSuggestions(String title, String subject) {
        String query = (subject != null && !subject.isBlank()) ? subject : title;
        if (query == null || query.isBlank()) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                // Books
                var books = suggestionsService.fetchBooks(query, 10);
                Platform.runLater(() -> {
                    booksContainer.getChildren().clear();
                    if (!books.isEmpty()) {
                        for (var b : books) booksContainer.getChildren().add(makeBookCard(b));
                        booksSection.setVisible(true);
                        booksSection.setManaged(true);
                        wireHorizontalScroll(booksScrollPane, booksContainer);
                    }
                });

                // Videos
                var videos = suggestionsService.fetchVideos(query, 10);
                Platform.runLater(() -> {
                    videosContainer.getChildren().clear();
                    if (!videos.isEmpty()) {
                        for (var v : videos) videosContainer.getChildren().add(makeVideoCard(v));
                        videosSection.setVisible(true);
                        videosSection.setManaged(true);
                        wireHorizontalScroll(videosScrollPane, videosContainer);
                    }
                });

                return null;
            }
        };
        new Thread(task, "suggestions-loader").start();
    }

    private VBox makeBookCard(SuggestionsService.BookResult book) {
        final double W = 160, H = 220, RADIUS = 10;

        VBox card = new VBox(0);
        card.getStyleClass().add("course-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinWidth(W); card.setPrefWidth(W); card.setMaxWidth(W);
        card.setStyle("-fx-cursor: hand;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefSize(W, H); imageHolder.setMinSize(W, H); imageHolder.setMaxSize(W, H);

        // Placeholder while loading
        imageHolder.getStyleClass().add("course-card-image-placeholder");
        Label icon = new Label("📖"); icon.setStyle("-fx-font-size: 28px;");
        imageHolder.getChildren().add(icon);

        // Load image on background thread
        if (book.coverUrl() != null) {
            Task<Image> imgTask = new Task<>() {
                @Override protected Image call() throws Exception {
                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection) new java.net.URL(book.coverUrl()).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "HarmonyApp/1.0");
                    try (java.io.InputStream is = conn.getInputStream()) {
                        byte[] data = is.readAllBytes();
                        return new Image(new java.io.ByteArrayInputStream(data), W, H, false, true);
                    } finally { conn.disconnect(); }
                }
            };
            imgTask.setOnSucceeded(e -> {
                Image img = imgTask.getValue();
                if (img != null && !img.isError()) {
                    javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(W, H);
                    rect.setArcWidth(RADIUS * 2); rect.setArcHeight(RADIUS * 2);
                    rect.setFill(new javafx.scene.paint.ImagePattern(img));
                    imageHolder.getChildren().setAll(rect);
                }
            });
            new Thread(imgTask, "book-img-loader").start();
        }

        card.getChildren().add(imageHolder);

        VBox info = new VBox(2);
        info.setPadding(new Insets(8, 10, 8, 10));

        Label titleLbl = new Label(book.title());
        titleLbl.getStyleClass().add("course-title");
        titleLbl.setStyle("-fx-font-size: 11px;");
        titleLbl.setWrapText(false);
        titleLbl.setMaxWidth(W - 20);
        info.getChildren().add(titleLbl);

        if (book.author() != null) {
            Label authorLbl = new Label(book.author());
            authorLbl.getStyleClass().add("course-subtitle");
            authorLbl.setStyle("-fx-font-size: 10px;");
            authorLbl.setMaxWidth(W - 20);
            info.getChildren().add(authorLbl);
        }
        card.getChildren().add(info);

        card.setOnMouseClicked(e -> openInBrowser(book.openLibUrl()));
        return card;
    }

    private VBox makeVideoCard(SuggestionsService.VideoResult video) {
        final double W = 240, H = 135, RADIUS = 10;

        VBox card = new VBox(0);
        card.getStyleClass().add("course-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinWidth(W); card.setPrefWidth(W); card.setMaxWidth(W);
        card.setStyle("-fx-cursor: hand;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefSize(W, H); imageHolder.setMinSize(W, H); imageHolder.setMaxSize(W, H);
        imageHolder.getStyleClass().add("course-card-image-placeholder");

        // Play overlay — always visible
        Label play = new Label("▶");
        play.setStyle("-fx-font-size: 28px; -fx-text-fill: white;"
                + "-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 50;"
                + "-fx-padding: 6 10 6 14;");

        // Load thumbnail on background thread
        if (video.thumbnailUrl() != null) {
            Task<Image> imgTask = new Task<>() {
                @Override protected Image call() throws Exception {
                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection) new java.net.URL(video.thumbnailUrl()).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "HarmonyApp/1.0");
                    try (java.io.InputStream is = conn.getInputStream()) {
                        byte[] data = is.readAllBytes();
                        return new Image(new java.io.ByteArrayInputStream(data), W, H, false, true);
                    } finally { conn.disconnect(); }
                }
            };
            imgTask.setOnSucceeded(e -> {
                Image img = imgTask.getValue();
                if (img != null && !img.isError()) {
                    javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(W, H);
                    rect.setArcWidth(RADIUS * 2); rect.setArcHeight(RADIUS * 2);
                    rect.setFill(new javafx.scene.paint.ImagePattern(img));
                    imageHolder.getChildren().setAll(rect, play);
                }
            });
            new Thread(imgTask, "video-img-loader").start();
        }

        imageHolder.getChildren().add(play);
        card.getChildren().add(imageHolder);

        VBox info = new VBox(2);
        info.setPadding(new Insets(8, 10, 8, 10));

        Label titleLbl = new Label(video.title());
        titleLbl.getStyleClass().add("course-title");
        titleLbl.setStyle("-fx-font-size: 11px;");
        titleLbl.setWrapText(false);
        titleLbl.setMaxWidth(W - 20);
        info.getChildren().add(titleLbl);

        if (video.channelName() != null) {
            Label channelLbl = new Label(video.channelName());
            channelLbl.getStyleClass().add("course-subtitle");
            channelLbl.setStyle("-fx-font-size: 10px;");
            info.getChildren().add(channelLbl);
        }
        card.getChildren().add(info);

        card.setOnMouseClicked(e -> openInBrowser(video.videoUrl()));
        return card;
    }

    private void wireHorizontalScroll(ScrollPane sp, HBox container) {
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() != 0) {
                double delta  = -e.getDeltaY() * 3;
                double newVal = sp.getHvalue()
                        + delta / (container.getWidth() - sp.getViewportBounds().getWidth());
                sp.setHvalue(Math.max(0, Math.min(1, newVal)));
                e.consume();
            }
        });
    }

    private void openInBrowser(String url) {
        if (url == null) return;
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
        catch (Exception e) { e.printStackTrace(); }
    }
}