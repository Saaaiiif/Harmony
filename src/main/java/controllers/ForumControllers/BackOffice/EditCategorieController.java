package controllers.ForumControllers.BackOffice;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import models.ForumModels.Categorie;
import services.ForumServices.ServiceCategorie;

public class EditCategorieController {

    @FXML private TextField nomField;
    @FXML private TextArea descriptionField;

    private Categorie categorie;
    private ServiceCategorie service = new ServiceCategorie();

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
        nomField.setText(categorie.getNomCategorie());
        descriptionField.setText(categorie.getDescription());
    }

    @FXML
    private void handleSave() {
        categorie.setNomCategorie(nomField.getText());
        categorie.setDescription(descriptionField.getText());

        service.update(categorie);

        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}