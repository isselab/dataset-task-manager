package service;

import model.UserAccount;
import org.junit.jupiter.api.Test;
import repository.JsonUserAccountRepository;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    @Test
    void registersAndAuthenticatesWithUsernameOrEmail() throws Exception {
        var file = Files.createTempFile("accounts", ".json");
        var service = new AuthenticationService(new JsonUserAccountRepository(file));
        assertNull(service.register("alice", "alice@example.com", "secret123", "secret123"));
        assertNotNull(service.authenticate("alice", "secret123"));
        assertNotNull(service.authenticate("alice@example.com", "secret123"));
        assertNull(service.authenticate("alice", "wrong-password"));
        assertFalse(Files.readString(file).contains("secret123"));
        Files.deleteIfExists(file);
    }

    @Test
    void rejectsDuplicateUsernameAndEmail() throws Exception {
        var file = Files.createTempFile("accounts", ".json");
        var service = new AuthenticationService(new JsonUserAccountRepository(file));
        assertNull(service.register("alice", "alice@example.com", "secret123", "secret123"));
        assertTrue(service.register("Alice", "other@example.com", "secret123", "secret123").contains("username"));
        assertTrue(service.register("bob", "ALICE@example.com", "secret123", "secret123").contains("email"));
        Files.deleteIfExists(file);
    }
}
