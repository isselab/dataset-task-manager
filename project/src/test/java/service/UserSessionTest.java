package service;

import model.UserAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {
    @Test
    void loginLogoutAndSwitchingUsersAreSessionOnly() {
        UserSession session = new UserSession();
        UserAccount alice = new UserAccount("alice", "alice@example.com", "salt", "hash");
        UserAccount bob = new UserAccount("bob", "bob@example.com", "salt", "hash");
        assertFalse(session.isLoggedIn());
        session.login(alice);
        assertEquals("alice", session.currentUser().orElseThrow().username());
        session.logout();
        assertFalse(session.isLoggedIn());
        session.login(bob);
        assertEquals("bob", session.currentUser().orElseThrow().username());
    }
}
