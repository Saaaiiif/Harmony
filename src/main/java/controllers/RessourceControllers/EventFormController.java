package controllers.RessourceControllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.RessourceModels.Evenement;
import models.RessourceModels.Salle;
import models.RessourceModels.StatutDemandeSalle;
import models.RessourceModels.TypeEvenement;
import services.RessourceServices.SalleService;

import java.net.URL;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class EventFormController implements Initializable {

    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private DatePicker fieldDateDebut;
    @FXML private DatePicker fieldDateFin;
    @FXML private Spinner<Integer> fieldHeureDebut;
    @FXML private Spinner<Integer> fieldMinuteDebut;
    @FXML private Spinner<Integer> fieldHeureFin;
    @FXML private Spinner<Integer> fieldMinuteFin;
    @FXML private TextField fieldLieu;
    @FXML private ComboBox<String> fieldMode;
    @FXML private VBox salleChoiceBox;
    @FXML private ComboBox<Salle> fieldSalle;
    @FXML private Label salleEmptyLabel;
    @FXML private Spinner<Integer> fieldPriorite;
    @FXML private CheckBox fieldRappelActif;
    @FXML private ComboBox<String> fieldType;

    private final SalleService salleService = new SalleService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (fieldPriorite != null) {
            fieldPriorite.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        }
        if (fieldType != null) {
            fieldType.getItems().setAll("COURS", "REUNION", "LOISIR");
            if (!fieldType.getItems().isEmpty()) {
                fieldType.getSelectionModel().selectFirst();
            }
        }
        if (fieldMode != null) {
            fieldMode.getItems().setAll("Présentiel", "En ligne");
            fieldMode.getSelectionModel().selectFirst();
            fieldMode.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> majVisibiliteSalle());
        }
        if (fieldHeureDebut != null) {
            fieldHeureDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        }
        if (fieldMinuteDebut != null) {
            fieldMinuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        }
        if (fieldHeureFin != null) {
            fieldHeureFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 17));
        }
        if (fieldMinuteFin != null) {
            fieldMinuteFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        }
        if (fieldSalle != null) {
            chargerSalles();
            majVisibiliteSalle();
            fieldSalle.setConverter(new javafx.util.StringConverter<Salle>() {
                @Override
                public String toString(Salle s) {
                    return s != null ? s.getNom() + " (cap. " + s.getCapacite() + ")" : "";
                }
                @Override
                public Salle fromString(String s) { return null; }
            });
        }
        if (fieldLieu != null) {
            fieldLieu.textProperty().addListener((o, oldVal, newVal) -> majVisibiliteSalle());
        }
        majVisibiliteSalle();
    }

    private void chargerSalles() {
        List<Salle> salles = salleService.getAll();
        fieldSalle.getItems().clear();
        fieldSalle.getItems().addAll(salles);
    }

    private void majVisibiliteSalle() {
        boolean isEnLigne = fieldMode != null && "en ligne".equalsIgnoreCase(
                fieldMode.getSelectionModel().getSelectedItem() != null ? fieldMode.getSelectionModel().getSelectedItem() : ""
        );
        if (fieldLieu != null) {
            fieldLieu.setVisible(!isEnLigne);
            fieldLieu.setManaged(!isEnLigne);
            if (isEnLigne) fieldLieu.clear();
        }
        if (salleChoiceBox == null) return;
        String lieu = fieldLieu != null && fieldLieu.getText() != null ? fieldLieu.getText().trim().toLowerCase() : "";
        boolean show = !isEnLigne && lieu.contains("esprit");
        salleChoiceBox.setVisible(show);
        salleChoiceBox.setManaged(show);
        if (!show) {
            if (fieldSalle != null) fieldSalle.getSelectionModel().clearSelection();
            if (salleEmptyLabel != null) salleEmptyLabel.setVisible(false);
        } else {
            boolean hasSalles = fieldSalle != null && !fieldSalle.getItems().isEmpty();
            if (salleEmptyLabel != null) {
                salleEmptyLabel.setVisible(!hasSalles);
                salleEmptyLabel.setManaged(!hasSalles);
            }
            if (fieldSalle != null) {
                fieldSalle.setVisible(hasSalles);
                fieldSalle.setManaged(hasSalles);
            }
        }
    }

    public void initFrom(Evenement e) {
        if (e == null) return;
        if (fieldTitre != null) fieldTitre.setText(e.getTitre() != null ? e.getTitre() : "");
        if (fieldDescription != null) fieldDescription.setText(e.getDescription() != null ? e.getDescription() : "");
        if (fieldDateDebut != null) fieldDateDebut.setValue(dateToLocalDate(e.getDateDebut()));
        if (fieldDateFin != null) fieldDateFin.setValue(dateToLocalDate(e.getDateFin()));
        if (e.getDateDebut() != null) {
            LocalDateTime ldt = dateToLocalDateTime(e.getDateDebut());
            if (fieldHeureDebut != null) fieldHeureDebut.getValueFactory().setValue(ldt.getHour());
            if (fieldMinuteDebut != null) fieldMinuteDebut.getValueFactory().setValue(ldt.getMinute());
        }
        if (e.getDateFin() != null) {
            LocalDateTime ldt = dateToLocalDateTime(e.getDateFin());
            if (fieldHeureFin != null) fieldHeureFin.getValueFactory().setValue(ldt.getHour());
            if (fieldMinuteFin != null) fieldMinuteFin.getValueFactory().setValue(ldt.getMinute());
        }
        if (fieldLieu != null) fieldLieu.setText(e.getLieu() != null ? e.getLieu() : "");
        if (fieldMode != null) {
            if (e.getSalleId() != null) {
                fieldMode.getSelectionModel().select("Présentiel");
            } else {
                fieldMode.getSelectionModel().select("En ligne");
            }
        }
        if (fieldPriorite != null) fieldPriorite.getValueFactory().setValue(Math.max(1, Math.min(10, e.getPriorite())));
        if (fieldRappelActif != null) fieldRappelActif.setSelected(e.isRappelActif());
        if (fieldType != null && e.getType() != null) fieldType.getSelectionModel().select(e.getType().name());
        if (e.getSalleId() != null && fieldSalle != null) {
            Salle sel = salleService.getById(e.getSalleId());
            if (sel != null) {
                fieldSalle.getSelectionModel().select(sel);
            }
        }
        majVisibiliteSalle();
    }

    public void initForDate(LocalDate date) {
        if (date == null) return;
        if (fieldDateDebut != null) fieldDateDebut.setValue(date);
        if (fieldDateFin != null) fieldDateFin.setValue(date);
    }

    public Evenement buildEvenement(int id) {
        String titre = (fieldTitre != null && fieldTitre.getText() != null) ? fieldTitre.getText().trim() : "";
        String description = (fieldDescription != null && fieldDescription.getText() != null) ? fieldDescription.getText().trim() : "";
        LocalDate deb = fieldDateDebut != null ? fieldDateDebut.getValue() : null;
        LocalDate fin = fieldDateFin != null ? fieldDateFin.getValue() : null;
        int hD = fieldHeureDebut != null && fieldHeureDebut.getValue() != null ? fieldHeureDebut.getValue() : 9;
        int mD = fieldMinuteDebut != null && fieldMinuteDebut.getValue() != null ? fieldMinuteDebut.getValue() : 0;
        int hF = fieldHeureFin != null && fieldHeureFin.getValue() != null ? fieldHeureFin.getValue() : 17;
        int mF = fieldMinuteFin != null && fieldMinuteFin.getValue() != null ? fieldMinuteFin.getValue() : 0;
        String lieu = (fieldLieu != null && fieldLieu.getText() != null) ? fieldLieu.getText().trim() : "";
        int priorite = (fieldPriorite != null && fieldPriorite.getValue() != null) ? fieldPriorite.getValue() : 1;
        boolean rappel = fieldRappelActif != null && fieldRappelActif.isSelected();
        String typeStr = (fieldType != null && fieldType.getSelectionModel().getSelectedItem() != null) ? fieldType.getSelectionModel().getSelectedItem() : "REUNION";
        TypeEvenement type = TypeEvenement.REUNION;
        try { type = TypeEvenement.valueOf(typeStr); } catch (IllegalArgumentException ignored) { }

        Date dateDebut = deb != null ? localDateTimeToDate(LocalDateTime.of(deb.getYear(), deb.getMonthValue(), deb.getDayOfMonth(), hD, mD, 0)) : new Date();
        Date dateFin = fin != null ? localDateTimeToDate(LocalDateTime.of(fin.getYear(), fin.getMonthValue(), fin.getDayOfMonth(), hF, mF, 0)) : new Date();

        Integer salleId = null;
        StatutDemandeSalle statut = null;
        boolean isPresentiel = fieldMode == null || !"en ligne".equalsIgnoreCase(
                fieldMode.getSelectionModel().getSelectedItem() != null ? fieldMode.getSelectionModel().getSelectedItem() : ""
        );
        if (isPresentiel && lieu.toLowerCase().contains("esprit") && fieldSalle != null) {
            Salle s = fieldSalle.getSelectionModel().getSelectedItem();
            if (s != null) {
                salleId = s.getId();
                statut = StatutDemandeSalle.EN_ATTENTE;
            }
        }

        return new Evenement(id, titre, description, dateDebut, dateFin, lieu, priorite, rappel, type, salleId, statut);
    }

    public boolean validate() {
        if (fieldTitre == null || fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            showError("Le titre est obligatoire.");
            return false;
        }
        if (fieldDateDebut == null || fieldDateDebut.getValue() == null) {
            showError("La date de début est obligatoire.");
            return false;
        }
        if (fieldDateFin == null || fieldDateFin.getValue() == null) {
            showError("La date de fin est obligatoire.");
            return false;
        }
        if (fieldDateFin.getValue().isBefore(fieldDateDebut.getValue())) {
            showError("La date de fin doit être après la date de début.");
            return false;
        }
        if (fieldDateDebut.getValue().equals(fieldDateFin.getValue())) {
            int hD = fieldHeureDebut != null && fieldHeureDebut.getValue() != null ? fieldHeureDebut.getValue() : 0;
            int mD = fieldMinuteDebut != null && fieldMinuteDebut.getValue() != null ? fieldMinuteDebut.getValue() : 0;
            int hF = fieldHeureFin != null && fieldHeureFin.getValue() != null ? fieldHeureFin.getValue() : 0;
            int mF = fieldMinuteFin != null && fieldMinuteFin.getValue() != null ? fieldMinuteFin.getValue() : 0;
            if (hD > hF || (hD == hF && mD >= mF)) {
                showError("L'heure de fin doit être après l'heure de début.");
                return false;
            }
        }
        String lieu = fieldLieu != null && fieldLieu.getText() != null ? fieldLieu.getText().trim().toLowerCase() : "";
        boolean isPresentiel = fieldMode == null || !"en ligne".equalsIgnoreCase(
                fieldMode.getSelectionModel().getSelectedItem() != null ? fieldMode.getSelectionModel().getSelectedItem() : ""
        );
        if (isPresentiel && lieu.contains("esprit")) {
            if (fieldSalle == null || fieldSalle.getItems().isEmpty()) {
                showError("Aucune salle disponible. L'administrateur doit d'abord ajouter des salles dans le backoffice (onglet Gestion des Salles).");
                return false;
            }
            if (fieldSalle.getSelectionModel().getSelectedItem() == null) {
                showError("Veuillez sélectionner une salle dans la liste.");
                return false;
            }
        }
        return true;
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Champs invalides");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now();
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Date localDateTimeToDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
