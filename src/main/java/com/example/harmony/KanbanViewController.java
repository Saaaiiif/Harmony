package com.example.harmony;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import models.StatutTache;
import models.Tache;
import services.TacheService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class KanbanViewController implements Initializable {

    @FXML private VBox todoList;
    @FXML private VBox doingList;
    @FXML private VBox doneList;

    private final TacheService tacheService = new TacheService();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonneDrop(todoList);
        configurerColonneDrop(doingList);
        configurerColonneDrop(doneList);
        chargerTaches();
    }

    private void chargerTaches() {
        todoList.getChildren().clear();
        doingList.getChildren().clear();
        doneList.getChildren().clear();

        List<Tache> taches = tacheService.getAll();
        for (Tache tache : taches) {
            VBox card = creerCarte(tache);
            ajouterCarteDansColonne(card, tache.getStatut());
        }
    }

    private VBox creerCarte(Tache tache) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kanban-card");
        card.setUserData(tache);

        Label titreLabel = new Label(tache.getNom() != null ? tache.getNom() : "Sans titre");
        titreLabel.getStyleClass().add("kanban-card-title");
        titreLabel.setWrapText(true);

        Label deadlineLabel = new Label();
        if (tache.getDeadline() != null) {
            deadlineLabel.setText("📅 " + DATE_FORMAT.format(tache.getDeadline()));
        } else {
            deadlineLabel.setText("📅 Pas de deadline");
        }
        deadlineLabel.getStyleClass().add("kanban-card-deadline");

        Label notesLabel = new Label();
        if (tache.getNotes() != null && !tache.getNotes().trim().isEmpty()) {
            notesLabel.setText(tache.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.getStyleClass().add("kanban-card-notes");
        }

        card.getChildren().addAll(titreLabel, deadlineLabel);
        if (tache.getNotes() != null && !tache.getNotes().trim().isEmpty()) {
            card.getChildren().add(notesLabel);
        }

        // Configuration du drag & drop (carte = source de drag)
        configurerDragAndDrop(card, tache);
        // Drop sur une carte : déplacer vers la colonne de cette carte
        configurerCarteDrop(card, tache);

        return card;
    }

    private void configurerCarteDrop(VBox card, Tache tache) {
        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                try {
                    int draggedId = Integer.parseInt(db.getString());
                    if (draggedId == tache.getId()) {
                        event.setDropCompleted(false);
                        event.consume();
                        return;
                    }
                    VBox targetColumn = (VBox) card.getParent();
                    StatutTache nouveauStatut = determinerStatut(targetColumn);
                    deplacerTache(draggedId, nouveauStatut, targetColumn);
                    event.setDropCompleted(true);
                } catch (Exception e) {
                    event.setDropCompleted(false);
                }
            }
            event.consume();
        });
    }

    private void configurerDragAndDrop(VBox card, Tache tache) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(tache.getId()));
            db.setContent(content);
            card.setOpacity(0.5);
            event.consume();
        });

        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            event.consume();
        });
    }

    private void configurerColonneDrop(VBox colonne) {
        colonne.setOnDragOver(event -> {
            if (event.getGestureSource() != colonne && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        colonne.setOnDragEntered(event -> {
            if (event.getGestureSource() != colonne && event.getDragboard().hasString()) {
                Object src = event.getGestureSource();
                if (src instanceof VBox) {
                    Tache t = (Tache) ((VBox) src).getUserData();
                    if (t != null && determinerStatut(colonne) != t.getStatut()) {
                        colonne.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; -fx-border-color: #2196f3; -fx-border-width: 2; -fx-border-radius: 8;");
                    }
                }
            }
            event.consume();
        });

        colonne.setOnDragExited(event -> {
            colonne.setStyle(null);
            event.consume();
        });

        colonne.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int tacheId = Integer.parseInt(db.getString());
                StatutTache nouveauStatut = determinerStatut(colonne);
                deplacerTache(tacheId, nouveauStatut, colonne);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private StatutTache determinerStatut(VBox colonne) {
        if (colonne == todoList) return StatutTache.A_FAIRE;
        if (colonne == doingList) return StatutTache.EN_COURS;
        if (colonne == doneList) return StatutTache.TERMINEE;
        return StatutTache.A_FAIRE;
    }

    private void deplacerTache(int tacheId, StatutTache nouveauStatut, VBox targetColumn) {
        try {
            Tache tache = tacheService.getById(tacheId);
            if (tache != null && tache.getStatut() != nouveauStatut) {
                tache.setStatut(nouveauStatut);
                tacheService.update(tache);

                // Retirer de toutes les colonnes
                todoList.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });
                doingList.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });
                doneList.getChildren().removeIf(node -> {
                    if (node instanceof VBox) {
                        Tache t = (Tache) ((VBox) node).getUserData();
                        return t != null && t.getId() == tacheId;
                    }
                    return false;
                });

                // Ajouter à la colonne cible
                VBox nouvelleCarte = creerCarte(tache);
                targetColumn.getChildren().add(nouvelleCarte);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ajouterCarteDansColonne(VBox card, StatutTache statut) {
        if (statut == StatutTache.A_FAIRE) {
            todoList.getChildren().add(card);
        } else if (statut == StatutTache.EN_COURS) {
            doingList.getChildren().add(card);
        } else if (statut == StatutTache.TERMINEE) {
            doneList.getChildren().add(card);
        }
    }
}
