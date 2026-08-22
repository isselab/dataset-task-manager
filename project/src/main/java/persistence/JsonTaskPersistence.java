package persistence;

import model.Label;
import model.Task;
import model.TaskPriority;
import service.LabelService;
import service.TaskService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class JsonTaskPersistence {
    private final Path file;

    public JsonTaskPersistence() {
        this(Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.json"));
    }

    JsonTaskPersistence(Path file) {
        this.file = file;
    }

    public void save(TaskService taskService, LabelService labelService) {
        StringBuilder json = new StringBuilder("{\"labels\":[");
        for (int i = 0; i < labelService.getLabels().size(); i++) {
            if (i > 0) json.append(',');
            Label label = labelService.getLabels().get(i);
            json.append("{\"id\":\"").append(escape(label.id())).append("\",\"name\":\"")
                    .append(escape(label.name())).append("\",\"color\":\"")
                    .append(escape(label.color())).append("\"}");
        }
        json.append("],\"tasks\":[");
        for (int i = 0; i < taskService.getTasks().size(); i++) {
            if (i > 0) json.append(',');
            Task task = taskService.getTasks().get(i);
            json.append("{\"id\":\"").append(escape(task.getId())).append("\",\"title\":\"")
                    .append(escape(task.getTitle())).append("\",\"description\":\"")
                    .append(escape(task.getDescription())).append("\",\"priority\":\"")
                    .append(task.getPriority().name()).append("\",\"labelIds\":[");
            for (int labelIndex = 0; labelIndex < task.getLabelIds().size(); labelIndex++) {
                if (labelIndex > 0) json.append(',');
                json.append('\"').append(escape(task.getLabelIds().get(labelIndex))).append('\"');
            }
            json.append(']');
            json.append(",\"completed\":").append(task.isCompleted());
            json.append('}');
        }
        json.append("]}");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The current session remains usable when the local file cannot be written.
        }
    }

    public void load(TaskService taskService, LabelService labelService) {
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonReader reader = new JsonReader(json);
            List<Label> labels = reader.readLabels();
            List<Task> tasks = reader.readTasks();
            labels.forEach(labelService::restore);
            tasks.forEach(taskService::restore);
        } catch (IOException | IllegalArgumentException ignored) {
            // A malformed local file should not prevent the application from starting.
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static final class JsonReader {
        private final String json;
        private int position;

        private JsonReader(String json) { this.json = json; }


        private List<Label> readLabels() {
            position = json.indexOf("\"labels\"");
            if (position < 0) throw new IllegalArgumentException();
            position = json.indexOf('[', position) + 1;
            List<Label> result = new ArrayList<>();
            while (!at(']')) {
                expect('{');
                String id = readStringField("id");
                expect(',');
                String name = readStringField("name");
                String color = "#4f46e5";
                if (at(',')) {
                    expect(',');
                    color = readStringField("color");
                }
                expect('}');
                result.add(new Label(id, name, color));
                consumeComma();
            }
            return result;
        }

        private List<Task> readTasks() {
            position = json.indexOf("\"tasks\"");
            position = json.indexOf('[', position) + 1;
            List<Task> result = new ArrayList<>();
            while (!at(']')) {
                expect('{');
                String id = readStringField("id");
                expect(',');
                String title = readStringField("title");
                expect(',');
                String description = "";
                if (atString("\"description\"")) {
                    description = readStringField("description");
                    expect(',');
                }
                TaskPriority priority = TaskPriority.MEDIUM;
                if (atString("\"priority\"")) {
                    priority = TaskPriority.valueOf(readStringField("priority"));
                    expect(',');
                }
                List<String> labelIds;
                if (atString("\"labelIds\"")) {
                    expectField("labelIds");
                    labelIds = readStringArray();
                } else {
                    expectField("labelId");
                    String legacyLabelId = atString("null") ? readNull() : readString();
                    labelIds = legacyLabelId == null ? List.of() : List.of(legacyLabelId);
                }
                boolean completed = false;
                if (at(',')) {
                    expect(',');
                    expectField("completed");
                    completed = readBoolean();
                }
                expect('}');
                result.add(new Task(id, title, description, labelIds, completed, priority)); // &line[TaskPriority]
                consumeComma();
            }
            return result;
        }

        private String readStringField(String field) {
            expectField(field);
            return readString();
        }

        private List<String> readStringArray() {
            expect('[');
            List<String> values = new ArrayList<>();
            while (!at(']')) {
                values.add(readString());
                consumeComma();
            }
            expect(']');
            return values;
        }

        private void expectField(String field) {
            skipWhitespace();
            expect('"');
            int start = position;
            while (json.charAt(position) != '"') position++;
            if (!json.substring(start, position).equals(field)) throw new IllegalArgumentException();
            position++;
            skipWhitespace();
            expect(':');
        }

        private String readString() {
            skipWhitespace();
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < json.length()) {
                char character = json.charAt(position++);
                if (character == '"') return value.toString();
                if (character == '\\') {
                    char escaped = json.charAt(position++);
                    value.append(escaped == 'n' ? '\n' : escaped == 'r' ? '\r' : escaped);
                } else value.append(character);
            }
            throw new IllegalArgumentException();
        }

        private String readNull() { position += 4; return null; }
        private boolean readBoolean() {
            skipWhitespace();
            if (json.startsWith("true", position)) {
                position += 4;
                return true;
            }
            if (json.startsWith("false", position)) {
                position += 5;
                return false;
            }
            throw new IllegalArgumentException();
        }
        private boolean atString(String value) { skipWhitespace(); return json.startsWith(value, position); }
        private boolean at(char character) { skipWhitespace(); return json.charAt(position) == character; }
        private void consumeComma() { skipWhitespace(); if (position < json.length() && json.charAt(position) == ',') position++; }
        private void expect(char character) { skipWhitespace(); if (position >= json.length() || json.charAt(position++) != character) throw new IllegalArgumentException(); }
        private void skipWhitespace() { while (position < json.length() && Character.isWhitespace(json.charAt(position))) position++; }
    }
}
