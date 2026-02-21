package models;

import java.time.LocalDateTime;

public class Session {
    private static Session instance; // Singleton
    private user currentUser;
    private String token;
    private LocalDateTime expiration;

    private Session() {} // Constructeur privé pour singleton

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void startSession(user user, String token, LocalDateTime expiration) {
        this.currentUser = user;
        this.token = token;
        this.expiration = expiration;
    }

    public user getUser() {
        return currentUser;
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return token != null && currentUser != null && LocalDateTime.now().isBefore(expiration);
    }

    public void clearSession() {
        currentUser = null;
        token = null;
        expiration = null;
    }
}