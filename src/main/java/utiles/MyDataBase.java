package utiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;

    private static final String[] URLS = {
            "jdbc:mysql://localhost:3306/integration_pi",
            "jdbc:mysql://localhost:4306/integration_pi"
    };
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection cnx;

    private MyDataBase() {
        try {
            SQLException last = null;
            for (String url : URLS) {
                try {
                    cnx = DriverManager.getConnection(url, USERNAME, PASSWORD);
                    last = null;
                    break;
                } catch (SQLException e) {
                    last = e;
                }
            }
            if (last != null) throw last;
            System.out.println("connected...");
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

    public Connection getConnection() {
        return cnx;
    }
}
