package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import models.user;

import java.time.LocalDate;
import java.time.Period;

public class DisplayUserController {

    @FXML private Label fullNameLabel, emailLabel, ageLabel, dateNaissanceLabel, dateInscriptionLabel, roleLabel;
    @FXML private Label sexeLabel, poidsLabel, tailleLabel, niveauActiviteLabel;
    @FXML private Label niveauScolaireLabel, etablissementLabel;

    public void setUser(user u) {
        fullNameLabel.setText(u.getUser_prenom() + " " + u.getUser_nom());
        emailLabel.setText(u.getUser_email());

        int age = calculateAge(u.getUser_date_de_naissance());
        ageLabel.setText("Âge: " + age + " ans");
        dateNaissanceLabel.setText("Date Naissance: " + u.getUser_date_de_naissance());
        dateInscriptionLabel.setText("Date Inscription: " + u.getDate_inscription());
        roleLabel.setText("Rôle: " + u.getType_utilisateur().name());

        sexeLabel.setText("Sexe: " + (u.getUser_sexe() != null ? u.getUser_sexe().getDisplayName() : "N/A"));
        poidsLabel.setText("Poids: " + (u.getUser_poids() != null ? u.getUser_poids() + " kg" : "N/A"));
        tailleLabel.setText("Taille: " + (u.getUser_taille() != null ? u.getUser_taille() + " cm" : "N/A"));
        niveauActiviteLabel.setText("Niveau Activité: " + (u.getUser_niveau_activite_physique() != null ? u.getUser_niveau_activite_physique().getDisplayName() : "N/A"));

        niveauScolaireLabel.setText("Niveau Scolaire: " + (u.getUser_niveau_scolaire() != null ? u.getUser_niveau_scolaire().getDisplayName() : "N/A"));
        etablissementLabel.setText("Établissement: " + (u.getUser_etablissement_scolaire() != null ? u.getUser_etablissement_scolaire() : "N/A"));
    }

    private int calculateAge(String birthDateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            LocalDate currentDate = LocalDate.now();
            return Period.between(birthDate, currentDate).getYears();
        } catch (Exception e) {
            return 0;
        }
    }
}