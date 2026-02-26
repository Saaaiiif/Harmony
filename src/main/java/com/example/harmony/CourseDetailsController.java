package com.example.harmony;

import com.example.harmony.interfaces.ThemeAware;
import com.example.harmony.model.SubjectRow;
import com.example.harmony.services.CourseService;
import com.example.harmony.util.UiPopups;
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

import org.fxmisc.richtext.model.StyledDocument;

public class CourseDetailsController implements ThemeAware {

    @FXML private Label courseTitle;
    @FXML private Button backBtn;
    @FXML private Button createNoteBtn;
    @FXML private TilePane filesContainer;
    @FXML private NavWheelController navWheelController;
    @FXML private Button addFilesBtn;
    @FXML private Label courseSubject;
    @FXML private ToggleButton publishToggle;
    private final CourseService courseService = new CourseService();
    private int courseId;
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
        installInlineEdit();
        refreshFiles();
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

    @FXML
    private void initialize() {
        if (navWheelController != null) navWheelController.setActiveIndex(5);
        if (navWheelController != null) navWheelController.setOnNavigate(this::handleNavigation);
        wirePublishToggle();

        backBtn.setOnAction(e -> {
            try {
                if (origin == Origin.LIBRARY) {
                    SceneTransitionUtil.changeContent(
                            "/com/example/harmony/library-layout.fxml",
                            SceneTransitionUtil.TransitionType.FADE,
                            LibraryLayoutController.class
                    );
                } else {
                    SceneTransitionUtil.changeContent(
                            "/com/example/harmony/courses-layout.fxml",
                            SceneTransitionUtil.TransitionType.FADE,
                            CoursesLayoutController.class
                    );
                }
            } catch (Exception ex) {
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
    }

    private void onAddFiles() {
        Stage owner = (Stage) filesContainer.getScene().getWindow();

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
                        "/com/example/harmony/front-layout.fxml",
                        SceneTransitionUtil.TransitionType.FADE,
                        FrontLayoutController.class
                );
                case 5 -> SceneTransitionUtil.changeContent(
                        "/com/example/harmony/courses-layout.fxml",
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
        RootLayoutController rc = SceneTransitionUtil.getRootController();
        return rc != null && rc.isDarkMode();
    }

    private void refreshFiles() {
        try {
            List<CourseService.CourseFileRow> files = courseService.listCourseFiles(courseId);
            filesContainer.getChildren().clear();

            if (files.isEmpty()) {
                Label empty = new Label("No files yet");
                empty.getStyleClass().add("empty-state-label");
                filesContainer.getChildren().add(empty);
                return;
            }

            for (CourseService.CourseFileRow f : files) {
                filesContainer.getChildren().add(makeFileCard(f));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        wrapper.getChildren().add(xBtn);
        StackPane.setAlignment(xBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(xBtn, new Insets(8));

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
        Stage owner = (Stage) filesContainer.getScene().getWindow();

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
        Stage owner = (Stage) filesContainer.getScene().getWindow();

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

        HBox actions = new HBox(10, download, rename, delete, close);
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
        Stage owner = (Stage) filesContainer.getScene().getWindow();

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
                getClass().getResource("/com/example/harmony/styles.css")
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
        Stage owner = (Stage) filesContainer.getScene().getWindow();

        TextField nameField = new TextField(stripExt(f.originalName() == null ? "note.rtfx" : f.originalName()));
        nameField.setPromptText("Note name");

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

        Button alignLeft = new Button("Left");
        Button alignCenter = new Button("Center");
        Button alignRight = new Button("Right");
        Button alignJustify = new Button("Justify");
        alignLeft.getStyleClass().add("action-button");
        alignCenter.getStyleClass().add("action-button");
        alignRight.getStyleClass().add("action-button");
        alignJustify.getStyleClass().add("action-button");

        Button findBtn = new Button("Find");
        findBtn.getStyleClass().add("action-button");

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

        Runnable applyFontToSelectionOrTyping = () -> {
            currentTextCss[0] = sanitizeCss(buildTextCss(fontBox.getValue(), sizeSpinner.getValue()));
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


        HBox topRow = new HBox(
                10,
                new Label("Name:"), nameField,
                new Region(),
                new Label("Font:"), fontBox,
                new Label("Size:"), sizeSpinner,
                alignLeft, alignCenter, alignRight, alignJustify,
                findBtn
        );
        HBox.setHgrow(topRow.getChildren().get(2), Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, save, saveAsNew, download, rename, delete, close);
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
        String fam = (family == null || family.isBlank())
                ? javafx.scene.text.Font.getDefault().getFamily()
                : family;
        int sz = (size == null ? 14 : size);
        return "-fx-font-family: '" + fam.replace("'", "") + "'; -fx-font-size: " + sz + "px;";
    }

    private static String alignCss(String align) {
        return "-fx-text-alignment: " + align + ";";
    }

    private byte[] exportNoteToPdf(org.fxmisc.richtext.InlineCssTextArea area) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument pdfDoc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        pdfDoc.addPage(page);

        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float usableWidth = page.getMediaBox().getWidth() - 2 * margin;
        float leading = 14.5f;
        float y = yStart;

        org.apache.pdfbox.pdmodel.font.PDFont font =
                new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA
                );
        float fontSize = 12f;

        org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page);

        for (int p = 0; p < area.getParagraphs().size(); p++) {
            String parText = area.getParagraph(p).getText();
            String parCss = area.getParagraph(p).getParagraphStyle();

            String align = extractAlignmentFromCss(parCss);

            List<String> lines = wrapText(parText, font, fontSize, usableWidth);
            if (lines.isEmpty()) lines = java.util.List.of("");

            for (String line : lines) {
                y -= leading;

                if (y < margin) {
                    cs.close();
                    page = new org.apache.pdfbox.pdmodel.PDPage();
                    pdfDoc.addPage(page);
                    cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page);
                    y = yStart - leading;
                }

                float lineWidth = font.getStringWidth(line) / 1000f * fontSize;
                float x = margin;

                if ("center".equals(align)) x = margin + Math.max(0, (usableWidth - lineWidth) / 2f);
                else if ("right".equals(align)) x = margin + Math.max(0, usableWidth - lineWidth);

                cs.beginText();
                cs.setFont(font, fontSize);
                cs.newLineAtOffset(x, y);
                cs.showText(line);
                cs.endText();
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


}
