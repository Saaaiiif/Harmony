package controllers.RessourceControllers;

import services.RessourceServices.AdviceApiService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import models.RessourceModels.StatutTache;
import models.RessourceModels.Tache;
import services.RessourceServices.TacheService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class KanbanViewController implements Initializable {

    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;
    @FXML private VBox todoCards;
    @FXML private VBox doingCards;
    @FXML private VBox doneCards;
    @FXML private Label labelConseil;
    @FXML private StackPane dialogOverlay;

    private final TacheService tacheService = new TacheService();
    private final AdviceApiService adviceApiService = new AdviceApiService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private TextField fieldNom;
    private DatePicker fieldDeadline;
    private TextArea fieldNotes;
    private ComboBox<StatutTache> fieldStatut;
    private Label dialogTitleLabel;
    private Button dialogConfirmButton;
    private Label validationErrorLabel;
    private VBox taskDialogBox;
    private VBox confirmDialogBox;
    private Tache taskBeingEdited;
    private VBox cardBeingEdited;
    private Tache taskToDelete;
    private VBox cardToDelete;
    private StatutTache addDialogDefaultStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonneDrop(todoCards);
        configurerColonneDrop(doingCards);
        configurerColonneDrop(doneCards);
        configurerColonneDrop(todoColumn);
        configurerColonneDrop(doingColumn);
        configurerColonneDrop(doneColumn);
        setupEmptyColumnClick();
        chargerTaches();
        loadAdvice();
    }

    private void setupEmptyColumnClick() {
        setupColumnClick(todoColumn, StatutTache.A_FAIRE);
        setupColumnClick(doingColumn, StatutTache.EN_COURS);
        setupColumnClick(doneColumn, StatutTache.TERMINEE);
    }

    private void setupColumnClick(VBox column, StatutTache defaultStatus) {
        column.setOnMouseClicked(event -> {
            if (!isClickOnTaskCard(event)) {
                showAddTaskDialog(defaultStatus);
            }
        });
    }

    private boolean isClickOnTaskCard(MouseEvent event) {
        Node target = event.getPickResult().getIntersectedNode();
        while (target != null) {
            if (target.getStyleClass().contains("task-card") || target.getUserData() instanceof Tache) {
                return true;
            }
            target = target.getParent();
        }
        return false;
    }

    private void buildTaskDialog() {
        dialogTitleLabel = new Label("Nouvelle Tâche");
        dialogTitleLabel.getStyleClass().add("dialog-title");

        VBox nomBox = new VBox(6);
        Label nomLabel = new Label("NOM");
        nomLabel.getStyleClass().add("field-label");
        fieldNom = new TextField();
        fieldNom.setPromptText("Nom de la tâche...");
        fieldNom.getStyleClass().add("modern-input");
        fieldNom.setMaxWidth(Double.MAX_VALUE);
        nomBox.getChildren().addAll(nomLabel, fieldNom);

        VBox deadlineBox = new VBox(6);
        Label deadlineLabel = new Label("DEADLINE");
        deadlineLabel.getStyleClass().add("field-label");
        fieldDeadline = new DatePicker();
        fieldDeadline.getStyleClass().add("modern-input");
        fieldDeadline.setMaxWidth(Double.MAX_VALUE);
        deadlineBox.getChildren().addAll(deadlineLabel, fieldDeadline);

        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("NOTES");
        notesLabel.getStyleClass().add("field-label");
        fieldNotes = new TextArea();
        fieldNotes.setPromptText("Notes...");
        fieldNotes.setPrefRowCount(3);
        fieldNotes.setWrapText(true);
        fieldNotes.getStyleClass().add("modern-input");
        fieldNotes.setMaxWidth(Double.MAX_VALUE);
        notesBox.getChildren().addAll(notesLabel, fieldNotes);

        VBox statutBox = new VBox(6);
        Label statutLabel = new Label("STATUT");
        statutLabel.getStyleClass().add("field-label");
        fieldStatut = new ComboBox<>();
        fieldStatut.getItems().addAll(StatutTache.A_FAIRE, StatutTache.EN_COURS, StatutTache.TERMINEE);
        fieldStatut.setConverter(new javafx.util.StringConverter<StatutTache>() {
            @Override
            public String toString(StatutTache s) {
                if (s == null) return "";
                switch (s) {
                    case A_FAIRE: return "À faire";
                    case EN_COURS: return "En cours";
                    case TERMINEE: return "Terminée";
                    default: return s.name();
                }
            }
            @Override
            public StatutTache fromString(String string) {
                if (string == null) return null;
                if ("À faire".equals(string)) return StatutTache.A_FAIRE;
                if ("En cours".equals(string)) return StatutTache.EN_COURS;
                if ("Terminée".equals(string)) return StatutTache.TERMINEE;
                return null;
            }
        });
        fieldStatut.getStyleClass().add("modern-input");
        fieldStatut.setMaxWidth(Double.MAX_VALUE);
        statutBox.getChildren().addAll(statutLabel, fieldStatut);

        validationErrorLabel = new Label();
        validationErrorLabel.getStyleClass().add("validation-error");
        validationErrorLabel.setWrapText(true);
        validationErrorLabel.setVisible(false);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> closeDialog());
        dialogConfirmButton = new Button("＋  Ajouter");
        dialogConfirmButton.getStyleClass().add("btn-primary");
        dialogConfirmButton.setOnAction(e -> {
            if (taskBeingEdited != null) handleEditTask();
            else handleAddTask();
        });
        buttons.getChildren().addAll(cancelBtn, dialogConfirmButton);

        taskDialogBox = new VBox(18);
        taskDialogBox.getStyleClass().add("task-dialog-box");
        taskDialogBox.setMaxWidth(420);
        taskDialogBox.getChildren().addAll(
            dialogTitleLabel,
            nomBox,
            deadlineBox,
            notesBox,
            statutBox,
            validationErrorLabel,
            buttons
        );
        VBox.setMargin(buttons, new Insets(8, 0, 0, 0));
    }

    private void showAddTaskDialog(StatutTache defaultStatus) {
        addDialogDefaultStatus = defaultStatus;
        taskBeingEdited = null;
        cardBeingEdited = null;
        if (taskDialogBox == null) buildTaskDialog();
        fieldNom.setText("");
        fieldDeadline.setValue(null);
        fieldNotes.setText("");
        fieldStatut.setValue(defaultStatus);
        dialogTitleLabel.setText("Nouvelle Tâche");
        dialogConfirmButton.setText("＋  Ajouter");
        validationErrorLabel.setVisible(false);
        showOverlay(taskDialogBox);
    }

    private void showEditTaskDialog(Tache task, VBox card) {
        taskBeingEdited = task;
        cardBeingEdited = card;
        addDialogDefaultStatus = null;
        if (taskDialogBox == null) buildTaskDialog();
        fieldNom.setText(task.getNom() != null ? task.getNom() : "");
        fieldDeadline.setValue(task.getDeadline() != null
            ? Instant.ofEpochMilli(task.getDeadline().getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
            : null);
        fieldNotes.setText(task.getNotes() != null ? task.getNotes() : "");
        fieldStatut.setValue(task.getStatut() != null ? task.getStatut() : StatutTache.A_FAIRE);
        dialogTitleLabel.setText("Modifier la Tâche");
        dialogConfirmButton.setText("✎  Enregistrer");
        validationErrorLabel.setVisible(false);
        showOverlay(taskDialogBox);
    }

    private void showOverlay(javafx.scene.Node content) {
        if (dialogOverlay == null) return;
        dialogOverlay.getChildren().clear();
        dialogOverlay.getChildren().add(content);
        StackPane.setAlignment(content, Pos.CENTER);
        dialogOverlay.setVisible(true);
        dialogOverlay.setMouseTransparent(false);
        dialogOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == dialogOverlay) closeDialog();
        });
    }

    @FXML
    private void closeDialog() {
        if (dialogOverlay != null) {
            dialogOverlay.getChildren().clear();
            dialogOverlay.setVisible(false);
            dialogOverlay.setMouseTransparent(true);
            dialogOverlay.setOnMouseClicked(null);
        }
        taskBeingEdited = null;
        cardBeingEdited = null;
        taskToDelete = null;
        cardToDelete = null;
    }

    private void handleAddTask() {
        String nom = fieldNom.getText() != null ? fieldNom.getText().trim() : "";
        LocalDate deadline = fieldDeadline.getValue();
        if (nom.isEmpty()) {
            validationErrorLabel.setText("Le nom est obligatoire.");
            validationErrorLabel.setVisible(true);
            return;
        }
        if (deadline == null) {
            validationErrorLabel.setText("La deadline est obligatoire.");
            validationErrorLabel.setVisible(true);
            return;
        }
        validationErrorLabel.setVisible(false);
        Tache task = new Tache();
        task.setNom(nom);
        task.setDeadline(Date.from(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        task.setNotes(fieldNotes.getText() != null ? fieldNotes.getText().trim() : null);
        task.setStatut(fieldStatut.getValue() != null ? fieldStatut.getValue() : StatutTache.A_FAIRE);
        tacheService.add(task);
        closeDialog();
        StatutTache addedStatus = task.getStatut();
        chargerTaches();
        playFadeInOnNewCard(addedStatus, nom);
    }

    private void playFadeInOnNewCard(StatutTache status, String nom) {
        VBox list = status == StatutTache.A_FAIRE ? todoCards : status == StatutTache.EN_COURS ? doingCards : doneCards;
        Platform.runLater(() -> {
            for (javafx.scene.Node n : list.getChildren()) {
                if (n instanceof VBox) {
                    Object data = ((VBox) n).getUserData();
                    if (data instanceof Tache && nom.equals(((Tache) data).getNom())) {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), n);
                        ft.setFromValue(0);
                        ft.setToValue(1);
                        ft.play();
                        break;
                    }
                }
            }
        });
    }

    private void handleEditTask() {
        if (taskBeingEdited == null) return;
        String nom = fieldNom.getText() != null ? fieldNom.getText().trim() : "";
        LocalDate deadline = fieldDeadline.getValue();
        if (nom.isEmpty()) {
            validationErrorLabel.setText("Le nom est obligatoire.");
            validationErrorLabel.setVisible(true);
            return;
        }
        if (deadline == null) {
            validationErrorLabel.setText("La deadline est obligatoire.");
            validationErrorLabel.setVisible(true);
            return;
        }
        validationErrorLabel.setVisible(false);
        taskBeingEdited.setNom(nom);
        taskBeingEdited.setDeadline(Date.from(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        taskBeingEdited.setNotes(fieldNotes.getText() != null ? fieldNotes.getText().trim() : null);
        taskBeingEdited.setStatut(fieldStatut.getValue() != null ? fieldStatut.getValue() : StatutTache.A_FAIRE);
        int editedId = taskBeingEdited.getId();
        tacheService.update(taskBeingEdited);
        closeDialog();
        chargerTaches();
        Platform.runLater(() -> {
            for (VBox list : List.of(todoCards, doingCards, doneCards)) {
                for (javafx.scene.Node n : list.getChildren()) {
                    if (n instanceof VBox) {
                        Object data = ((VBox) n).getUserData();
                        if (data instanceof Tache && ((Tache) data).getId() == editedId) {
                            playPulse(n);
                            return;
                        }
                    }
                }
            }
        });
    }

    private void playPulse(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setFromX(1.03);
        st.setToX(1.0);
        st.setFromY(1.03);
        st.setToY(1.0);
        st.play();
    }

    private void showDeleteConfirmDialog(Tache task, VBox card) {
        taskToDelete = task;
        cardToDelete = card;
        Label title = new Label("Supprimer la tâche ?");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("Cette action est irréversible.");
        subtitle.getStyleClass().add("dialog-subtitle");
        Label taskNameLabel = new Label(task.getNom() != null ? task.getNom() : "Sans titre");
        taskNameLabel.getStyleClass().add("confirm-task-name");
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> closeDialog());
        Button deleteBtn = new Button("🗑  Supprimer");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDeleteTask());
        buttons.getChildren().addAll(cancelBtn, deleteBtn);
        confirmDialogBox = new VBox(16);
        confirmDialogBox.getStyleClass().add("confirm-dialog-box");
        confirmDialogBox.setMaxWidth(360);
        confirmDialogBox.getChildren().addAll(title, subtitle, taskNameLabel, buttons);
        showOverlay(confirmDialogBox);
    }

    private void handleDeleteTask() {
        if (taskToDelete == null) return;
        VBox card = cardToDelete;
        int taskId = taskToDelete.getId();
        closeDialog();
        if (card != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(250), card);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                tacheService.delete(taskId);
                todoCards.getChildren().removeIf(n -> n == card);
                doingCards.getChildren().removeIf(n -> n == card);
                doneCards.getChildren().removeIf(n -> n == card);
            });
            ft.play();
        } else {
            tacheService.delete(taskId);
            chargerTaches();
        }
    }

    private void loadAdvice() {
        if (labelConseil == null) return;
        adviceApiService.getRandomAdvice().thenAccept(advice -> {
            Platform.runLater(() -> {
                if (labelConseil != null) labelConseil.setText("Conseil du jour : " + advice);
            });
        });
    }

    @FXML
    private void onExportTachesJson() {
        List<Tache> list = tacheService.getAll();
        org.json.JSONArray arr = new org.json.JSONArray();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        for (Tache t : list) {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("id", t.getId());
            o.put("nom", t.getNom());
            o.put("notes", t.getNotes());
            o.put("deadline", t.getDeadline() != null ? df.format(t.getDeadline()) : null);
            o.put("statut", t.getStatut() != null ? t.getStatut().name() : null);
            arr.put(o);
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les tâches");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        Window window = todoCards != null && todoCards.getScene() != null ? todoCards.getScene().getWindow() : null;
        File f = fc.showSaveDialog(window);
        if (f != null) {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                w.write(arr.toString(2));
                new Alert(Alert.AlertType.INFORMATION, "Export réussi : " + f.getAbsolutePath()).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void handleExportHtml() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les tâches en HTML");
        fc.setInitialFileName("taches_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".html");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier HTML", "*.html"));
        Window window = todoCards != null && todoCards.getScene() != null ? todoCards.getScene().getWindow() : null;
        File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            List<Tache> todo = getTasksByStatus(StatutTache.A_FAIRE);
            List<Tache> doing = getTasksByStatus(StatutTache.EN_COURS);
            List<Tache> done = getTasksByStatus(StatutTache.TERMINEE);
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html lang='fr'>\n<head>\n<meta charset='UTF-8'>\n<title>Tâches Harmony</title>\n<style>\n");
            html.append("body{font-family:'Segoe UI',sans-serif;background:#F4F3FA;padding:32px;} ");
            html.append("h1{color:#7C3AED;font-size:28px;margin-bottom:6px;} .subtitle{color:#6B7280;font-size:14px;margin-bottom:32px;} ");
            html.append(".board{display:grid;grid-template-columns:repeat(3,1fr);gap:20px;} ");
            html.append(".column{background:rgba(255,255,255,0.6);border-radius:16px;padding:20px;border:1.5px solid rgba(124,58,237,0.1);} ");
            html.append(".col-title{font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;margin-bottom:16px;} ");
            html.append(".col-todo .col-title{color:#F59E0B;} .col-doing .col-title{color:#7C3AED;} .col-done .col-title{color:#10B981;} ");
            html.append(".task-card{background:white;border-radius:12px;padding:16px;margin-bottom:12px;border:1.5px solid rgba(124,58,237,0.08);box-shadow:0 2px 8px rgba(124,58,237,0.06);} ");
            html.append(".task-name{font-size:14px;font-weight:600;color:#1E1B4B;margin-bottom:6px;} .task-date{font-size:12px;color:#7C3AED;margin-bottom:4px;} .task-notes{font-size:13px;color:#6B7280;} ");
            html.append(".badge{display:inline-block;padding:3px 10px;border-radius:999px;font-size:11px;font-weight:600;margin-bottom:8px;} ");
            html.append(".badge-todo{background:rgba(245,158,11,0.1);color:#F59E0B;} .badge-doing{background:rgba(124,58,237,0.1);color:#7C3AED;} .badge-done{background:rgba(16,185,129,0.1);color:#10B981;} ");
            html.append(".stats{display:flex;gap:16px;margin-bottom:28px;} .stat-box{background:white;border-radius:12px;padding:16px 24px;text-align:center;border:1.5px solid rgba(124,58,237,0.1);} ");
            html.append(".stat-num{font-size:28px;font-weight:700;color:#7C3AED;} .stat-label{font-size:12px;color:#6B7280;} .export-info{color:#9CA3AF;font-size:12px;margin-top:24px;text-align:right;}</style>\n</head>\n<body>\n");
            html.append("<h1>📋 Tâches Harmony</h1>\n<div class='subtitle'>Tableau Kanban exporté le ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</div>\n");
            int total = todo.size() + doing.size() + done.size();
            html.append("<div class='stats'>\n  <div class='stat-box'><div class='stat-num'>").append(total).append("</div><div class='stat-label'>Total</div></div>\n");
            html.append("  <div class='stat-box'><div class='stat-num' style='color:#F59E0B'>").append(todo.size()).append("</div><div class='stat-label'>À faire</div></div>\n");
            html.append("  <div class='stat-box'><div class='stat-num' style='color:#7C3AED'>").append(doing.size()).append("</div><div class='stat-label'>En cours</div></div>\n");
            html.append("  <div class='stat-box'><div class='stat-num' style='color:#10B981'>").append(done.size()).append("</div><div class='stat-label'>Terminées</div></div>\n</div>\n");
            html.append("<div class='board'>\n");
            appendTaskColumnHtml(html, "TODO", "col-todo", "badge-todo", "À faire", todo);
            appendTaskColumnHtml(html, "DOING", "col-doing", "badge-doing", "En cours", doing);
            appendTaskColumnHtml(html, "DONE", "col-done", "badge-done", "Terminée", done);
            html.append("</div>\n<div class='export-info'>Exporté depuis Harmony</div>\n</body>\n</html>");
            Files.writeString(file.toPath(), html.toString());
            showExportSuccess("HTML", file);
        } catch (Exception e) {
            showExportError("HTML", e);
        }
    }

    private void appendTaskColumnHtml(StringBuilder html, String colId, String colClass, String badgeClass, String badgeLabel, List<Tache> tasks) {
        html.append("  <div class='column ").append(colClass).append("'>\n    <div class='col-title'>").append(colId).append("</div>\n");
        for (Tache t : tasks) {
            html.append("    <div class='task-card'>\n      <span class='badge ").append(badgeClass).append("'>").append(badgeLabel).append("</span>\n");
            html.append("      <div class='task-name'>").append(t.getNom() != null ? t.getNom().replace("<","&lt;").replace(">","&gt;") : "").append("</div>\n");
            html.append("      <div class='task-date'>📅 ").append(getTaskDeadlineStr(t)).append("</div>\n");
            String notes = t.getNotes();
            if (notes != null && !notes.isEmpty() && !notes.equals("aucune note"))
                html.append("      <div class='task-notes'>").append(notes.replace("<","&lt;").replace(">","&gt;")).append("</div>\n");
            html.append("    </div>\n");
        }
        if (tasks.isEmpty()) html.append("    <div style='color:#9CA3AF;font-size:13px;text-align:center;padding:20px;'>Aucune tâche</div>\n");
        html.append("  </div>\n");
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les tâches en Excel");
        fc.setInitialFileName("taches_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier Excel", "*.xlsx"));
        Window window = todoCards != null && todoCards.getScene() != null ? todoCards.getScene().getWindow() : null;
        File file = fc.showSaveDialog(window);
        if (file == null) return;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet kanban = wb.createSheet("Kanban");
            XSSFCellStyle todoStyle = createColStyle(wb, new byte[]{(byte)254,(byte)243,(byte)199}, new byte[]{(byte)245,(byte)158,(byte)11});
            XSSFCellStyle doingStyle = createColStyle(wb, new byte[]{(byte)237,(byte)233,(byte)254}, new byte[]{(byte)124,(byte)58,(byte)237});
            XSSFCellStyle doneStyle = createColStyle(wb, new byte[]{(byte)209,(byte)250,(byte)229}, new byte[]{(byte)16,(byte)185,(byte)129});
            Row headerRow = kanban.createRow(0);
            String[] cols = {"À FAIRE","EN COURS","TERMINÉE"};
            XSSFCellStyle[] styles = {todoStyle, doingStyle, doneStyle};
            for (int i = 0; i < 3; i++) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(styles[i]);
                kanban.setColumnWidth(i, 30 * 256);
            }
            List<Tache> todo = getTasksByStatus(StatutTache.A_FAIRE);
            List<Tache> doing = getTasksByStatus(StatutTache.EN_COURS);
            List<Tache> done = getTasksByStatus(StatutTache.TERMINEE);
            int maxRows = Math.max(todo.size(), Math.max(doing.size(), done.size()));
            for (int i = 0; i < maxRows; i++) {
                Row row = kanban.createRow(i + 1);
                row.setHeightInPoints(50);
                if (i < todo.size()) { org.apache.poi.ss.usermodel.Cell c = row.createCell(0); c.setCellValue(buildTaskCellStr(todo.get(i))); }
                if (i < doing.size()) { org.apache.poi.ss.usermodel.Cell c = row.createCell(1); c.setCellValue(buildTaskCellStr(doing.get(i))); }
                if (i < done.size()) { org.apache.poi.ss.usermodel.Cell c = row.createCell(2); c.setCellValue(buildTaskCellStr(done.get(i))); }
            }
            XSSFSheet listSheet = wb.createSheet("Toutes les tâches");
            XSSFCellStyle listHeaderStyle = wb.createCellStyle();
            listHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)124,(byte)58,(byte)237}, null));
            listHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont whiteFont = wb.createFont();
            whiteFont.setBold(true);
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            listHeaderStyle.setFont(whiteFont);
            Row lh = listSheet.createRow(0);
            String[] listHeaders = {"Nom","Deadline","Statut","Notes"};
            for (int i = 0; i < listHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = lh.createCell(i);
                c.setCellValue(listHeaders[i]);
                c.setCellStyle(listHeaderStyle);
                listSheet.setColumnWidth(i, 22 * 256);
            }
            List<Tache> allTasks = tacheService.getAll();
            int r = 1;
            for (Tache t : allTasks) {
                Row row = listSheet.createRow(r++);
                row.createCell(0).setCellValue(t.getNom() != null ? t.getNom() : "");
                row.createCell(1).setCellValue(getTaskDeadlineStr(t));
                row.createCell(2).setCellValue(getTaskStatutStr(t));
                row.createCell(3).setCellValue(t.getNotes() != null ? t.getNotes() : "");
            }
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showExportSuccess("Excel", file);
        } catch (Exception e) {
            showExportError("Excel", e);
        }
    }

    private List<Tache> getTasksByStatus(StatutTache status) {
        return tacheService.getAll().stream().filter(t -> t.getStatut() == status).collect(Collectors.toList());
    }

    private String getTaskDeadlineStr(Tache t) {
        return t.getDeadline() != null ? DATE_FORMAT.format(t.getDeadline()) : "";
    }

    private String getTaskStatutStr(Tache t) {
        if (t.getStatut() == null) return "";
        switch (t.getStatut().name()) {
            case "A_FAIRE": return "À faire";
            case "EN_COURS": return "En cours";
            case "TERMINEE": return "Terminée";
            default: return t.getStatut().name();
        }
    }

    private String buildTaskCellStr(Tache t) {
        return (t.getNom() != null ? t.getNom() : "") + "\n📅 " + getTaskDeadlineStr(t) + "\n" + (t.getNotes() != null ? t.getNotes() : "");
    }

    private XSSFCellStyle createColStyle(XSSFWorkbook wb, byte[] bg, byte[] text) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(bg, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(text, null));
        style.setFont(font);
        return style;
    }

    private void showExportSuccess(String format, File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export réussi");
        alert.setHeaderText("✅ Export " + format + " réussi !");
        alert.setContentText("Fichier sauvegardé :\n" + file.getAbsolutePath());
        if (todoCards != null && todoCards.getScene() != null) alert.initOwner(todoCards.getScene().getWindow());
        alert.showAndWait();
    }

    private void showExportError(String format, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur d'export");
        alert.setHeaderText("❌ Erreur lors de l'export " + format);
        alert.setContentText(e.getMessage() != null ? e.getMessage() : "");
        if (todoCards != null && todoCards.getScene() != null) alert.initOwner(todoCards.getScene().getWindow());
        alert.showAndWait();
        e.printStackTrace();
    }

    private void chargerTaches() {
        todoCards.getChildren().clear();
        doingCards.getChildren().clear();
        doneCards.getChildren().clear();

        List<Tache> taches = tacheService.getAll();
        for (Tache tache : taches) {
            VBox card = buildTaskCard(tache);
            ajouterCarteDansColonne(card, tache.getStatut());
        }
    }

    private VBox buildTaskCard(Tache tache) {
        VBox card = new VBox(10);
        card.getStyleClass().add("task-card");
        card.setUserData(tache);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(tache.getNom() != null ? tache.getNom() : "Sans titre");
        nameLabel.getStyleClass().add("task-card-name");
        nameLabel.setWrapText(true);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().addAll("btn-ghost", "btn-edit");
        editBtn.setOpacity(0);

        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().addAll("btn-ghost", "btn-delete");
        deleteBtn.setOpacity(0);

        topRow.getChildren().addAll(nameLabel, editBtn, deleteBtn);

        card.setOnMouseEntered(e -> {
            editBtn.setOpacity(1);
            deleteBtn.setOpacity(1);
        });
        card.setOnMouseExited(e -> {
            editBtn.setOpacity(0);
            deleteBtn.setOpacity(0);
        });

        HBox deadlineRow = new HBox(6);
        deadlineRow.setAlignment(Pos.CENTER_LEFT);
        Label calIcon = new Label("📅");
        String deadlineText = tache.getDeadline() != null ? DATE_FORMAT.format(tache.getDeadline()) : "Pas de deadline";
        Label deadlineLabel = new Label(deadlineText);
        deadlineLabel.getStyleClass().add("task-card-deadline");
        deadlineRow.getChildren().addAll(calIcon, deadlineLabel);

        Label notesLabel = new Label();
        if (tache.getNotes() != null && !tache.getNotes().trim().isEmpty()) {
            notesLabel.setText(tache.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.getStyleClass().add("task-card-notes");
        }

        card.getChildren().add(topRow);
        card.getChildren().add(deadlineRow);
        if (tache.getNotes() != null && !tache.getNotes().trim().isEmpty()) {
            card.getChildren().add(notesLabel);
        }

        editBtn.setOnAction(e -> showEditTaskDialog(tache, card));
        deleteBtn.setOnAction(e -> showDeleteConfirmDialog(tache, card));

        configurerDragAndDrop(card, tache);
        configurerCarteDrop(card, tache);

        return card;
    }

    private void configurerCarteDrop(VBox card, Tache tache) {
        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                try {
                    int draggedId = Integer.parseInt(db.getString());
                    if (draggedId == tache.getId()) {
                        event.setDropCompleted(false);
                        event.consume();
                        return;
                    }
                    VBox targetColumn = (VBox) card.getParent();
                    StatutTache nouveauStatut = determinerStatut(targetColumn);
                    deplacerTache(draggedId, nouveauStatut, targetColumn);
                    event.setDropCompleted(true);
                } catch (Exception e) {
                    event.setDropCompleted(false);
                }
            }
            event.consume();
        });
    }

    private void configurerDragAndDrop(VBox card, Tache tache) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(tache.getId()));
            db.setContent(content);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = card.snapshot(params, null);
            if (snapshot != null) {
                db.setDragView(snapshot, snapshot.getWidth() / 2, 20);
            }
            card.setOpacity(0.4);
            event.consume();
        });

        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            event.consume();
        });
    }

    private void configurerColonneDrop(VBox colonne) {
        colonne.setOnDragOver(event -> {
            if (event.getGestureSource() != colonne && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        colonne.setOnDragEntered(event -> {
            if (event.getGestureSource() != colonne && event.getDragboard().hasString()) {
                Object src = event.getGestureSource();
                if (src instanceof VBox) {
                    Tache t = (Tache) ((VBox) src).getUserData();
                    if (t != null && determinerStatut(colonne) != t.getStatut()) {
                        colonne.setStyle("-fx-background-color: rgba(124,58,237,0.08); -fx-background-radius: 8; -fx-border-color: rgba(124,58,237,0.3); -fx-border-width: 2; -fx-border-radius: 8;");
                    }
                }
            }
            event.consume();
        });

        colonne.setOnDragExited(event -> {
            colonne.setStyle(null);
            event.consume();
        });

        colonne.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int tacheId = Integer.parseInt(db.getString());
                VBox targetCards = getCardsListForColumn(colonne);
                StatutTache nouveauStatut = determinerStatut(colonne);
                if (targetCards != null) {
                    deplacerTache(tacheId, nouveauStatut, targetCards);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            colonne.setStyle(null);
            event.consume();
        });
    }

    private VBox getCardsListForColumn(VBox colonne) {
        if (colonne == todoColumn || colonne == todoCards) return todoCards;
        if (colonne == doingColumn || colonne == doingCards) return doingCards;
        if (colonne == doneColumn || colonne == doneCards) return doneCards;
        return null;
    }

    private StatutTache determinerStatut(VBox colonne) {
        if (colonne == todoColumn || colonne == todoCards) return StatutTache.A_FAIRE;
        if (colonne == doingColumn || colonne == doingCards) return StatutTache.EN_COURS;
        if (colonne == doneColumn || colonne == doneCards) return StatutTache.TERMINEE;
        return StatutTache.A_FAIRE;
    }

    private void deplacerTache(int tacheId, StatutTache nouveauStatut, VBox targetColumn) {
        try {
            Tache tache = tacheService.getById(tacheId);
            if (tache != null && tache.getStatut() != nouveauStatut) {
                tache.setStatut(nouveauStatut);
                tacheService.update(tache);

                todoCards.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });
                doingCards.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });
                doneCards.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });

                VBox nouvelleCarte = buildTaskCard(tache);
                targetColumn.getChildren().add(nouvelleCarte);
                animateCardIn(nouvelleCarte);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void animateCardIn(VBox card) {
        card.setTranslateX(-30);
        card.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), card);
        slide.setToX(0);
        FadeTransition fade = new FadeTransition(Duration.millis(250), card);
        fade.setToValue(1.0);
        new ParallelTransition(slide, fade).play();
    }

    private void ajouterCarteDansColonne(VBox card, StatutTache statut) {
        if (statut == StatutTache.A_FAIRE) {
            todoCards.getChildren().add(card);
        } else if (statut == StatutTache.EN_COURS) {
            doingCards.getChildren().add(card);
        } else if (statut == StatutTache.TERMINEE) {
            doneCards.getChildren().add(card);
        }
    }
}
