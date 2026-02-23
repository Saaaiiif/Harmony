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
    // === GÉNÉRATION DU BILAN PDF PREMIUM (CHARTE GRAPHIQUE MAUVE) ===
    // =========================================================================

    @FXML
    void genererBilanPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Bilan de Santé");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Bilan_Premium_Harmony_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");

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

                int totalSportMin = activites.stream().mapToInt(Activite::getDuree_minutes).sum();
                int totalCaloriesBrulees = activites.stream().mapToInt(Activite::getCalories_brulees).sum();
                int totalEauMl = repas.stream().mapToInt(Consommation::getQuantite_eau_ml).sum();

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // COULEURS DE LA CHARTE HARMONY
                BaseColor mauveFonce = new BaseColor(106, 27, 154); // #6a1b9a
                BaseColor grisTexte = new BaseColor(80, 80, 80);

                // POLICES PROFESSIONNELLES
                Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, mauveFonce);
                Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, mauveFonce);
                Font fontTexteTableau = FontFactory.getFont(FontFactory.HELVETICA, 12, grisTexte);
                Font fontTableHdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

                // --- EN-TÊTE DESIGN ---
                Paragraph titre = new Paragraph("BILAN GLOBAL DE SANTÉ", fontTitre);
                titre.setAlignment(Element.ALIGN_CENTER);
                document.add(titre);

                Paragraph dateGen = new Paragraph("Généré par l'application Harmony le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, BaseColor.GRAY));
                dateGen.setAlignment(Element.ALIGN_CENTER);
                dateGen.setSpacingAfter(25f);
                document.add(dateGen);

                // --- 1. SECTION SPORT ---
                Paragraph titreSport = new Paragraph("🏃 SYNTHÈSE SPORTIVE", fontSousTitre);
                titreSport.setSpacingAfter(10f);
                document.add(titreSport);

                PdfPTable tableSport = createDesignTable("Indicateur", "Performances", fontTableHdr);
                addRow(tableSport, "Séances d'entraînement réalisées", activites.size() + " séances", fontTexteTableau, true);
                addRow(tableSport, "Temps total d'effort physique", (totalSportMin / 60) + "h " + (totalSportMin % 60) + "m", fontTexteTableau, false);
                addRow(tableSport, "Énergie dépensée (Calories)", totalCaloriesBrulees + " kcal", fontTexteTableau, true);
                document.add(tableSport);

                // Graphique Sport (BarChart stylisé en mauve)
                if (!activites.isEmpty()) {
                    CategoryAxis xAxis = new CategoryAxis();
                    NumberAxis yAxis = new NumberAxis();

                    // CORRECTION CRUCIALE : Désactiver l'animation DES AXES pour l'export off-screen !
                    xAxis.setAnimated(false);
                    yAxis.setAnimated(false);

                    BarChart<String, Number> chartSport = new BarChart<>(xAxis, yAxis);
                    chartSport.setAnimated(false); // Désactiver l'animation globale

                    chartSport.setTitle("Évolution des Calories Brûlées par séance");
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    int i = 1;
                    for (Activite a : activites) {
                        series.getData().add(new XYChart.Data<>("S" + i++, a.getCalories_brulees()));
                    }
                    chartSport.getData().add(series);
                    document.add(convertChartToPdfImage(chartSport, "bar"));
                }

                // --- 2. SECTION NUTRITION ---
                document.newPage(); // Nouvelle page
                Paragraph titreNutri = new Paragraph("🍏 NUTRITION & HYDRATATION", fontSousTitre);
                titreNutri.setSpacingAfter(10f);
                document.add(titreNutri);

                PdfPTable tableNutri = createDesignTable("Indicateur Nutritionnel", "Valeur", fontTableHdr);
                addRow(tableNutri, "Repas enregistrés dans le journal", String.valueOf(repas.size()), fontTexteTableau, true);
                addRow(tableNutri, "Volume d'eau total bu", String.format("%.1f Litres", totalEauMl / 1000.0), fontTexteTableau, false);
                document.add(tableNutri);

                // Graphique Nutrition (LineChart stylisé avec courbe mauve)
                if (!repas.isEmpty()) {
                    CategoryAxis xWater = new CategoryAxis();
                    NumberAxis yWater = new NumberAxis();

                    // CORRECTION CRUCIALE AUSSI ICI
                    xWater.setAnimated(false);
                    yWater.setAnimated(false);

                    LineChart<String, Number> chartEau = new LineChart<>(xWater, yWater);
                    chartEau.setAnimated(false);

                    chartEau.setTitle("Suivi de l'Hydratation (en ml)");
                    XYChart.Series<String, Number> seriesEau = new XYChart.Series<>();
                    int j = 1;
                    for (Consommation c : repas) {
                        seriesEau.getData().add(new XYChart.Data<>("R" + j++, c.getQuantite_eau_ml()));
                    }
                    chartEau.getData().add(seriesEau);
                    document.add(convertChartToPdfImage(chartEau, "line"));
                }

                // --- 3. SECTION SOMMEIL ---
                Paragraph titreSommeil = new Paragraph("\n💤 RÉCUPÉRATION & SOMMEIL", fontSousTitre);
                titreSommeil.setSpacingAfter(10f);
                document.add(titreSommeil);

                PdfPTable tableSommeil = createDesignTable("Indicateur de Sommeil", "Donnée", fontTableHdr);
                addRow(tableSommeil, "Nuits complètes enregistrées", String.valueOf(nuits.size()), fontTexteTableau, true);
                document.add(tableSommeil);

                // Fin du document
                Paragraph conclusion = new Paragraph("\n\nCe rapport a été généré automatiquement par l'intelligence de votre application Harmony.\nContinuez de prendre soin de vous !", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, mauveFonce));
                conclusion.setAlignment(Element.ALIGN_CENTER);
                document.add(conclusion);

                document.close();

                // Lancer la fenêtre modale PRÉCÉDENTE ("Génial !")
                afficherFenetreSucces(mainStage);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur de génération : " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // === MÉTHODES UTILITAIRES POUR LE DESIGN PDF ET L'INTERFACE ===
    // =========================================================================

    // Créer l'en-tête d'un tableau très design
    private PdfPTable createDesignTable(String col1, String col2, Font fontHdr) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(95);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);

        BaseColor headerBg = new BaseColor(106, 27, 154); // Mauve Harmony

        PdfPCell c1 = new PdfPCell(new Phrase(col1, fontHdr));
        c1.setBackgroundColor(headerBg);
        c1.setPadding(12f);
        c1.setBorder(PdfPCell.NO_BORDER);

        PdfPCell c2 = new PdfPCell(new Phrase(col2, fontHdr));
        c2.setBackgroundColor(headerBg);
        c2.setPadding(12f);
        c2.setBorder(PdfPCell.NO_BORDER);

        table.addCell(c1);
        table.addCell(c2);
        return table;
    }

    // Ajouter une ligne avec une couleur alternée très discrète
    private void addRow(PdfPTable table, String val1, String val2, Font font, boolean isLight) {
        BaseColor bg = isLight ? new BaseColor(248, 240, 252) : BaseColor.WHITE; // Violet très clair
        BaseColor borderColor = new BaseColor(230, 230, 230); // Gris léger

        PdfPCell c1 = new PdfPCell(new Phrase(val1, font));
        c1.setBackgroundColor(bg);
        c1.setPadding(10f);
        c1.setBorderColor(borderColor);
        c1.setBorderWidthTop(0); c1.setBorderWidthRight(0); c1.setBorderWidthLeft(0);

        PdfPCell c2 = new PdfPCell(new Phrase(val2, font));
        c2.setBackgroundColor(bg);
        c2.setPadding(10f);
        c2.setBorderColor(borderColor);
        c2.setBorderWidthTop(0); c2.setBorderWidthRight(0); c2.setBorderWidthLeft(0);

        table.addCell(c1);
        table.addCell(c2);
    }

    // Appliquer le CSS Mauve au graphique puis le convertir en image PDF
    private Image convertChartToPdfImage(Chart chart, String type) throws Exception {
        chart.setLegendVisible(false); // Retirer la légende inutile

        // Rendu en mémoire obligatoire pour que le CSS s'applique et que les barres se dessinent
        Scene scene = new Scene(chart, 650, 320);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        chart.applyCss();
        chart.layout();

        // Application de la charte graphique Mauve Harmony
        chart.setStyle("-fx-background-color: transparent;");
        Node plotBackground = chart.lookup(".chart-plot-background");
        if (plotBackground != null) {
            plotBackground.setStyle("-fx-background-color: #fbf6fc;"); // Fond de grille subtil
        }

        if (type.equals("bar")) {
            for (Node n : chart.lookupAll(".default-color0.chart-bar")) {
                n.setStyle("-fx-bar-fill: linear-gradient(to top, #6a1b9a, #d500f9); -fx-background-radius: 5 5 0 0;");
            }
        } else if (type.equals("line")) {
            Node line = chart.lookup(".chart-series-line");
            if (line != null) {
                line.setStyle("-fx-stroke: #6a1b9a; -fx-stroke-width: 4px;");
            }
            for (Node symbol : chart.lookupAll(".chart-line-symbol")) {
                symbol.setStyle("-fx-background-color: #d500f9, white; -fx-background-insets: 0, 2; -fx-padding: 6px;");
            }
        }

        WritableImage fxImage = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", baos);

        Image pdfImg = Image.getInstance(baos.toByteArray());
        pdfImg.setAlignment(Element.ALIGN_CENTER);
        pdfImg.scalePercent(75);
        return pdfImg;
    }

    // L'ancienne fenêtre de succès "Génial !" demandée
    private void afficherFenetreSucces(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #6a1b9a, #d500f9); -fx-background-radius: 25; -fx-padding: 40; -fx-border-color: white; -fx-border-width: 3; -fx-border-radius: 25;");

        Label icon = new Label("✨ 📄 ✨");
        icon.setStyle("-fx-font-size: 50px;");

        Label title = new Label("Génération Réussie !");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label msg = new Label("Votre bilan professionnel a été exporté.\nLes graphiques ont été intégrés avec succès !");
        msg.setStyle("-fx-font-size: 15px; -fx-text-fill: white; -fx-text-alignment: center;");
        msg.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("Génial !");
        closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #6a1b9a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 30; -fx-padding: 10 40; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");

        // Animation au survol du bouton
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #6a1b9a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 30; -fx-padding: 10 40; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #6a1b9a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 30; -fx-padding: 10 40; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);"));

        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(icon, title, msg, closeBtn);

        Scene scene = new Scene(root, 450, 300);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Animations d'apparition
        FadeTransition ft = new FadeTransition(Duration.millis(600), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(600), root);
        st.setFromX(0.7); st.setFromY(0.7);
        st.setToX(1.0); st.setToY(1.0);
        st.play();

        dialog.showAndWait();
    }
}