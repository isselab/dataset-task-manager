package service;

import model.UserAccount;

import java.util.Optional;

public final class UserSession {
    private UserAccount currentUser;

    // &begin[Login]
    public void login(UserAccount user) {
        if (user == null) throw new IllegalArgumentException("A session requires a user");
        currentUser = user;
    }
    // &end[Login]

    // &begin[Logout]
    public void logout() { currentUser = null; }
    // &end[Logout]

    public Optional<UserAccount> currentUser() { return Optional.ofNullable(currentUser); }

    public boolean isLoggedIn() { return currentUser != null; }
}
