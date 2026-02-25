# Guide d'Application du Style Professionnel

## 🎯 Comment Styler Vos Composants

Ce guide vous montre comment appliquer le style professionnel de Harmony à vos nouveaux éléments FXML.

---

## 📝 Formulaires

### Formulaire Simple
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<GridPane xmlns:fx="http://javafx.com/fxml"
         fx:controller="com.example.MyController"
         hgap="16" vgap="16"
         styleClass="modern-form"
         prefWidth="500" prefHeight="400">
    
    <columnConstraints>
        <ColumnConstraints minWidth="130" prefWidth="130" hgrow="NEVER"/>
        <ColumnConstraints minWidth="320" prefWidth="320" hgrow="ALWAYS"/>
    </columnConstraints>

    <!-- Champ texte -->
    <Label text="Nom *" GridPane.columnIndex="0" GridPane.rowIndex="0" 
            styleClass="form-label"/>
    <TextField fx:id="fieldName" promptText="Entrez le nom" 
               GridPane.columnIndex="1" GridPane.rowIndex="0" 
               GridPane.hgrow="ALWAYS" styleClass="form-field"/>

    <!-- ComboBox -->
    <Label text="Catégorie" GridPane.columnIndex="0" GridPane.rowIndex="1" 
            styleClass="form-label"/>
    <ComboBox fx:id="fieldCategory" GridPane.columnIndex="1" GridPane.rowIndex="1" 
              styleClass="form-combo"/>

    <!-- TextArea -->
    <Label text="Description" GridPane.columnIndex="0" GridPane.rowIndex="2" 
            GridPane.valignment="TOP" styleClass="form-label"/>
    <TextArea fx:id="fieldDescription" wrapText="true" prefRowCount="3" 
              GridPane.columnIndex="1" GridPane.rowIndex="2" 
              GridPane.hgrow="ALWAYS" styleClass="form-textarea"/>

    <!-- Checkbox -->
    <Label text="Actif" GridPane.columnIndex="0" GridPane.rowIndex="3" 
            styleClass="form-label"/>
    <CheckBox fx:id="fieldActive" text="Activer cet élément" 
              GridPane.columnIndex="1" GridPane.rowIndex="3" 
              styleClass="form-checkbox"/>

    <padding>
        <Insets top="24" right="24" bottom="24" left="24"/>
    </padding>
</GridPane>
```

---

## 🗓️ Vue Calendrier

### Table avec Boutons d'Actions
```xml
<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.MyViewController"
      styleClass="calendar-view"
      spacing="16">
    
    <!-- En-tête avec actions -->
    <HBox alignment="CENTER_LEFT" styleClass="calendar-header toolbar-header">
        <Label text="Mes Éléments" styleClass="calendar-title"/>
        <Region HBox.hgrow="ALWAYS"/>
        <HBox spacing="6" alignment="CENTER_RIGHT" styleClass="toolbar-actions">
            <Button text="+" styleClass="icon-btn btn-add" onAction="#onAdd"/>
            <Button text="✎" styleClass="icon-btn btn-edit" onAction="#onEdit"/>
            <Button text="✕" styleClass="icon-btn btn-delete" onAction="#onDelete"/>
        </HBox>
    </HBox>

    <!-- Table -->
    <TableView fx:id="mainTable" VBox.vgrow="ALWAYS" styleClass="events-table">
        <columns>
            <TableColumn fx:id="colName" text="Nom" prefWidth="200"/>
            <TableColumn fx:id="colDate" text="Date" prefWidth="150"/>
            <TableColumn fx:id="colStatus" text="Statut" prefWidth="100"/>
            <TableColumn fx:id="colActions" text="" prefWidth="100"/>
        </columns>
    </TableView>

    <padding>
        <Insets top="24" right="24" bottom="24" left="24"/>
    </padding>
</VBox>
```

---

## 🎨 Cartes (Cards)

### Affichage de Contenu en Cartes
```xml
<VBox spacing="15" styleClass="events-container">
    <!-- Une carte -->
    <VBox styleClass="event-card">
        <HBox alignment="CENTER_LEFT" styleClass="event-card-header">
            <Label text="Mon Événement" styleClass="event-card-title"/>
            <Label text="Type 1" styleClass="event-card-type"/>
        </HBox>
        
        <Label text="Date: 25 février 2026" styleClass="event-card-dates"/>
        <Label text="Lieu: Salle 101" styleClass="event-card-lieu"/>
        <Label text="Description détaillée ici..." 
               wrapText="true" styleClass="event-card-description"/>
        
        <HBox alignment="CENTER_LEFT" styleClass="event-card-footer">
            <Label text="Priorité: 5/10" styleClass="event-card-priorite"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label text="Rappel: Oui" styleClass="event-card-rappel"/>
        </HBox>
    </VBox>
</VBox>
```

---

## 📊 Tableau Admin

### Table Professionnelle avec Actions
```xml
<VBox spacing="14">
    <!-- En-tête -->
    <HBox alignment="CENTER_LEFT" styleClass="admin-header">
        <Label text="Gestion des Éléments" styleClass="admin-section-title"/>
        <Region HBox.hgrow="ALWAYS"/>
        <HBox spacing="8" alignment="CENTER_RIGHT" styleClass="admin-actions">
            <Button text="Ajouter" styleClass="btn-admin btn-add" 
                    onAction="#onAdd"/>
            <Button text="Modifier" styleClass="btn-admin btn-edit" 
                    onAction="#onEdit"/>
            <Button text="Supprimer" styleClass="btn-admin btn-delete" 
                    onAction="#onDelete"/>
        </HBox>
    </HBox>

    <!-- Table -->
    <TableView fx:id="adminTable" VBox.vgrow="ALWAYS" 
               styleClass="admin-table">
        <columns>
            <TableColumn fx:id="colId" text="ID" prefWidth="50"/>
            <TableColumn fx:id="colName" text="Nom" prefWidth="200"/>
            <TableColumn fx:id="colEmail" text="Email" prefWidth="200"/>
            <TableColumn fx:id="colActions" text="Actions" prefWidth="150"/>
        </columns>
    </TableView>
</VBox>
```

---

## 🔘 Boutons Stylisés

### Différents Types de Boutons
```xml
<!-- Bouton Standard Calendrier -->
<Button text="Enregistrer" styleClass="btn-calendar" onAction="#onSave"/>

<!-- Bouton Admin -->
<Button text="Ajouter" styleClass="btn-admin btn-add"/>
<Button text="Modifier" styleClass="btn-admin btn-edit"/>
<Button text="Supprimer" styleClass="btn-admin btn-delete"/>

<!-- Bouton Icône -->
<Button text="+" styleClass="icon-btn btn-add"/>
<Button text="✎" styleClass="icon-btn btn-edit"/>
<Button text="✕" styleClass="icon-btn btn-delete"/>

<!-- Bouton Action dans Ligne -->
<Button text="✎" styleClass="row-action-btn row-action-edit"/>
<Button text="✕" styleClass="row-action-btn row-action-delete"/>

<!-- Bouton de Fenêtre -->
<Button text="—" styleClass="window-control-button"/>
<Button text="✕" styleClass="window-control-button"/>
```

---

## 🎭 Sections de Contenu

### Barre avec Titre et Actions
```xml
<HBox alignment="CENTER_LEFT" spacing="16" 
      styleClass="calendar-header toolbar-header">
    <Label text="Titre de Section" styleClass="calendar-title"/>
    <Region HBox.hgrow="ALWAYS"/>
    <HBox spacing="6" alignment="CENTER_RIGHT" 
          styleClass="toolbar-actions">
        <Button text="Exporter" styleClass="btn-calendar"/>
    </HBox>
</HBox>
```

### Bloc de Notification
```xml
<Label fx:id="notificationLabel" 
       text="Message important!" 
       styleClass="admin-notification" 
       visible="false" managed="false" 
       wrapText="true"/>
```

### Texte d'Indice
```xml
<Label text="Les utilisateurs doivent sélectionner une salle..." 
       styleClass="admin-hint" wrapText="true"/>
```

---

## 📱 Inputs Spécialisés

### Date et Heure
```xml
<HBox spacing="10">
    <DatePicker fx:id="fieldDate" prefWidth="160" 
                styleClass="form-field"/>
    <Label text="Heure" styleClass="form-sublabel"/>
    <Spinner fx:id="fieldHour" prefWidth="55" 
             styleClass="form-spinner-small"/>
    <Label text=":" styleClass="form-sublabel"/>
    <Spinner fx:id="fieldMinute" prefWidth="55" 
             styleClass="form-spinner-small"/>
</HBox>
```

### Sélection de Salle
```xml
<VBox fx:id="salleContainer" spacing="8" styleClass="salle-choice-box">
    <Label text="Sélectionner une salle" styleClass="form-sublabel"/>
    <ComboBox fx:id="fieldSalle" 
              promptText="Choisir une salle..."
              styleClass="form-combo"/>
    <Label fx:id="emptyLabel" 
           text="Aucune salle disponible."
           visible="false" managed="false"
           styleClass="form-empty-hint" wrapText="true"/>
</VBox>
```

---

## 🎯 Patterns Courants

### Conteneur Flexible
```xml
<Region HBox.hgrow="ALWAYS"/>  <!-- Pousse le reste à droite -->
<Region VBox.vgrow="ALWAYS"/>  <!-- Remplit l'espace vertical -->
```

### Contrôle Visible/Hidden
```xml
<VBox visible="false" managed="false">
    <!-- Invisible et n'occupe pas d'espace -->
</VBox>
```

### Alignement
```xml
<!-- Horizontalement -->
<HBox alignment="CENTER_LEFT"/>     <!-- À gauche -->
<HBox alignment="CENTER"/>          <!-- Centré -->
<HBox alignment="CENTER_RIGHT"/>    <!-- À droite -->

<!-- Verticalement -->
<VBox alignment="TOP_CENTER"/>      <!-- En haut -->
<VBox alignment="CENTER"/>          <!-- Centré -->
<VBox alignment="BOTTOM_CENTER"/>   <!-- En bas -->
```

---

## 🔄 Mode Light/Dark

Tous les styles supportent automatiquement les deux modes. Pour tester:

### Java Code
```java
// Activer le mode clair
scene.getRoot().getStyleClass().add("light-mode");

// Désactiver le mode clair
scene.getRoot().getStyleClass().remove("light-mode");
```

---

## ✅ Checklist de Style

Avant de déployer un nouvel écran:

- [ ] Formulaires utilisent `.modern-form`
- [ ] Labels utilisent `.form-label`
- [ ] Inputs utilisent `.form-field`, `.form-combo`, etc.
- [ ] Tables utilisent `.events-table` ou `.admin-table`
- [ ] Boutons utilisent `.btn-calendar`, `.btn-admin` ou `.icon-btn`
- [ ] En-têtes utilisent `.calendar-title` ou `.admin-section-title`
- [ ] Espacements via HBox/VBox avec `spacing`
- [ ] Padding via `<Insets/>`
- [ ] Testé en mode clair et sombre

---

## 🎨 Personnalisation

### Ajouter une Variante Personnalisée
Si vous avez besoin d'une variante spéciale, créez-la dans `styles.css`:

```css
/* Nouveau style personnalisé */
.my-custom-form {
    -fx-background-color: rgba(200, 180, 230, 0.95);
    -fx-padding: 30;
    -fx-background-radius: 20px;
}

.light-mode .my-custom-form {
    -fx-background-color: rgba(240, 230, 255, 0.98);
}
```

Puis utilisez-le:
```xml
<GridPane styleClass="modern-form my-custom-form">
    ...
</GridPane>
```

---

## 📚 Ressources

- **Fichier principal CSS**: `styles.css` (2000+ lignes)
- **Guide des classes**: `CSS_CLASSES_GUIDE.md`
- **Améliorations appliquées**: `UI_IMPROVEMENTS.md`

---

**Créé**: 25 Février 2026
**Auteur**: GitHub Copilot
**Version**: 1.0

