package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Evenement;
import models.TypeEvenement;
import services.EvenementService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class CalendarFullViewController implements Initializable {

    @FXML private VBox eventsContainer;

    private final EvenementService evenementService = new EvenementService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerEvenements();
    }

    private void chargerEvenements() {
        eventsContainer.getChildren().clear();

        List<Evenement> evenements = evenementService.getAll();
        if (evenements.isEmpty()) {
            Label emptyLabel = new Label("Aucun événement à afficher");
            emptyLabel.getStyleClass().add("empty-message");
            eventsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Evenement event : evenements) {
            VBox eventCard = creerCarteEvenement(event);
            eventsContainer.getChildren().add(eventCard);
        }
    }

    private VBox creerCarteEvenement(Evenement event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("event-card");

        HBox header = new HBox(10);
        header.getStyleClass().add("event-card-header");

        Label titreLabel = new Label(event.getTitre() != null ? event.getTitre() : "Sans titre");
        titreLabel.getStyleClass().add("event-card-title");

        Label typeLabel = new Label(event.getType() != null ? event.getType().name() : "");
        typeLabel.getStyleClass().add("event-card-type");

        header.getChildren().addAll(titreLabel, typeLabel);

        Label datesLabel = new Label();
        if (event.getDateDebut() != null && event.getDateFin() != null) {
            datesLabel.setText("📅 " + DATE_FORMAT.format(event.getDateDebut()) + " - " + DATE_FORMAT.format(event.getDateFin()));
        }
        datesLabel.getStyleClass().add("event-card-dates");

        Label lieuLabel = new Label();
        if (event.getLieu() != null && !event.getLieu().trim().isEmpty()) {
            lieuLabel.setText("📍 " + event.getLieu());
        }
        lieuLabel.getStyleClass().add("event-card-lieu");

        Label descriptionLabel = new Label();
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            descriptionLabel.setText(event.getDescription());
            descriptionLabel.setWrapText(true);
        }
        descriptionLabel.getStyleClass().add("event-card-description");

        HBox footer = new HBox(15);
        footer.getStyleClass().add("event-card-footer");

        Label prioriteLabel = new Label("Priorité: " + event.getPriorite());
        prioriteLabel.getStyleClass().add("event-card-priorite");

        Label rappelLabel = new Label(event.isRappelActif() ? "🔔 Rappel activé" : "🔕 Pas de rappel");
        rappelLabel.getStyleClass().add("event-card-rappel");

        footer.getChildren().addAll(prioriteLabel, rappelLabel);

        card.getChildren().addAll(header, datesLabel);
        if (event.getLieu() != null && !event.getLieu().trim().isEmpty()) {
            card.getChildren().add(lieuLabel);
        }
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            card.getChildren().add(descriptionLabel);
        }
        card.getChildren().add(footer);

        return card;
    }
}
