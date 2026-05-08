package com.example.harmony;

public class SessionManager {

    public record UserSession(
            int id,
            String nom,
            String prenom,
            String email,
            String imagePath,
            String typeUtilisateur
    ) {
        public String fullName() { return prenom + " " + nom; }
        public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(typeUtilisateur); }
    }

    private static SessionManager instance;
    private UserSession currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(UserSession user) { this.currentUser = user; }
    public void logout() { this.currentUser = null; }
    public UserSession getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }
    public int getCurrentUserId() { return currentUser != null ? currentUser.id() : -1; }
}
