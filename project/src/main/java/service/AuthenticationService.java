package service;

import model.UserAccount;
import repository.JsonUserAccountRepository;
import repository.UserAccountRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public final class AuthenticationService {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private final UserAccountRepository repository;
    private final List<UserAccount> accounts;
    private final SecureRandom random = new SecureRandom();

    public AuthenticationService() { this(new JsonUserAccountRepository()); }

    public AuthenticationService(UserAccountRepository repository) {
        this.repository = repository;
        this.accounts = new ArrayList<>(repository.load());
    }

    // &begin[Registration]
    public String register(String username, String email, String password, String confirmation) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (cleanUsername.length() < 3) return "Username must contain at least 3 characters.";
        if (!cleanUsername.matches("[A-Za-z0-9_]+")) return "Username may contain only letters, numbers, and underscores.";
        if (!cleanEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "Enter a valid email address.";
        if (password == null || password.length() < 8) return "Password must contain at least 8 characters.";
        if (!password.equals(confirmation)) return "Passwords do not match.";
        if (accounts.stream().anyMatch(account -> account.username().equalsIgnoreCase(cleanUsername)))
            return "That username is already registered.";
        if (accounts.stream().anyMatch(account -> account.email().equalsIgnoreCase(cleanEmail)))
            return "That email address is already registered.";
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        accounts.add(new UserAccount(cleanUsername, cleanEmail, encode(salt), encode(hash(password.toCharArray(), salt))));
        repository.save(accounts);
        return null;
    }
    // &end[Registration]

    // &begin[Login]
    public UserAccount authenticate(String identifier, String password) {
        if (identifier == null || password == null) return null;
        UserAccount account = accounts.stream().filter(candidate ->
                candidate.username().equalsIgnoreCase(identifier.trim())
                        || candidate.email().equalsIgnoreCase(identifier.trim())).findFirst().orElse(null);
        if (account == null) return null;
        try {
            byte[] salt = Base64.getDecoder().decode(account.salt());
            byte[] expected = Base64.getDecoder().decode(account.passwordHash());
            return MessageDigest.isEqual(expected, hash(password.toCharArray(), salt)) ? account : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
    // &end[Login]

    private static byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        }
    }

    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
}
