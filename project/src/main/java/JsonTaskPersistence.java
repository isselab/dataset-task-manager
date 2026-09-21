import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonTaskPersistence {

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".taskmanager");
    private static final Path DATA_FILE = DATA_DIR.resolve("data.json");

    public static class Snapshot {
        public final List<Task> tasks;
        public final List<Label> labels;

        public Snapshot(List<Task> tasks, List<Label> labels) {
            this.tasks = tasks;
            this.labels = labels;
        }
    }

    public Snapshot load() {
        if (!Files.exists(DATA_FILE)) {
            return new Snapshot(new ArrayList<>(), new ArrayList<>());
        }
        try {
            String json = Files.readString(DATA_FILE, StandardCharsets.UTF_8);
            return parse(json);
        } catch (IOException | RuntimeException e) {
            return new Snapshot(new ArrayList<>(), new ArrayList<>());
        }
    }

    public void save(List<Task> tasks, List<Label> labels) {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(DATA_FILE, toJson(tasks, labels), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String toJson(List<Task> tasks, List<Label> labels) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"labels\": [\n");
        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            json.append("    {\"id\": ").append(quote(label.getId()))
                    .append(", \"name\": ").append(quote(label.getName())).append("}")
                    .append(i < labels.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            json.append("    {\"id\": ").append(quote(task.getId()))
                    .append(", \"title\": ").append(quote(task.getTitle()))
                    .append(", \"labelId\": ").append(task.getLabelId() == null ? "null" : quote(task.getLabelId()))
                    .append("}")
                    .append(i < tasks.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': quoted.append("\\\""); break;
                case '\\': quoted.append("\\\\"); break;
                case '\n': quoted.append("\\n"); break;
                default: quoted.append(c);
            }
        }
        return quoted.append('"').toString();
    }

    private Snapshot parse(String json) {
        JsonReader reader = new JsonReader(json);
        reader.expect('{');
        List<Label> labels = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        while (reader.peek() != '}') {
            String key = reader.readString();
            reader.expect(':');
            if (key.equals("labels")) {
                labels = readLabelArray(reader);
            } else if (key.equals("tasks")) {
                tasks = readTaskArray(reader);
            }
            if (reader.peek() == ',') {
                reader.next();
            }
        }
        reader.expect('}');
        return new Snapshot(tasks, labels);
    }

    private List<Label> readLabelArray(JsonReader reader) {
        List<Label> result = new ArrayList<>();
        reader.expect('[');
        while (reader.peek() != ']') {
            reader.expect('{');
            String id = null;
            String name = null;
            while (reader.peek() != '}') {
                String key = reader.readString();
                reader.expect(':');
                String value = reader.readString();
                if (key.equals("id")) id = value;
                if (key.equals("name")) name = value;
                if (reader.peek() == ',') reader.next();
            }
            reader.expect('}');
            result.add(new Label(id, name));
            if (reader.peek() == ',') reader.next();
        }
        reader.expect(']');
        return result;
    }

    private List<Task> readTaskArray(JsonReader reader) {
        List<Task> result = new ArrayList<>();
        reader.expect('[');
        while (reader.peek() != ']') {
            reader.expect('{');
            String id = null;
            String title = null;
            String labelId = null;
            while (reader.peek() != '}') {
                String key = reader.readString();
                reader.expect(':');
                String value = reader.peek() == 'n' ? reader.readNull() : reader.readString();
                if (key.equals("id")) id = value;
                if (key.equals("title")) title = value;
                if (key.equals("labelId")) labelId = value;
                if (reader.peek() == ',') reader.next();
            }
            reader.expect('}');
            Task task = new Task(id, title);
            task.setLabelId(labelId);
            result.add(task);
            if (reader.peek() == ',') reader.next();
        }
        reader.expect(']');
        return result;
    }

    private static class JsonReader {
        private final String json;
        private int pos = 0;

        JsonReader(String json) {
            this.json = json;
        }

        char peek() {
            skipWhitespace();
            return json.charAt(pos);
        }

        void next() {
            skipWhitespace();
            pos++;
        }

        void expect(char c) {
            skipWhitespace();
            if (json.charAt(pos) != c) {
                throw new IllegalStateException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        String readNull() {
            skipWhitespace();
            if (!json.startsWith("null", pos)) {
                throw new IllegalStateException("Expected 'null' at position " + pos);
            }
            pos += 4;
            return null;
        }

        String readString() {
            skipWhitespace();
            if (json.charAt(pos) != '"') {
                throw new IllegalStateException("Expected string at position " + pos);
            }
            pos++;
            StringBuilder value = new StringBuilder();
            while (json.charAt(pos) != '"') {
                char c = json.charAt(pos);
                if (c == '\\') {
                    pos++;
                    char escaped = json.charAt(pos);
                    switch (escaped) {
                        case 'n': value.append('\n'); break;
                        case '"': value.append('"'); break;
                        case '\\': value.append('\\'); break;
                        default: value.append(escaped);
                    }
                } else {
                    value.append(c);
                }
                pos++;
            }
            pos++;
            return value.toString();
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
