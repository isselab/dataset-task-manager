package repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class UserDataPaths {
    private static final Path DATA_DIRECTORY = Paths.get(System.getProperty("user.home"), ".task-manager");
    private static final Path LEGACY_FILE = DATA_DIRECTORY.resolve("tasks.json");

    private UserDataPaths() { }

    static Path forUser(String username) {
        String accountKey = hash(username.trim().toLowerCase(java.util.Locale.ROOT));
        Path userFile = DATA_DIRECTORY.resolve("user-" + accountKey + ".json");
        migrateLegacyFile(userFile);
        return userFile;
    }

    private static void migrateLegacyFile(Path userFile) {
        if (Files.exists(userFile) || !Files.exists(LEGACY_FILE)) return;
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Files.move(LEGACY_FILE, userFile);
        } catch (IOException ignored) { }
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}