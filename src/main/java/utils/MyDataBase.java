package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    // VOS INFOS DE CONNEXION
    // Vérifiez bien que le nom de la base est "projet_pi" comme dans PHPMyAdmin
    private final String URL = "jdbc:mysql://localhost:3306/projet_pi";
    private final String USER = "root";
    private final String PASSWORD = ""; // Laisser vide si vous utilisez XAMPP/WAMP par défaut

    private Connection cnx;
    private static MyDataBase instance;

    // Le constructeur privé (c'est lui qui se connecte)
    private MyDataBase() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base 'projet_pi' établie avec succès !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    // Méthode pour récupérer l'instance unique (Singleton)
    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    // Méthode pour récupérer l'objet Connection
    public Connection getCnx() {
        return cnx;
    }
}