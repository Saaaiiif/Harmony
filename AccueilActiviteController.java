package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import services.ServiceActivite;
import services.ServiceConsommation;
import services.ServiceSommeil;
import models.Activite;
import models.Consommation;
import models.Sommeil;

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

    // --- NAVIGATION VERS LE BACK OFFICE (ADMIN) ---
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
    // === NOUVELLE FONCTIONNALITÉ : GÉNÉRATION DU BILAN PDF ===
    // =========================================================================

    @FXML
    void genererBilanPDF(ActionEvent event) {
        // 1. Choix de l'emplacement de sauvegarde
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Bilan de Santé");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Bilan_Harmony_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                // 2. Récupération des données statistiques
                ServiceActivite sa = new ServiceActivite();
                ServiceConsommation sc = new ServiceConsommation();
                ServiceSommeil ss = new ServiceSommeil();

                List<Activite> activites = sa.afficherTout();
                List<Consommation> repas = sc.afficherTout();
                List<Sommeil> nuits = ss.afficherTout();

                int totalSportMin = activites.stream().mapToInt(Activite::getDuree_minutes).sum();
                int totalCaloriesBrulees = activites.stream().mapToInt(Activite::getCalories_brulees).sum();
                int totalEauMl = repas.stream().mapToInt(Consommation::getQuantite_eau_ml).sum();
                int nbNuitsEnregistrees = nuits.size();

                // 3. Création du PDF avec iText
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // -- Styles de Polices --
                Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, BaseColor.MAGENTA.darker());
                Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
                Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.BLACK);
                Font fontDate = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, BaseColor.GRAY);

                // -- En-tête --
                Paragraph titre = new Paragraph("Bilan de Santé - Harmony", fontTitre);
                titre.setAlignment(Element.ALIGN_CENTER);
                document.add(titre);

                Paragraph dateGen = new Paragraph("Généré le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), fontDate);
                dateGen.setAlignment(Element.ALIGN_CENTER);
                dateGen.setSpacingAfter(30f);
                document.add(dateGen);

                // -- Section Sport --
                Paragraph sectionSport = new Paragraph("🏃 Bilan Sportif", fontSousTitre);
                sectionSport.setSpacingBefore(10f);
                document.add(sectionSport);
                document.add(new Paragraph("• Heures totales de sport : " + (totalSportMin / 60) + "h " + (totalSportMin % 60) + "m", fontTexte));
                document.add(new Paragraph("• Calories totales brûlées : " + totalCaloriesBrulees + " kcal", fontTexte));
                document.add(new Paragraph("• Séances réalisées : " + activites.size() + " séances", fontTexte));

                // -- Section Nutrition --
                Paragraph sectionAliment = new Paragraph("\n🍏 Bilan Nutritionnel", fontSousTitre);
                document.add(sectionAliment);
                document.add(new Paragraph("• Nombre de repas enregistrés : " + repas.size() + " repas", fontTexte));
                document.add(new Paragraph("• Volume total d'eau bu : " + (totalEauMl / 1000.0) + " Litres", fontTexte));

                // -- Section Sommeil --
                Paragraph sectionSommeil = new Paragraph("\n💤 Bilan Sommeil", fontSousTitre);
                document.add(sectionSommeil);
                document.add(new Paragraph("• Nuits enregistrées : " + nbNuitsEnregistrees + " nuits", fontTexte));

                // Petit mot de fin
                Paragraph conclusion = new Paragraph("\n\nContinuez vos efforts, l'équipe Harmony est fière de vous !", fontDate);
                conclusion.setAlignment(Element.ALIGN_CENTER);
                document.add(conclusion);

                // 4. Fermeture
                document.close();

                // Alerte de succès
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText("Génération réussie");
                alert.setContentText("Votre bilan PDF a été enregistré avec succès !");
                alert.showAndWait();

            } catch (Exception e) {
                // Alerte d'erreur
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Échec de la génération");
                alert.setContentText("Une erreur s'est produite : " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}