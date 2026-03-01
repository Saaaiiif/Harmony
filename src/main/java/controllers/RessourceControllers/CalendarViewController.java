package controllers.RessourceControllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.layout.*;
import javafx.scene.Cursor;
import javafx.stage.Modality;
import javafx.stage.Window;
import models.RessourceModels.Evenement;
import models.RessourceModels.Salle;
import models.RessourceModels.StatutDemandeSalle;
import models.RessourceModels.Tache;
import models.RessourceModels.TypeEvenement;
import services.RessourceServices.HolidaysApiService;
import services.RessourceServices.WeatherApiService;
import services.RessourceServices.EvenementService;
import services.RessourceServices.SalleService;
import services.RessourceServices.TacheService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class CalendarViewController implements Initializable {

    @FXML private TabPane mainTabPane;
    @FXML private HBox eventsHeader;
    @FXML private HBox eventsActionBar;
    @FXML private HBox tasksHeader;
    @FXML private HBox tasksActionBar;
    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, Number> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colDateDebut;
    @FXML private TableColumn<Evenement, String> colDateFin;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, String> colSalle;
    @FXML private TableColumn<Evenement, String> colStatutDemande;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, Void> colActions;

    @FXML private TableView<Tache> tacheTable;
    @FXML private TableColumn<Tache, Number> colTacheId;
    @FXML private TableColumn<Tache, String> colTacheNom;
    @FXML private TableColumn<Tache, String> colTacheDeadline;
    @FXML private TableColumn<Tache, String> colTacheStatut;
    @FXML private TableColumn<Tache, Void> colTacheActions;

    @FXML private VBox calendarMonthBox;
    @FXML private Label labelMonthYear;
    @FXML private Label labelWeather;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnNextMonth;

    private final EvenementService evenementService = new EvenementService();
    private final WeatherApiService weatherApiService = new WeatherApiService();
    private final HolidaysApiService holidaysApiService = new HolidaysApiService();
    private Map<LocalDate, String> holidaysForYear = new HashMap<>();
    private int lastHolidaysYear = -1;
    private final TacheService tacheService = new TacheService();
    private final SalleService salleService = new SalleService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("dd/MM/yyyy");
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate defaultDateForNewEvent = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnes();
        configurerColonnesTache();
        if (eventsTable != null) eventsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        configurerToolbarHover();
        chargerEvenements();
        chargerTaches();
        loadWeatherForToday();
        buildCalendarMonth();
        loadHolidaysThenBuildCalendar();
    }

    private void configurerToolbarHover() {
        if (eventsActionBar != null) eventsActionBar.setOpacity(1);
        if (tasksActionBar != null) tasksActionBar.setOpacity(1);
    }

    private Window getDialogOwner() {
        if (eventsTable != null && eventsTable.getScene() != null) return eventsTable.getScene().getWindow();
        if (mainTabPane != null && mainTabPane.getScene() != null) return mainTabPane.getScene().getWindow();
        if (calendarMonthBox != null && calendarMonthBox.getScene() != null) return calendarMonthBox.getScene().getWindow();
        return null;
    }

    private void configurerColonnes() {
        if (colId != null) colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        if (colTitre != null) colTitre.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getTitre())));
        if (colDateDebut != null) colDateDebut.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getDateDebut())));
        if (colDateFin != null) colDateFin.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getDateFin())));
        if (colLieu != null) colLieu.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getLieu())));
        if (colSalle != null) colSalle.setCellValueFactory(cell -> {
            if (cell.getValue().getSalleId() == null) return new SimpleStringProperty("");
            Salle s = salleService.getById(cell.getValue().getSalleId());
            return new SimpleStringProperty(s != null ? s.getNom() : "");
        });
        if (colStatutDemande != null) colStatutDemande.setCellValueFactory(cell -> {
            StatutDemandeSalle statut = cell.getValue().getStatutDemandeSalle();
            if (statut == null) return new SimpleStringProperty("");
            switch (statut) {
                case EN_ATTENTE: return new SimpleStringProperty("En attente");
                case REFUSE: return new SimpleStringProperty("Refusé");
                case CONFIRME: return new SimpleStringProperty("Confirmé");
                default: return new SimpleStringProperty(statut.name());
            }
        });
        if (colType != null) colType.setCellValueFactory(cell -> {
            TypeEvenement t = cell.getValue().getType();
            return new SimpleStringProperty(t != null ? t.name() : "");
        });
        if (colActions != null) {
            colActions.setCellValueFactory(param -> null);
            colActions.setCellFactory(param -> new TableCell<Evenement, Void>() {
                private final HBox box = new HBox(8);
                private final Button btnEdit = new Button("✎");
                private final Button btnDelete = new Button("✕");
                {
                    box.setAlignment(Pos.CENTER);
                    btnEdit.getStyleClass().addAll("row-action-btn", "row-action-edit");
                    btnDelete.getStyleClass().addAll("row-action-btn", "row-action-delete");
                    btnEdit.setOnAction(e -> {
                        Evenement ev = getTableRow().getItem();
                        if (ev != null) {
                            eventsTable.getSelectionModel().clearSelection();
                            eventsTable.getSelectionModel().select(ev);
                            onModifier();
                        }
                    });
                    btnDelete.setOnAction(e -> {
                        Evenement ev = getTableRow().getItem();
                        if (ev != null) supprimerEvenement(ev);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        box.getChildren().setAll(btnEdit, btnDelete);
                        setGraphic(box);
                    }
                }
            });
        }
    }

    private void supprimerEvenement(Evenement e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer l'événement \"" + (e.getTitre() != null ? e.getTitre() : "") + "\" ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                evenementService.delete(e.getId());
                chargerEvenements();
                buildCalendarMonth();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        }
    }

    private void consulterEvenement(Evenement e) {
        String salleInfo = "";
        if (e.getSalleId() != null) {
            Salle s = salleService.getById(e.getSalleId());
            salleInfo = "\nSalle : " + (s != null ? s.getNom() : "") + "\nStatut demande : " + (e.getStatutDemandeSalle() != null ? e.getStatutDemandeSalle() : "");
        }
        String detail = String.format(
                "Titre : %s\nDescription : %s\nDate début : %s\nDate fin : %s\nLieu : %s%s\nType : %s",
                nullToEmpty(e.getTitre()), nullToEmpty(e.getDescription()),
                formatDateTime(e.getDateDebut()), formatDateTime(e.getDateFin()),
                nullToEmpty(e.getLieu()), salleInfo, e.getType() != null ? e.getType().name() : "");
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détail de l'événement");
        info.setHeaderText(e.getTitre());
        info.setContentText(detail);
        info.showAndWait();
    }

    private void modifierEvenementDepuisCalendrier(Evenement ev) {
        Dialog<Evenement> dialog = creerDialogEvenement("Modifier l'événement", ev);
        if (dialog == null) return;
        dialog.showAndWait().filter(e -> e != null).ifPresent(e -> {
            try {
                e.setId(ev.getId());
                evenementService.update(e);
                chargerEvenements();
                buildCalendarMonth();
                showInfo("Événement modifié.");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        });
    }

    private String formatDateTime(Date date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    private String formatDate(Date date) {
        return date != null ? DATE_FORMAT_SHORT.format(date) : "";
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void configurerColonnesTache() {
        if (colTacheId != null) colTacheId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        if (colTacheNom != null) colTacheNom.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(cell.getValue().getNom())));
        if (colTacheDeadline != null) colTacheDeadline.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDeadline())));
        if (colTacheStatut != null) colTacheStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatut() != null ? cell.getValue().getStatut().name() : ""));
        if (tacheTable != null) tacheTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (colTacheActions != null) {
            colTacheActions.setCellValueFactory(param -> null);
            colTacheActions.setCellFactory(param -> new TableCell<Tache, Void>() {
                private final HBox box = new HBox(8);
                private final Button btnEdit = new Button("✎");
                private final Button btnDelete = new Button("✕");
                {
                    box.setAlignment(Pos.CENTER);
                    btnEdit.getStyleClass().addAll("row-action-btn", "row-action-edit");
                    btnDelete.getStyleClass().addAll("row-action-btn", "row-action-delete");
                    btnEdit.setOnAction(e -> {
                        Tache t = getTableRow().getItem();
                        if (t != null) {
                            tacheTable.getSelectionModel().clearSelection();
                            tacheTable.getSelectionModel().select(t);
                            onModifierTache();
                        }
                    });
                    btnDelete.setOnAction(e -> {
                        Tache t = getTableRow().getItem();
                        if (t != null) supprimerTache(t);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        box.getChildren().setAll(btnEdit, btnDelete);
                        setGraphic(box);
                    }
                }
            });
        }
    }

    private void ajouterEvenementDepuisCalendrier(LocalDate date) {
        try {
            defaultDateForNewEvent = date;
            Dialog<Evenement> dialog = creerDialogEvenement("Nouvel événement", null);
            if (dialog == null) return;
            dialog.showAndWait().filter(e -> e != null).ifPresent(e -> {
                try {
                    evenementService.add(e);
                    chargerEvenements();
                    buildCalendarMonth();
                    showInfo("Événement créé.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlertErreur(getDialogOwner(), ex);
                }
            });
        } finally {
            defaultDateForNewEvent = null;
        }
    }

    private void supprimerTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la tâche \"" + (t.getNom() != null ? t.getNom() : "") + "\" ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                tacheService.delete(t.getId());
                chargerTaches();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        }
    }

    private void chargerEvenements() {
        if (eventsTable == null) return;
        List<Evenement> list = evenementService.getAll();
        eventsTable.setItems(FXCollections.observableArrayList(list));
    }

    private void chargerTaches() {
        if (tacheTable == null) return;
        List<Tache> list = tacheService.getAll();
        tacheTable.setItems(FXCollections.observableArrayList(list));
    }

    private void loadWeatherForToday() {
        if (labelWeather == null) return;
        weatherApiService.getWeatherForDate(LocalDate.now()).thenAccept(w -> {
            Platform.runLater(() -> {
                if (labelWeather == null) return;
                if (w.error != null) labelWeather.setText("Météo : " + w.error);
                else if (w.tempMax != null) labelWeather.setText("Météo aujourd'hui : " + w.description + ", " + w.tempMin.intValue() + "° / " + w.tempMax.intValue() + "°");
                else labelWeather.setText("Météo : " + w.description);
            });
        });
    }

    private void loadHolidaysThenBuildCalendar() {
        int year = currentMonth.getYear();
        lastHolidaysYear = year;
        holidaysApiService.getHolidaysForYear(year).thenAccept(map -> {
            holidaysForYear = map != null ? map : new HashMap<>();
            Platform.runLater(this::buildCalendarMonth);
        });
    }

    @FXML
    private void onPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        if (currentMonth.getYear() != lastHolidaysYear) loadHolidaysThenBuildCalendar();
        else buildCalendarMonth();
    }

    @FXML
    private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        if (currentMonth.getYear() != lastHolidaysYear) loadHolidaysThenBuildCalendar();
        else buildCalendarMonth();
    }

    @FXML
    private void onExportEvenementsJson() {
        List<Evenement> list = evenementService.getAll();
        org.json.JSONArray arr = new org.json.JSONArray();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Evenement e : list) {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("id", e.getId());
            o.put("titre", e.getTitre());
            o.put("description", e.getDescription());
            o.put("dateDebut", e.getDateDebut() != null ? df.format(e.getDateDebut()) : null);
            o.put("dateFin", e.getDateFin() != null ? df.format(e.getDateFin()) : null);
            o.put("lieu", e.getLieu());
            o.put("type", e.getType() != null ? e.getType().name() : null);
            arr.put(o);
        }
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter les événements");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(getDialogOwner());
        if (f != null) {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                w.write(arr.toString(2));
                showInfo("Export réussi : " + f.getAbsolutePath());
            } catch (Exception ex) {
                showAlertErreur(getDialogOwner(), ex);
            }
        }
    }

    @FXML
    private void handleExportHtml() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le calendrier en HTML");
        fc.setInitialFileName("calendrier_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".html");
        fc.getExtensionFilters().add(new ExtensionFilter("Fichier HTML", "*.html"));
        File file = fc.showSaveDialog(getDialogOwner());
        if (file == null) return;
        try {
            List<Evenement> events = getCurrentMonthEvents();
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html lang='fr'>\n<head>\n<meta charset='UTF-8'>\n");
            html.append("<title>Calendrier Harmony - ").append(getCurrentMonthLabel()).append("</title>\n<style>\n");
            html.append("body{font-family:'Segoe UI',sans-serif;background:#F4F3FA;margin:0;padding:24px;} ");
            html.append("h1{color:#7C3AED;font-size:28px;margin-bottom:4px;} ");
            html.append(".subtitle{color:#6B7280;font-size:14px;margin-bottom:24px;} ");
            html.append(".calendar-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:1px;background:rgba(124,58,237,0.1);border:1px solid rgba(124,58,237,0.1);border-radius:12px;overflow:hidden;} ");
            html.append(".day-header{background:rgba(124,58,237,0.08);text-align:center;padding:10px;font-weight:bold;color:#7C3AED;font-size:12px;text-transform:uppercase;} ");
            html.append(".day-cell{background:white;min-height:90px;padding:8px;vertical-align:top;} .day-cell.empty{background:#fafafa;} ");
            html.append(".day-cell.today{background:rgba(124,58,237,0.05);border-top:2px solid #7C3AED;} ");
            html.append(".day-number{font-weight:bold;color:#1E1B4B;font-size:14px;margin-bottom:4px;} .day-number.today-num{color:#7C3AED;} ");
            html.append(".event-pill{background:rgba(124,58,237,0.1);color:#7C3AED;font-size:11px;padding:2px 8px;border-radius:999px;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;} ");
            html.append(".export-info{color:#9CA3AF;font-size:12px;margin-top:16px;text-align:right;}</style>\n</head>\n<body>\n");
            html.append("<h1>📅 Calendrier Harmony</h1>\n<div class='subtitle'>").append(getCurrentMonthLabel()).append("</div>\n<div class='calendar-grid'>\n");
            String[] days = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
            for (String d : days) html.append("  <div class='day-header'>").append(d).append("</div>\n");
            LocalDate firstDay = currentMonth.atDay(1);
            int startDow = firstDay.getDayOfWeek().getValue();
            int daysInMonth = currentMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();
            for (int i = 1; i < startDow; i++) html.append("  <div class='day-cell empty'></div>\n");
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentMonth.atDay(day);
                boolean isToday = date.equals(today);
                html.append("  <div class='day-cell").append(isToday ? " today" : "").append("'>\n");
                html.append("    <div class='day-number").append(isToday ? " today-num" : "").append("'>").append(day).append("</div>\n");
                for (String ev : getEventsForDate(date)) {
                    String safe = ev.replace("<","&lt;").replace(">","&gt;");
                    html.append("    <div class='event-pill' title='").append(safe).append("'>").append(safe).append("</div>\n");
                }
                html.append("  </div>\n");
            }
            int totalCells = startDow - 1 + daysInMonth;
            int remaining = (7 - (totalCells % 7)) % 7;
            for (int i = 0; i < remaining; i++) html.append("  <div class='day-cell empty'></div>\n");
            html.append("</div>\n<div class='export-info'>Exporté le ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(" depuis Harmony</div>\n</body>\n</html>");
            Files.writeString(file.toPath(), html.toString());
            showExportSuccess("HTML", file);
        } catch (Exception e) {
            showExportError("HTML", e);
        }
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le calendrier en Excel");
        fc.setInitialFileName("calendrier_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".xlsx");
        fc.getExtensionFilters().add(new ExtensionFilter("Fichier Excel", "*.xlsx"));
        File file = fc.showSaveDialog(getDialogOwner());
        if (file == null) return;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Calendrier " + getCurrentMonthLabel());
            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)237,(byte)233,(byte)254}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("📅 Calendrier Harmony — " + getCurrentMonthLabel());
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            String[] dayNames = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < 7; i++) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
                c.setCellValue(dayNames[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            List<Evenement> allEvents = getCurrentMonthEvents();
            XSSFSheet listSheet = wb.createSheet("Liste des événements");
            String[] listHeaders = {"Date","Titre","Description","Heure début","Heure fin","Lieu","Type"};
            Row listHeader = listSheet.createRow(0);
            for (int i = 0; i < listHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = listHeader.createCell(i);
                c.setCellValue(listHeaders[i]);
                c.setCellStyle(headerStyle);
                listSheet.setColumnWidth(i, 18 * 256);
            }
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            int listRow = 1;
            for (Evenement e : allEvents) {
                Row r = listSheet.createRow(listRow++);
                r.createCell(0).setCellValue(e.getDateDebut() != null ? DATE_FORMAT_SHORT.format(e.getDateDebut()) : "");
                r.createCell(1).setCellValue(e.getTitre() != null ? e.getTitre() : "");
                r.createCell(2).setCellValue(e.getDescription() != null ? e.getDescription() : "");
                r.createCell(3).setCellValue(e.getDateDebut() != null ? df.format(e.getDateDebut()) : "");
                r.createCell(4).setCellValue(e.getDateFin() != null ? df.format(e.getDateFin()) : "");
                r.createCell(5).setCellValue(e.getLieu() != null ? e.getLieu() : "");
                r.createCell(6).setCellValue(e.getType() != null ? e.getType().name() : "");
            }
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showExportSuccess("Excel", file);
        } catch (Exception e) {
            showExportError("Excel", e);
        }
    }

    private List<Evenement> getCurrentMonthEvents() {
        List<Evenement> all = evenementService.getAll();
        List<Evenement> list = new ArrayList<>();
        for (Evenement e : all) {
            if (e.getDateDebut() == null) continue;
            LocalDate d = e.getDateDebut().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.getYear() == currentMonth.getYear() && d.getMonth() == currentMonth.getMonth())
                list.add(e);
        }
        return list;
    }

    private List<String> getEventsForDate(LocalDate date) {
        List<Evenement> all = evenementService.getAll();
        List<String> titles = new ArrayList<>();
        for (Evenement e : all) {
            if (e.getDateDebut() == null) continue;
            LocalDate d = e.getDateDebut().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.equals(date)) titles.add(e.getTitre() != null ? e.getTitre() : "");
        }
        return titles;
    }

    private String getCurrentMonthLabel() {
        if (labelMonthYear != null && labelMonthYear.getText() != null) return labelMonthYear.getText();
        String[] mois = {"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        return mois[currentMonth.getMonthValue() - 1] + " " + currentMonth.getYear();
    }

    private void showExportSuccess(String format, File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export réussi");
        alert.setHeaderText("✅ Export " + format + " réussi !");
        alert.setContentText("Fichier sauvegardé :\n" + file.getAbsolutePath());
        if (getDialogOwner() != null) alert.initOwner(getDialogOwner());
        alert.showAndWait();
    }

    private void showExportError(String format, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur d'export");
        alert.setHeaderText("❌ Erreur lors de l'export " + format);
        alert.setContentText(e.getMessage() != null ? e.getMessage() : "");
        if (getDialogOwner() != null) alert.initOwner(getDialogOwner());
        alert.showAndWait();
        e.printStackTrace();
    }

    private void buildCalendarMonth() {
        if (calendarMonthBox == null) return;
        calendarMonthBox.getChildren().clear();
        if (labelMonthYear != null) {
            String[] mois = {"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
            labelMonthYear.setText(mois[currentMonth.getMonthValue() - 1] + " " + currentMonth.getYear());
        }
        List<Evenement> events = evenementService.getAll();
        Map<LocalDate, List<Evenement>> byDay = new HashMap<>();
        for (Evenement e : events) {
            if (e.getDateDebut() == null) continue;
            LocalDate d = e.getDateDebut().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.getYear() == currentMonth.getYear() && d.getMonth() == currentMonth.getMonth()) {
                byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
            }
        }
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        GridPane grid = new GridPane();
        grid.getStyleClass().add("calendar-grid");
        grid.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        grid.setHgap(0);
        grid.setVgap(0);

        grid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }

        grid.getRowConstraints().clear();
        RowConstraints rcHeader = new RowConstraints();
        rcHeader.setVgrow(Priority.NEVER);
        rcHeader.setMinHeight(36);
        rcHeader.setPrefHeight(36);
        grid.getRowConstraints().add(rcHeader);
        for (int r = 0; r < 6; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            rc.setMinHeight(60);
            grid.getRowConstraints().add(rc);
        }

        for (int i = 0; i < 7; i++) {
            Label h = new Label(jours[i]);
            h.getStyleClass().addAll("calendar-weekday", "calendar-header-label");
            h.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(h, Priority.ALWAYS);
            grid.add(h, i, 0);
        }
        LocalDate first = currentMonth.atDay(1);
        int startOffset = first.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();
        int row = 1, col = startOffset;
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox cell = new VBox(4);
            cell.getStyleClass().add("calendar-day-cell");
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMaxHeight(Double.MAX_VALUE);
            cell.setMinHeight(60);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
            GridPane.setFillWidth(cell, true);
            GridPane.setFillHeight(cell, true);
            String holidayName = holidaysForYear.get(date);
            boolean isHoliday = holidayName != null;
            if (date.equals(today)) cell.getStyleClass().add("calendar-day-today");
            if (isHoliday) cell.getStyleClass().add("calendar-day-holiday");
            Label num = new Label(String.valueOf(day));
            num.getStyleClass().addAll("calendar-day-num", "calendar-day-number");
            cell.getChildren().add(num);

            if (isHoliday) {
                Label holidayLabel = new Label(holidayName);
                holidayLabel.getStyleClass().add("calendar-day-holiday-label");
                cell.getChildren().add(holidayLabel);
            }
            List<Evenement> dayEvents = byDay.getOrDefault(date, Collections.emptyList());
            List<Evenement> dayEventsCopy = new ArrayList<>(dayEvents);
            for (Evenement ev : dayEvents.stream().limit(3).collect(Collectors.toList())) {
                Label el = new Label(ev.getTitre() != null ? (ev.getTitre().length() > 18 ? ev.getTitre().substring(0, 17) + "…" : ev.getTitre()) : "");
                el.getStyleClass().addAll("calendar-day-event", "calendar-event-label");
                el.setWrapText(true);
                el.setUserData(ev);
                el.setCursor(Cursor.HAND);
                el.setOnMouseClicked(me -> {
                    Evenement event = (Evenement) el.getUserData();
                    if (event == null) return;
                    ContextMenu menu = new ContextMenu();
                    MenuItem miConsulter = new MenuItem("Consulter");
                    MenuItem miModifier = new MenuItem("Modifier");
                    MenuItem miSupprimer = new MenuItem("Supprimer");
                    miConsulter.setOnAction(e -> consulterEvenement(event));
                    miModifier.setOnAction(e -> modifierEvenementDepuisCalendrier(event));
                    miSupprimer.setOnAction(e -> supprimerEvenement(event));
                    menu.getItems().addAll(miConsulter, miModifier, miSupprimer);
                    menu.show(el, me.getScreenX(), me.getScreenY());
                });
                cell.getChildren().add(el);
            }
            if (dayEvents.size() > 3) {
                Label more = new Label("+" + (dayEvents.size() - 3) + " autre(s)");
                more.getStyleClass().add("calendar-day-more");
                cell.getChildren().add(more);
            }

            cell.setOnMouseClicked(me -> {
                if (dayEventsCopy.isEmpty()) {
                    ajouterEvenementDepuisCalendrier(date);
                } else {
                    ContextMenu menu = new ContextMenu();
                    MenuItem miAjouter = new MenuItem("Ajouter un événement ce jour");
                    miAjouter.setOnAction(e -> ajouterEvenementDepuisCalendrier(date));
                    menu.getItems().add(miAjouter);

                    for (Evenement ev : dayEventsCopy) {
                        String titre = ev.getTitre() != null ? ev.getTitre() : "(sans titre)";
                        MenuItem miMod = new MenuItem("Modifier : " + titre);
                        MenuItem miSup = new MenuItem("Supprimer : " + titre);
                        miMod.setOnAction(e -> modifierEvenementDepuisCalendrier(ev));
                        miSup.setOnAction(e -> supprimerEvenement(ev));
                        menu.getItems().addAll(miMod, miSup);
                    }
                    menu.show(cell, me.getScreenX(), me.getScreenY());
                }
            });

            grid.add(cell, col, row);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
            col++;
            if (col == 7) { col = 0; row++; }
        }
        calendarMonthBox.getChildren().add(grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
    }

    @FXML
    private void onAjouter() {
        try {
            Dialog<Evenement> dialog = creerDialogEvenement("Nouvel événement", null);
            if (dialog == null) return;
            dialog.showAndWait().filter(e -> e != null).ifPresent(e -> {
                try {
                    evenementService.add(e);
                    chargerEvenements();
                    buildCalendarMonth();
                    showInfo("Événement créé." + (e.getStatutDemandeSalle() == StatutDemandeSalle.EN_ATTENTE ? " Demande de salle en attente." : ""));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlertErreur(getDialogOwner(), ex);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlertErreur(getDialogOwner(), e);
        }
    }

    @FXML
    private void onModifier() {
        ObservableList<Evenement> selected = eventsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) { showInfo("Veuillez sélectionner un événement à modifier."); return; }
        if (selected.size() > 1) { showInfo("Veuillez sélectionner un seul événement."); return; }
        Evenement event = selected.get(0);
        Dialog<Evenement> dialog = creerDialogEvenement("Modifier l'événement", event);
        if (dialog == null) return;
        dialog.showAndWait().filter(e -> e != null).ifPresent(e -> {
            try {
                e.setId(event.getId());
                evenementService.update(e);
                chargerEvenements();
                buildCalendarMonth();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        });
    }

    @FXML
    private void onSupprimer() {
        ObservableList<Evenement> selected = eventsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) { showInfo("Veuillez sélectionner un ou plusieurs événements à supprimer."); return; }
        int count = selected.size();
        String message = count == 1 ? "Supprimer l'événement \"" + selected.get(0).getTitre() + "\" ?" : "Supprimer les " + count + " événements sélectionnés ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                selected.forEach(e -> evenementService.delete(e.getId()));
                chargerEvenements();
                buildCalendarMonth();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        }
    }

    private Dialog<Evenement> creerDialogEvenement(String titre, Evenement initial) {
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window window = getDialogOwner();
        if (window != null) dialog.initOwner(window);
        dialog.getDialogPane().setMinWidth(560);
        dialog.getDialogPane().setMinHeight(560);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RessourceViews/event-form.fxml"));
            Parent form = loader.load();
            EventFormController formCtrl = loader.getController();
            if (initial != null) {
                formCtrl.initFrom(initial);
            } else if (defaultDateForNewEvent != null) {
                formCtrl.initForDate(defaultDateForNewEvent);
            }
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/views/RessourceViews/ressource-styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.addEventFilter(ActionEvent.ACTION, e -> { if (!formCtrl.validate()) e.consume(); });
            dialog.setResultConverter(btn -> btn == ButtonType.OK ? formCtrl.buildEvenement(initial != null ? initial.getId() : 0) : null);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlertErreur(window, ex);
            return null;
        }
        return dialog;
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showAlertErreur(Window window, Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) msg = e.getClass().getName();
        if (e.getCause() != null) msg += " - " + e.getCause().getMessage();
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle("Erreur");
        err.setHeaderText(null);
        err.setContentText(msg);
        if (window != null) err.initOwner(window);
        err.showAndWait();
    }

    @FXML
    private void onAjouterTache() {
        Dialog<Tache> d = creerDialogTache("Nouvelle tâche", null);
        if (d == null) return;
        d.showAndWait().filter(t -> t != null).ifPresent(t -> {
            try {
                tacheService.add(t);
                chargerTaches();
            } catch (Exception e) {
                e.printStackTrace();
                showAlertErreur(getDialogOwner(), e);
            }
        });
    }

    @FXML
    private void onModifierTache() {
        ObservableList<Tache> sel = tacheTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner une tâche à modifier."); return; }
        if (sel.size() > 1) { showInfo("Veuillez sélectionner une seule tâche."); return; }
        Tache t = sel.get(0);
        Dialog<Tache> d = creerDialogTache("Modifier la tâche", t);
        if (d == null) return;
        d.showAndWait().filter(x -> x != null).ifPresent(x -> {
            try {
                x.setId(t.getId());
                tacheService.update(x);
                chargerTaches();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        });
    }

    @FXML
    private void onSupprimerTache() {
        ObservableList<Tache> sel = tacheTable.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { showInfo("Veuillez sélectionner une ou plusieurs tâches à supprimer."); return; }
        String msg = sel.size() == 1 ? "Supprimer la tâche \"" + sel.get(0).getNom() + "\" ?" : "Supprimer les " + sel.size() + " tâches sélectionnées ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                sel.forEach(t -> tacheService.delete(t.getId()));
                chargerTaches();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlertErreur(getDialogOwner(), ex);
            }
        }
    }

    private Dialog<Tache> creerDialogTache(String titre, Tache initial) {
        Dialog<Tache> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window window = getDialogOwner();
        if (window != null) dialog.initOwner(window);
        dialog.getDialogPane().setMinWidth(480);
        dialog.getDialogPane().setMinHeight(420);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RessourceViews/tache-form.fxml"));
            Parent form = loader.load();
            TacheFormController ctrl = loader.getController();
            if (initial != null) ctrl.initFrom(initial);
            dialog.getDialogPane().setContent(form);
            URL cssUrl = getClass().getResource("/views/RessourceViews/ressource-styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.addEventFilter(ActionEvent.ACTION, e -> { if (!ctrl.validate()) e.consume(); });
            dialog.setResultConverter(btn -> btn == ButtonType.OK ? ctrl.buildTache(initial != null ? initial.getId() : 0) : null);
        } catch (Exception e) {
            e.printStackTrace();
            showAlertErreur(window, e);
            return null;
        }
        return dialog;
    }
}
