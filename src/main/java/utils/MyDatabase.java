package utils;

import java.sql.*;

public class MyDatabase {

    private static final String DB_NAME = "harmonie";
    private static final String URL_BASE = "jdbc:mysql://localhost:3306/";
    private static final String URL = URL_BASE + DB_NAME + "?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to Harmonie database");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    System.out.println("Connected to the database.");
                } catch (SQLException e) {
                    System.err.println("Database connection failed: " + e.getMessage());
                    throw new SQLException("Failed to connect to the database.", e);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    private void initializeDatabase() {
        try {
            createTables();
            insertDefaultUsers();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        String createUserTable = """
            CREATE TABLE IF NOT EXISTS user (
                id INT PRIMARY KEY AUTO_INCREMENT,
                nom VARCHAR(100) NOT NULL,
                prenom VARCHAR(100) NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                date_naissance DATE,
                date_inscription DATE DEFAULT (CURRENT_DATE),
                role ENUM('ADMIN', 'ETUDIANT') NOT NULL
            )
        """;
        stmt.execute(createUserTable);
        System.out.println("Table 'user' ready.");

        String createSessionTable = """
            CREATE TABLE IF NOT EXISTS session_meditation (
                id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                auteur VARCHAR(255) NOT NULL,
                duree INT NOT NULL,
                theme VARCHAR(255) NOT NULL,
                audio_url VARCHAR(500),
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """;
        stmt.execute(createSessionTable);
        System.out.println("Table 'session_meditation' ready.");

        String createConseilTable = """
            CREATE TABLE IF NOT EXISTS conseil (
                id INT PRIMARY KEY AUTO_INCREMENT,
                session_id INT NOT NULL,
                contenu TEXT NOT NULL,
                FOREIGN KEY (session_id) REFERENCES session_meditation(id) ON DELETE CASCADE
            )
        """;
        stmt.execute(createConseilTable);
        System.out.println("Table 'conseil' ready.");

        String createJournalTable = """
            CREATE TABLE IF NOT EXISTS journal_humeur (
                id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                date DATE DEFAULT (CURRENT_DATE),
                humeur VARCHAR(50) NOT NULL,
                score INT NOT NULL,
                contenu TEXT,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """;
        stmt.execute(createJournalTable);
        System.out.println("Table 'journal_humeur' ready.");

        stmt.close();
    }

    private void insertDefaultUsers() throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM user";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(checkQuery);
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();

        if (count == 0) {
            String insertUsers = """
                INSERT INTO user (nom, prenom, email, password, date_naissance, date_inscription, role) VALUES
                ('Brahmi', 'Sarra', 'sarra.brahmi@esprit.tn', 'sarra123A!', '1990-01-15', CURDATE(), 'ADMIN'),
                ('Brahmi', 'Sarra', 'sarrabrahmi@gmail.com', 'sarra123A!', '2000-05-20', CURDATE(), 'ETUDIANT')
            """;
            PreparedStatement pstmt = connection.prepareStatement(insertUsers);
            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("Default users created (Admin ID=1, Etudiant ID=2)");
        } else {
            System.out.println("Users already exist in database.");
        }
    }
}
