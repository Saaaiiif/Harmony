package controllers;

import entities.Aliment;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import services.ServiceAliment;

public class AjouterAlimentController {

    // On relie le code aux éléments de ton fichier .fxml grâce à @FXML
    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfCalories;

    // On prépare le service qui va parler à la base de données
    private ServiceAliment sa = new ServiceAliment();

    // C'est la méthode qui s'exécute quand on clique sur le bouton "Ajouter"
    @FXML
    void ajouterAliment(ActionEvent event) {
        // 1. On récupère ce que l'utilisateur a écrit dans les cases
        String nom = tfNom.getText();
        String caloriesTexte = tfCalories.getText();

        try {
            // 2. On transforme le texte des calories en nombre entier
            int calories = Integer.parseInt(caloriesTexte);

            // 3. On crée l'entité et on l'ajoute à la base de données
            Aliment a = new Aliment(nom, calories);
            sa.ajouter(a);

            // 4. On affiche une pop-up de succès (comme sur ton Slide 18 !)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("L'aliment a été ajouté avec succès !");
            alert.show();

            // 5. On vide les cases pour le prochain ajout
            tfNom.clear();
            tfCalories.clear();

        } catch (NumberFormatException e) {
            // Si l'utilisateur tape des lettres au lieu d'un nombre pour les calories
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de saisie");
            alert.setContentText("Veuillez entrer un nombre valide pour les calories.");
            alert.show();
        }
    }
}