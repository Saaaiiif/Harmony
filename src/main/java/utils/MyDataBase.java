package utils;

import java.sql.*;

public class MyDataBase {

    private static MyDataBase instance;
    final String URL = "jdbc:mysql://localhost:3306/integration-pi?createDatabaseIfNotExist=true";
    final String USER = "root";
    final String PASS = "";
    private Connection cnx;

    private MyDataBase() {
        try {
            this.cnx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connected to integration-pi database.");
            initializeHarmonieTables();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(URL, USER, PASS);
                System.out.println("Re-connected to database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cnx;
    }

    // =========================================================================
    //  Harmonie tables — created automatically on first run
    // =========================================================================

    private void initializeHarmonieTables() {
        try {
            createHarmonieTables();
            insertDefaultMeditationData();
        } catch (SQLException e) {
            System.err.println("Error initializing Harmonie tables: " + e.getMessage());
        }
    }

    private void createHarmonieTables() throws SQLException {
        Statement stmt = cnx.createStatement();

        // session_meditation table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS session_meditation (
                id       INT PRIMARY KEY AUTO_INCREMENT,
                user_id  INT NOT NULL DEFAULT 1,
                auteur   VARCHAR(255) NOT NULL,
                duree    INT NOT NULL,
                theme    VARCHAR(255) NOT NULL,
                audio_url VARCHAR(500)
            )
        """);
        System.out.println("Table 'session_meditation' ready.");

        // conseil table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS conseil (
                id         INT PRIMARY KEY AUTO_INCREMENT,
                session_id INT NOT NULL,
                contenu    TEXT NOT NULL,
                FOREIGN KEY (session_id) REFERENCES session_meditation(id) ON DELETE CASCADE
            )
        """);
        System.out.println("Table 'conseil' ready.");

        // journal_humeur table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS journal_humeur (
                id       INT PRIMARY KEY AUTO_INCREMENT,
                user_id  INT NOT NULL,
                date     DATE DEFAULT (CURRENT_DATE),
                humeur   VARCHAR(50) NOT NULL,
                score    INT NOT NULL,
                contenu  TEXT
            )
        """);
        System.out.println("Table 'journal_humeur' ready.");

        stmt.close();
    }

    private void insertDefaultMeditationData() throws SQLException {
        // Only insert if tables are empty
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM session_meditation");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();

        if (count == 0) {
            // Insert 3 sample meditation sessions
            PreparedStatement pstmt = cnx.prepareStatement(
                "INSERT INTO session_meditation (user_id, auteur, duree, theme, audio_url) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            // Session 1 — Relaxation
            pstmt.setInt(1, 1); pstmt.setString(2, "Dr. Amina Benlahcen");
            pstmt.setInt(3, 15); pstmt.setString(4, "Relaxation");
            pstmt.setString(5, "https://www.youtube.com/watch?v=inpok4MKVLM");
            pstmt.executeUpdate();
            ResultSet keys1 = pstmt.getGeneratedKeys();
            int s1Id = keys1.next() ? keys1.getInt(1) : -1;
            keys1.close();

            // Session 2 — Gestion du stress
            pstmt.setInt(1, 1); pstmt.setString(2, "Karim Mansouri");
            pstmt.setInt(3, 20); pstmt.setString(4, "Gestion du stress");
            pstmt.setString(5, "https://www.youtube.com/watch?v=O-6f5wQXSu8");
            pstmt.executeUpdate();
            ResultSet keys2 = pstmt.getGeneratedKeys();
            int s2Id = keys2.next() ? keys2.getInt(1) : -1;
            keys2.close();

            // Session 3 — Sommeil
            pstmt.setInt(1, 1); pstmt.setString(2, "Leila Cherif");
            pstmt.setInt(3, 30); pstmt.setString(4, "Sommeil");
            pstmt.setString(5, "https://www.youtube.com/watch?v=aXItOY0sLRY");
            pstmt.executeUpdate();
            ResultSet keys3 = pstmt.getGeneratedKeys();
            int s3Id = keys3.next() ? keys3.getInt(1) : -1;
            keys3.close();

            pstmt.close();

            // Insert conseils for each session
            if (s1Id != -1) insertConseils(s1Id, new String[]{
                "Trouvez un endroit calme et confortable pour pratiquer.",
                "Respirez profondément par le nez pendant 4 secondes, retenez 4 secondes, expirez par la bouche 6 secondes.",
                "Laissez vos pensées passer sans les juger — revenez doucement à votre respiration."
            });
            if (s2Id != -1) insertConseils(s2Id, new String[]{
                "Identifiez la source de votre stress avant de commencer la séance.",
                "Contractez et relâchez chaque groupe musculaire progressivement pour libérer la tension.",
                "Visualisez un lieu sûr et serein pendant toute la durée de la méditation."
            });
            if (s3Id != -1) insertConseils(s3Id, new String[]{
                "Éteignez tous les écrans au moins 30 minutes avant de commencer.",
                "Maintenez une température fraîche dans la pièce (18-20°C) pour favoriser l'endormissement.",
                "Focalisez-vous sur la pesanteur de votre corps — sentez chaque partie de vous se détendre."
            });

            System.out.println("Default meditation sessions and conseils created.");
        } else {
            System.out.println("Meditation sessions already exist.");
        }
    }

    private void insertConseils(int sessionId, String[] conseils) throws SQLException {
        PreparedStatement pstmt = cnx.prepareStatement(
            "INSERT INTO conseil (session_id, contenu) VALUES (?, ?)"
        );
        for (String contenu : conseils) {
            pstmt.setInt(1, sessionId);
            pstmt.setString(2, contenu);
            pstmt.executeUpdate();
        }
        pstmt.close();
    }
}
