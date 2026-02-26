module harmony { // J'ai simplifié le nom du module
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires java.desktop;
    requires org.json;
    requires java.net.http;


    requires itextpdf;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires javafx.media;


    // 1. On autorise JavaFX à démarrer l'application depuis le package 'main'
    exports main;

    // 2. On autorise JavaFX à injecter les @FXML dans tes contrôleurs
    opens controllers to javafx.fxml;
    exports controllers;

    // 3. On exporte le reste de tes packages au cas où d'autres bibliothèques en ont besoin
    exports models;
    exports services;
    exports utils;
    exports interfaces;
}