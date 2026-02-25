package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.application.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import services.ServiceActivite;
import services.ServiceConsommation;
import services.ServiceSommeil;
import models.Activite;
import models.Consommation;
import models.Sommeil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AccueilActiviteController {

    // --- NAVIGATION DU FRONT OFFICE ---
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    // --- LABELS DE LA CITATION ---
    @FXML private Label lblQuoteText;
    @FXML private Label lblQuoteAuthor;

    @FXML
    public void initialize() {
        chargerCitation();
    }

    // =========================================================================
    // LOGIQUE API CITATION
    // =========================================================================

    @FXML
    void changerCitation(ActionEvent event) {
        lblQuoteText.setText("✨ Recherche d'une nouvelle inspiration...");
        lblQuoteAuthor.setText("—");
        chargerCitation();
    }

    private void chargerCitation() {
        Thread t = new Thread(() -> {
            try {
                // java.time.Duration utilisé en nom complet pour éviter le conflit avec javafx.util.Duration
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(8))
                        .build();

                // Étape 1 : Récupérer la citation en anglais
                HttpRequest reqZen = HttpRequest.newBuilder()
                        .uri(URI.create("https://zenquotes.io/api/random"))
                        .timeout(java.time.Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> resZen = client.send(reqZen, HttpResponse.BodyHandlers.ofString());

                if (resZen.statusCode() != 200 || resZen.body() == null || resZen.body().isBlank()) {
                    afficherCitationSecours();
                    return;
                }

                String jsonZen = resZen.body();
                String quoteEn = extraireStringJSON(jsonZen, "\"q\":\"");
                String author  = extraireStringJSON(jsonZen, "\"a\":\"");

                if (quoteEn.isEmpty()) {
                    afficherCitationSecours();
                    return;
                }

                // Étape 2 : Traduire en français
                String quoteFr = quoteEn;
                try {
                    String encodedQuote = URLEncoder.encode(quoteEn, StandardCharsets.UTF_8);
                    HttpRequest reqTrad = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.mymemory.translated.net/get?q=" + encodedQuote + "&langpair=en|fr"))
                            .timeout(java.time.Duration.ofSeconds(8))
                            .GET()
                            .build();

                    HttpResponse<String> resTrad = client.send(reqTrad, HttpResponse.BodyHandlers.ofString());

                    if (resTrad.statusCode() == 200 && resTrad.body() != null) {
                        String translated = extraireStringJSON(resTrad.body(), "\"translatedText\":\"");
                        if (!translated.isEmpty()
                                && !translated.contains("MYMEMORY WARNING")
                                && !translated.equalsIgnoreCase(quoteEn)) {
                            quoteFr = translated;
                        }
                    }
                } catch (Exception tradEx) {
                    System.out.println("Traduction non disponible, affichage en anglais.");
                }

                // Étape 3 : Afficher avec animation (Duration ici = javafx.util.Duration, pas de conflit)
                final String finalQuote  = nettoyerTexte(quoteFr);
                final String finalAuthor = nettoyerTexte(author);

                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), lblQuoteText);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(e -> {
                        lblQuoteText.setText("\u201C" + finalQuote + "\u201D");
                        lblQuoteAuthor.setText("— " + (finalAuthor.isEmpty() ? "Auteur inconnu" : finalAuthor));

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), lblQuoteText);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();

                        FadeTransition fadeInAuthor = new FadeTransition(Duration.millis(600), lblQuoteAuthor);
                        fadeInAuthor.setFromValue(0.0);
                        fadeInAuthor.setToValue(1.0);
                        fadeInAuthor.play();
                    });
                    fadeOut.play();
                });

            } catch (Exception e) {
                System.out.println("API citation inaccessible : " + e.getMessage());
                afficherCitationSecours();
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void afficherCitationSecours() {
        String[][] citations = {
                {"\u201CPrenez soin de votre corps. C\u2019est le seul endroit o\u00f9 vous \u00eates oblig\u00e9 de vivre.\u201D", "Jim Rohn"},
                {"\u201CLa sant\u00e9 est la chose la plus importante.\u201D", "Neil Young"},
                {"\u201CChaque jour est une nouvelle chance de changer votre vie.\u201D", "Anonyme"},
                {"\u201CLe mouvement, c\u2019est la vie.\u201D", "Hippocrate"}
        };
        int index = (int) (Math.random() * citations.length);
        final String quote  = citations[index][0];
        final String author = citations[index][1];

        Platform.runLater(() -> {
            lblQuoteText.setText(quote);
            lblQuoteAuthor.setText("\u2014 " + author);
        });
    }

    // =========================================================================
    // OUTILS JSON
    // =========================================================================

    private String extraireStringJSON(String json, String key) {
        try {
            int index = json.indexOf(key);
            if (index == -1) return "";
            index += key.length();
            StringBuilder sb = new StringBuilder();
            while (index < json.length()) {
                char c = json.charAt(index);
                if (c == '"' && (index == 0 || json.charAt(index - 1) != '\\')) break;
                sb.append(c);
                index++;
            }
            return sb.toString().replace("\\\"", "\"").replace("\\/", "/");
        } catch (Exception e) {
            return "";
        }
    }

    private String nettoyerTexte(String text) {
        if (text == null || text.isBlank()) return "";
        return text
                .replace("\\u00e9", "\u00e9").replace("\\u00e8", "\u00e8")
                .replace("\\u00ea", "\u00ea").replace("\\u00e0", "\u00e0")
                .replace("\\u00e2", "\u00e2").replace("\\u00ee", "\u00ee")
                .replace("\\u00f4", "\u00f4").replace("\\u00fb", "\u00fb")
                .replace("\\u00e7", "\u00e7").replace("\\u00f9", "\u00f9")
                .replace("\\u2019", "\u2019").replace("\\u0027", "'")
                .replace("\\n", " ").replace("\\r", "")
                .trim();
    }

    // =========================================================================
    // NAVIGATION BACK-OFFICE
    // =========================================================================

    @FXML
    void goToBackOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/root-layout.fxml"));
            Parent root = loader.load();
            controllers.RootLayoutController controller = loader.getController();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (controller != null) controller.setStage(stage);

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // GÉNÉRATION BILAN PDF
    // =========================================================================

    @FXML
    void genererBilanPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Bilan de Santé");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Bilan_Harmony_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");

        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(mainStage);

        if (file != null) {
            try {
                ServiceActivite sa = new ServiceActivite();
                ServiceConsommation sc = new ServiceConsommation();
                ServiceSommeil ss = new ServiceSommeil();

                List<Activite> activites = sa.afficherTout();
                List<Consommation> repas = sc.afficherTout();
                List<Sommeil> nuits = ss.afficherTout();

                int totalSportMin         = activites.stream().mapToInt(Activite::getDuree_minutes).sum();
                int totalCaloriesBrulees  = activites.stream().mapToInt(Activite::getCalories_brulees).sum();
                int totalEauMl            = repas.stream().mapToInt(Consommation::getQuantite_eau_ml).sum();

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                BaseColor mauveFonce  = new BaseColor(106, 27, 154);
                BaseColor grisTexte   = new BaseColor(80, 80, 80);

                Font fontTitre        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, mauveFonce);
                Font fontSousTitre    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, mauveFonce);
                Font fontTexteTableau = FontFactory.getFont(FontFactory.HELVETICA, 12, grisTexte);
                Font fontTableHdr     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

                Paragraph titre = new Paragraph("BILAN GLOBAL DE SANTÉ", fontTitre);
                titre.setAlignment(Element.ALIGN_CENTER);
                document.add(titre);

                Paragraph dateGen = new Paragraph(
                        "Généré par Harmony le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, BaseColor.GRAY));
                dateGen.setAlignment(Element.ALIGN_CENTER);
                dateGen.setSpacingAfter(25f);
                document.add(dateGen);

                // --- SPORT ---
                Paragraph titreSport = new Paragraph("SYNTHESE SPORTIVE", fontSousTitre);
                titreSport.setSpacingAfter(10f);
                document.add(titreSport);

                PdfPTable tableSport = createDesignTable("Indicateur", "Performances", fontTableHdr);
                addRow(tableSport, "Séances d'entraînement réalisées", activites.size() + " séances", fontTexteTableau, true);
                addRow(tableSport, "Temps total d'effort physique", (totalSportMin / 60) + "h " + (totalSportMin % 60) + "m", fontTexteTableau, false);
                addRow(tableSport, "Énergie dépensée (Calories)", totalCaloriesBrulees + " kcal", fontTexteTableau, true);
                document.add(tableSport);

                if (!activites.isEmpty()) {
                    CategoryAxis xAxis = new CategoryAxis();
                    NumberAxis yAxis   = new NumberAxis();
                    xAxis.setAnimated(false);
                    yAxis.setAnimated(false);
                    BarChart<String, Number> chartSport = new BarChart<>(xAxis, yAxis);
                    chartSport.setAnimated(false);
                    chartSport.setTitle("Calories Brûlées par séance");
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    int i = 1;
                    for (Activite a : activites)
                        series.getData().add(new XYChart.Data<>("S" + i++, a.getCalories_brulees()));
                    chartSport.getData().add(series);
                    document.add(convertChartToPdfImage(chartSport, "bar"));
                }

                // --- NUTRITION ---
                document.newPage();
                Paragraph titreNutri = new Paragraph("NUTRITION & HYDRATATION", fontSousTitre);
                titreNutri.setSpacingAfter(10f);
                document.add(titreNutri);

                PdfPTable tableNutri = createDesignTable("Indicateur Nutritionnel", "Valeur", fontTableHdr);
                addRow(tableNutri, "Repas enregistrés", String.valueOf(repas.size()), fontTexteTableau, true);
                addRow(tableNutri, "Volume d'eau total", String.format("%.1f Litres", totalEauMl / 1000.0), fontTexteTableau, false);
                document.add(tableNutri);

                if (!repas.isEmpty()) {
                    CategoryAxis xWater = new CategoryAxis();
                    NumberAxis yWater   = new NumberAxis();
                    xWater.setAnimated(false);
                    yWater.setAnimated(false);
                    LineChart<String, Number> chartEau = new LineChart<>(xWater, yWater);
                    chartEau.setAnimated(false);
                    chartEau.setTitle("Suivi de l'Hydratation (en ml)");
                    XYChart.Series<String, Number> seriesEau = new XYChart.Series<>();
                    int j = 1;
                    for (Consommation c : repas)
                        seriesEau.getData().add(new XYChart.Data<>("R" + j++, c.getQuantite_eau_ml()));
                    chartEau.getData().add(seriesEau);
                    document.add(convertChartToPdfImage(chartEau, "line"));
                }

                // --- SOMMEIL ---
                Paragraph titreSommeil = new Paragraph("\nRECUPERATION & SOMMEIL", fontSousTitre);
                titreSommeil.setSpacingAfter(10f);
                document.add(titreSommeil);

                PdfPTable tableSommeil = createDesignTable("Indicateur de Sommeil", "Donnée", fontTableHdr);
                addRow(tableSommeil, "Nuits complètes enregistrées", String.valueOf(nuits.size()), fontTexteTableau, true);
                document.add(tableSommeil);

                Paragraph conclusion = new Paragraph(
                        "\n\nCe rapport a été généré automatiquement par Harmony.\nContinuez de prendre soin de vous !",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, mauveFonce));
                conclusion.setAlignment(Element.ALIGN_CENTER);
                document.add(conclusion);

                document.close();
                afficherFenetreSucces(mainStage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES PDF
    // =========================================================================

    private PdfPTable createDesignTable(String col1, String col2, Font fontHdr) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(95);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);
        BaseColor headerBg = new BaseColor(106, 27, 154);
        PdfPCell c1 = new PdfPCell(new Phrase(col1, fontHdr));
        c1.setBackgroundColor(headerBg); c1.setPadding(12f); c1.setBorder(PdfPCell.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(col2, fontHdr));
        c2.setBackgroundColor(headerBg); c2.setPadding(12f); c2.setBorder(PdfPCell.NO_BORDER);
        table.addCell(c1);
        table.addCell(c2);
        return table;
    }

    private void addRow(PdfPTable table, String val1, String val2, Font font, boolean isLight) {
        BaseColor bg          = isLight ? new BaseColor(248, 240, 252) : BaseColor.WHITE;
        BaseColor borderColor = new BaseColor(230, 230, 230);
        PdfPCell c1 = new PdfPCell(new Phrase(val1, font));
        c1.setBackgroundColor(bg); c1.setPadding(10f); c1.setBorderColor(borderColor);
        c1.setBorderWidthTop(0); c1.setBorderWidthRight(0); c1.setBorderWidthLeft(0);
        PdfPCell c2 = new PdfPCell(new Phrase(val2, font));
        c2.setBackgroundColor(bg); c2.setPadding(10f); c2.setBorderColor(borderColor);
        c2.setBorderWidthTop(0); c2.setBorderWidthRight(0); c2.setBorderWidthLeft(0);
        table.addCell(c1);
        table.addCell(c2);
    }

    private Image convertChartToPdfImage(Chart chart, String type) throws Exception {
        chart.setLegendVisible(false);
        Scene scene = new Scene(chart, 650, 320);
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        chart.applyCss();
        chart.layout();
        chart.setStyle("-fx-background-color: transparent;");
        Node plotBackground = chart.lookup(".chart-plot-background");
        if (plotBackground != null) plotBackground.setStyle("-fx-background-color: #fbf6fc;");
        if (type.equals("bar")) {
            for (Node n : chart.lookupAll(".default-color0.chart-bar"))
                n.setStyle("-fx-bar-fill: linear-gradient(to top, #6a1b9a, #d500f9); -fx-background-radius: 5 5 0 0;");
        } else if (type.equals("line")) {
            Node line = chart.lookup(".chart-series-line");
            if (line != null) line.setStyle("-fx-stroke: #6a1b9a; -fx-stroke-width: 4px;");
            for (Node symbol : chart.lookupAll(".chart-line-symbol"))
                symbol.setStyle("-fx-background-color: #d500f9, white; -fx-background-insets: 0, 2; -fx-padding: 6px;");
        }
        WritableImage fxImage  = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", baos);
        Image pdfImg = Image.getInstance(baos.toByteArray());
        pdfImg.setAlignment(Element.ALIGN_CENTER);
        pdfImg.scalePercent(75);
        return pdfImg;
    }

    private void afficherFenetreSucces(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #6a1b9a, #d500f9); -fx-background-radius: 25; -fx-padding: 40; -fx-border-color: white; -fx-border-width: 3; -fx-border-radius: 25;");

        Label icon  = new Label("✨ 📄 ✨");
        icon.setStyle("-fx-font-size: 50px;");

        Label title = new Label("Génération Réussie !");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label msg = new Label("Votre bilan professionnel a été exporté avec succès !");
        msg.setStyle("-fx-font-size: 15px; -fx-text-fill: white; -fx-text-alignment: center;");
        msg.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("Génial !");
        closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #6a1b9a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 30; -fx-padding: 10 40; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(icon, title, msg, closeBtn);

        Scene scene = new Scene(root, 450, 300);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(600), root);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(600), root);
        st.setFromX(0.7); st.setFromY(0.7); st.setToX(1.0); st.setToY(1.0); st.play();

        dialog.showAndWait();
    }
}