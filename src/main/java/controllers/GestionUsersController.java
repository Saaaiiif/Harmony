package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.user;
import services.serviceUser;

import java.io.IOException;

public class GestionUsersController {

    @FXML private TableView<user> tableUsers;
    @FXML private TableColumn<user, Integer> colId;
    @FXML private TableColumn<user, String> colNom;
    @FXML private TableColumn<user, String> colPrenom;
    @FXML private TableColumn<user, String> colEmail;
    @FXML private TableColumn<user, String> colRole;
    @FXML private TableColumn<user, Void> colActions;   // Nouvelle colonne Actions

    private final serviceUser service = new serviceUser();
    private ObservableList<user> observableList;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("user_nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("user_prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("user_email"));
        colRole.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getType_utilisateur().name()));

        // Colonne Actions avec boutons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("\uD83D\uDD27");
            private final Button btnDelete = new Button("❌");

            {
                btnEdit.setStyle("-fx-background-color: #701fc7; -fx-text-fill: white; -fx-font-size: 16;");
                btnDelete.setStyle("-fx-background-color: #b487e3; -fx-text-fill: white; -fx-font-size: 16;");

                btnEdit.setOnAction(e -> editUser(getTableRow().getItem()));
                btnDelete.setOnAction(e -> deleteUser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8, btnEdit, btnDelete);
                    setGraphic(hbox);
                }
            }
        });

        observableList = FXCollections.observableArrayList(service.getAll());
        tableUsers.setItems(observableList);
    }

    private void deleteUser(user selected) {
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet utilisateur ?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            service.deleteById(selected.getUser_id());
            observableList.remove(selected);
        }
    }

    private void editUser(user selected) {
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EditUserPopup.fxml"));
            Parent root = loader.load();

            EditUserPopupController popupController = loader.getController();
            popupController.setUser(selected);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Utilisateur");
            popupStage.setScene(new Scene(root));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.showAndWait();

            // Rafraîchir après fermeture de la popup
            observableList.setAll(service.getAll());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void refreshTable() {
        observableList.setAll(service.getAll());
    }
}