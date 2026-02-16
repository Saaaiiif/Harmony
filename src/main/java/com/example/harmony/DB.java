package com.example.harmony;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DB {
    private static final String PROPS_FILE = "db.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (in == null) throw new RuntimeException("Missing " + PROPS_FILE + " in resources");
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + PROPS_FILE, e);
        }
    }

    private DB() {}

    public static Connection getConnection() throws SQLException {
        String url = PROPS.getProperty("jdbc.url");
        String user = PROPS.getProperty("jdbc.username");
        String pass = PROPS.getProperty("jdbc.password");
        return DriverManager.getConnection(url, user, pass);
    }
}
