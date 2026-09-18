package repository;

import model.UserAccount;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class JsonUserAccountRepository implements UserAccountRepository {
    private final Path file;

    public JsonUserAccountRepository() {
        this(Paths.get(System.getProperty("user.home"), ".task-manager", "accounts.json"));
    }

    public JsonUserAccountRepository(Path file) { this.file = file; }

    @Override
    public List<UserAccount> load() {
        if (!Files.exists(file)) return List.of();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (!json.startsWith("[")) return List.of();
            List<UserAccount> accounts = new ArrayList<>();
            int position = 1;
            while (position < json.length() - 1) {
                int start = json.indexOf('{', position);
                if (start < 0) break;
                int end = json.indexOf('}', start);
                if (end < 0) throw new IllegalArgumentException();
                String object = json.substring(start, end + 1);
                String username = field(object, "username");
                String email = field(object, "email");
                String salt = field(object, "salt");
                String hash = field(object, "passwordHash");
                accounts.add(new UserAccount(username, email, salt, hash));
                position = end + 1;
            }
            return accounts;
        } catch (IOException | IllegalArgumentException ignored) {
            return List.of();
        }
    }

    @Override
    public void save(List<UserAccount> accounts) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < accounts.size(); i++) {
            if (i > 0) json.append(',');
            UserAccount account = accounts.get(i);
            json.append("{\"username\":\"").append(escape(account.username()))
                    .append("\",\"email\":\"").append(escape(account.email()))
                    .append("\",\"salt\":\"").append(escape(account.salt()))
                    .append("\",\"passwordHash\":\"").append(escape(account.passwordHash())).append("\"}");
        }
        json.append(']');
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    private static String field(String object, String name) {
        String marker = "\"" + name + "\":\"";
        int start = object.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException();
        start += marker.length();
        int end = start;
        while (end < object.length()) {
            if (object.charAt(end) == '"' && object.charAt(end - 1) != '\\') break;
            end++;
        }
        if (end >= object.length()) throw new IllegalArgumentException();
        return object.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
