package controllers.MeditationControllers;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.MeditationModels.Humeur;
import models.MeditationModels.JournalHumeur;
import models.UserModels.Role;
import models.UserModels.user;
import services.MeditationServices.GroqAIService;
import services.MeditationServices.JournalHumeurService;
import services.UserServices.serviceUser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Admin view — lists all students and allows generating an AI-powered
 * well-being report + exporting it as a PDF for each student.
 */
public class AdminJournalController implements Initializable {

    @FXML private VBox      studentListContainer;
    @FXML private VBox      emptyState;
    @FXML private TextField searchField;

    private final serviceUser      userService    = new serviceUser();
    private final JournalHumeurService journalService = new JournalHumeurService();
    private final GroqAIService    groqService    = new GroqAIService();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private List<user> allStudents;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStudents();
    }

    @FXML
    private void onSearchChanged() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<user> filtered = allStudents.stream()
                .filter(s -> query.isEmpty() || getFullName(s).toLowerCase().contains(query))
                .collect(Collectors.toList());
        displayStudents(filtered);
    }

    private void loadStudents() {
        try {
            // Get all active users and keep only students
            allStudents = userService.getAll().stream()
                    .filter(u -> u.getType_utilisateur() == Role.ETUDIANT)
                    .collect(Collectors.toList());
            displayStudents(allStudents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayStudents(List<user> students) {
        studentListContainer.getChildren().clear();
        if (students.isEmpty()) {
            emptyState.setVisible(true); emptyState.setManaged(true);
            studentListContainer.setVisible(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            studentListContainer.setVisible(true);
            for (user s : students) studentListContainer.getChildren().add(createStudentRow(s));
        }
    }

    private HBox createStudentRow(user student) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 8, 0, 0, 2);");

        Label nameLabel = new Label("👤  " + getFullName(student));
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label emailLabel = new Label(student.getUser_email() != null ? student.getUser_email() : "");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        VBox info = new VBox(2, nameLabel, emailLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button rapportBtn = new Button("📊 Rapport d'étudiant");
        rapportBtn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        rapportBtn.setOnAction(e -> showRapportModal(student));

        row.getChildren().addAll(info, spacer, rapportBtn);
        return row;
    }

    // ── Rapport modal ────────────────────────────────────────────────────────

    private void showRapportModal(user student) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Rapport — " + getFullName(student));
        modal.setMinWidth(650); modal.setMinHeight(520);

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ffffff;");

        Label titleLabel = new Label("Rapport de bien-être : " + getFullName(student));
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7C3AED;");

        Label statusLabel = new Label("⏳ Analyse en cours par l'IA...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7C3AED;");

        TextArea rapportArea = new TextArea();
        rapportArea.setWrapText(true); rapportArea.setEditable(false); rapportArea.setPrefRowCount(20);
        rapportArea.setStyle("-fx-font-size: 13px; -fx-border-color: #D1D5DB; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(rapportArea, Priority.ALWAYS);

        Button exportPdfBtn = new Button("📄 Exporter PDF");
        exportPdfBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 9 22; -fx-background-radius: 8; -fx-cursor: hand;");
        exportPdfBtn.setDisable(true);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 9 28; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        HBox btnBox = new HBox(10, exportPdfBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titleLabel, statusLabel, rapportArea, btnBox);
        modal.setScene(new Scene(root, 680, 560));
        modal.show();

        new Thread(() -> {
            try {
                List<JournalHumeur> journals = journalService.getByUserId(student.getUser_id());
                if (journals.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("⚠️ Aucune donnée disponible");
                        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #D97706;");
                        rapportArea.setText("Cet étudiant n'a pas encore d'entrées dans son journal d'humeur.\n\nAucun rapport ne peut être généré pour le moment.");
                    });
                    return;
                }

                String journalSummary = journals.stream()
                        .map(j -> "Date: " + (j.getDate() != null ? j.getDate().format(dateFormatter) : "N/A")
                                + " | Humeur: " + j.getHumeur().getLabel()
                                + " | Score: " + j.getScore() + "/5")
                        .collect(Collectors.joining("\n"));
                journalSummary += "\n\nRésumé: " + journals.size() + " entrées au total.";
                double avgScore = journals.stream().mapToInt(JournalHumeur::getScore).average().orElse(0);
                journalSummary += "\nScore moyen: " + String.format("%.1f", avgScore) + "/5 (" + Humeur.fromScore((int) Math.round(avgScore)).getLabel() + ")";

                String rapport = groqService.generateStudentRapport(getFullName(student), journalSummary);
                final List<JournalHumeur> finalJournals = journals;
                final double finalAvgScore = avgScore;

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Rapport généré avec succès");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #059669;");
                    rapportArea.setText(rapport);
                    exportPdfBtn.setDisable(false);
                    exportPdfBtn.setOnAction(ev -> exportRapportPdf(student, rapportArea.getText(), finalJournals, finalAvgScore, modal));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur lors de la génération");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #DC2626;");
                    rapportArea.setText("Erreur: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ── PDF export ───────────────────────────────────────────────────────────

    private void exportRapportPdf(user student, String rapportText, List<JournalHumeur> journals, double avgScore, Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("Rapport_" + getFullName(student).replace(" ", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        try {
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(40, 40, 40, 40);

            DeviceRgb purple      = new DeviceRgb(124, 109, 175);
            DeviceRgb lightPurple = new DeviceRgb(237, 233, 248);
            DeviceRgb darkText    = new DeviceRgb(51,  51,  51);
            DeviceRgb grey        = new DeviceRgb(119, 119, 119);
            DeviceRgb green       = new DeviceRgb(39,  174, 96);

            PdfFont bold    = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regular = PdfFontFactory.createFont("Helvetica");

            // Header
            Div header = new Div().setBackgroundColor(purple).setPadding(25).setMarginBottom(25);
            header.add(new Paragraph("HARMONIE").setFont(bold).setFontSize(28).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
            header.add(new Paragraph("Rapport de bien-être étudiant").setFont(regular).setFontSize(14).setFontColor(new DeviceRgb(220, 215, 240)).setTextAlignment(TextAlignment.CENTER));
            doc.add(header);

            // Info table
            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth().setMarginBottom(20);
            addInfoRow(info, "Étudiant :", getFullName(student), bold, regular, purple, darkText);
            addInfoRow(info, "Date du rapport :", LocalDate.now().format(dateFormatter), bold, regular, purple, darkText);
            addInfoRow(info, "Entrées analysées :", String.valueOf(journals.size()), bold, regular, purple, darkText);
            Humeur avg = Humeur.fromScore((int) Math.round(avgScore));
            addInfoRow(info, "Score moyen :", String.format("%.1f/5 (%s)", avgScore, avg.getLabel()), bold, regular, purple, green);
            doc.add(info);

            doc.add(new Div().setBackgroundColor(lightPurple).setHeight(3).setMarginBottom(20));

            // History table
            doc.add(new Paragraph("Historique des humeurs").setFont(bold).setFontSize(16).setFontColor(purple).setMarginBottom(10));
            Table hist = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1})).useAllAvailableWidth().setMarginBottom(25);
            hist.addHeaderCell(headerCell("Date",   bold));
            hist.addHeaderCell(headerCell("Humeur", bold));
            hist.addHeaderCell(headerCell("Score",  bold));
            boolean alt = false;
            for (JournalHumeur j : journals) {
                DeviceRgb bg = alt ? lightPurple : new DeviceRgb(255, 255, 255);
                hist.addCell(dataCell(j.getDate() != null ? j.getDate().format(dateFormatter) : "N/A", regular, bg));
                hist.addCell(dataCell(j.getHumeur().getLabel(), regular, bg));
                hist.addCell(dataCell(j.getScore() + "/5", regular, bg));
                alt = !alt;
            }
            doc.add(hist);

            doc.add(new Div().setBackgroundColor(lightPurple).setHeight(3).setMarginBottom(20));

            // AI analysis
            doc.add(new Paragraph("Analyse IA du bien-être").setFont(bold).setFontSize(16).setFontColor(purple).setMarginBottom(10));
            Div analysisBox = new Div().setBackgroundColor(new DeviceRgb(250, 249, 253)).setPadding(20).setMarginBottom(25);
            analysisBox.add(new Paragraph(rapportText).setFont(regular).setFontSize(11).setFontColor(darkText).setMultipliedLeading(1.5f));
            doc.add(analysisBox);

            // Footer
            doc.add(new Paragraph("Généré automatiquement par Harmonie — " + LocalDate.now().format(dateFormatter))
                    .setFont(regular).setFontSize(9).setFontColor(grey).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            new Alert(Alert.AlertType.INFORMATION, "Rapport PDF exporté !\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'exporter le PDF: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    // ── PDF helpers ──────────────────────────────────────────────────────────

    private void addInfoRow(Table t, String label, String value, PdfFont bold, PdfFont regular, DeviceRgb labelColor, DeviceRgb valueColor) {
        t.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(12).setFontColor(labelColor)).setBorder(Border.NO_BORDER).setPadding(5));
        t.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(12).setFontColor(valueColor)).setBorder(Border.NO_BORDER).setPadding(5));
    }

    private Cell headerCell(String text, PdfFont bold) {
        return new Cell().add(new Paragraph(text).setFont(bold).setFontSize(11).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new DeviceRgb(124, 109, 175)).setPadding(8).setTextAlignment(TextAlignment.CENTER);
    }

    private Cell dataCell(String text, PdfFont regular, DeviceRgb bg) {
        return new Cell().add(new Paragraph(text).setFont(regular).setFontSize(10).setFontColor(new DeviceRgb(51, 51, 51)))
                .setBackgroundColor(bg).setPadding(6).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER);
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String getFullName(user u) {
        String prenom = u.getUser_prenom() != null ? u.getUser_prenom() : "";
        String nom    = u.getUser_nom()    != null ? u.getUser_nom()    : "";
        return (prenom + " " + nom).trim();
    }
}
