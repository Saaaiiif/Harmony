package controllers.ForumControllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.ForumModels.Categorie;
import services.ForumServices.ServiceCategorie;
import services.ForumServices.SpellCheckService;

public class AddCategorieController {

    private SpellCheckService spellService = new SpellCheckService();

    @FXML private TextField nomField;
    @FXML private TextArea descriptionField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private ServiceCategorie service = new ServiceCategorie();
    @FXML
    private Button backBtn;

    @FXML
    private void handleBack(){
        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.close();
    }


    @FXML
    public void initialize(){

        saveBtn.setOnAction(e -> saveCategorie());
        cancelBtn.setOnAction(e -> closeWindow());
        nomField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // quand on quitte le champ
                String corrected =
                        spellService.correctText(nomField.getText());
                nomField.setText(corrected);
            }
        });

        descriptionField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String corrected =
                        spellService.correctText(descriptionField.getText());
                descriptionField.setText(corrected);
            }
        });


    }

    private void saveCategorie(){

        String nom = spellService.correctText(nomField.getText());
        String desc = spellService.correctText(descriptionField.getText());

        nomField.setText(nom);
        descriptionField.setText(desc);


        if(nom.isEmpty()){
            showAlert("Nom obligatoire !");
            return;
        }

        Categorie c = new Categorie(nom, desc);
        service.add(c);

        showAlert("Catégorie ajoutée !");
        closeWindow();
    }

    private void closeWindow(){
        nomField.getScene().getWindow().hide();
    }

    private void showAlert(String msg){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}