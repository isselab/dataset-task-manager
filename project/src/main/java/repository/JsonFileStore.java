package repository;

import model.Tag;
import model.Task;
import model.TaskPriority;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class JsonFileStore {
    private final Path file;

    JsonFileStore(Path file) { this.file = file; }

    Snapshot read() {
        if (!Files.exists(file)) return new Snapshot(List.of(), List.of());
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonReader reader = new JsonReader(json);
            return new Snapshot(reader.readTags(), reader.readTasks());
        } catch (IOException | IllegalArgumentException ignored) {
            return new Snapshot(List.of(), List.of());
        }
    }

    // &begin[PersistTags]
    void write(List<Tag> tags, List<Task> tasks) {
        StringBuilder json = new StringBuilder("{\"labels\":[");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) json.append(',');
            Tag tag = tags.get(i);
            json.append("{\"id\":\"").append(escape(tag.id())).append("\",\"name\":\"")
                    .append(escape(tag.name())).append("\",\"color\":\"")
                    .append(escape(tag.color())).append("\"}");
        }
        json.append("],\"tasks\":[");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) json.append(',');
            Task task = tasks.get(i);
            json.append("{\"id\":\"").append(escape(task.getId())).append("\",\"title\":\"")
                    .append(escape(task.getTitle())).append("\",\"description\":\"")
                    .append(escape(task.getDescription())).append("\",\"priority\":\"")
                    .append(task.getPriority().name()).append("\",\"labelIds\":[");
            for (int j = 0; j < task.getTagIds().size(); j++) {
                if (j > 0) json.append(',');
                json.append('"').append(escape(task.getTagIds().get(j))).append('"');
            }
            json.append("],\"completed\":").append(task.isCompleted()).append('}');
        }
        json.append("]}");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }
    // &end[PersistTags]

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    record Snapshot(List<Tag> tags, List<Task> tasks) { }

    private static final class JsonReader {
        private final String json;
        private int position;
        JsonReader(String json) { this.json = json; }

        // &begin[PersistTags]
        List<Tag> readTags() {
            position = json.indexOf("\"labels\"");
            if (position < 0) throw new IllegalArgumentException();
            position = json.indexOf('[', position) + 1;
            List<Tag> result = new ArrayList<>();
            while (!at(']')) {
                expect('{'); String id = readStringField("id"); expect(',');
                String name = readStringField("name"); String color = "#4f46e5";
                if (at(',')) { expect(','); color = readStringField("color"); }
                expect('}'); result.add(new Tag(id, name, color)); consumeComma();
            }
            return result;
        }
        // &end[PersistTags]

        List<Task> readTasks() {
            position = json.indexOf("\"tasks\"");
            position = json.indexOf('[', position) + 1;
            List<Task> result = new ArrayList<>();
            while (!at(']')) {
                expect('{'); String id = readStringField("id"); expect(',');
                String title = readStringField("title"); expect(','); String description = "";
                if (atString("\"description\"")) { description = readStringField("description"); expect(','); }
                TaskPriority priority = TaskPriority.MEDIUM;
                if (atString("\"priority\"")) { priority = TaskPriority.valueOf(readStringField("priority")); expect(','); }
                List<String> tagIds;
                if (atString("\"labelIds\"")) { expectField("labelIds"); tagIds = readStringArray(); }
                else { expectField("labelId"); String legacy = atString("null") ? readNull() : readString(); tagIds = legacy == null ? List.of() : List.of(legacy); }
                boolean completed = false;
                if (at(',')) { expect(','); expectField("completed"); completed = readBoolean(); }
                expect('}'); result.add(new Task(id, title, description, tagIds, completed, priority)); consumeComma();
            }
            return result;
        }

        private String readStringField(String field) { expectField(field); return readString(); }
        private List<String> readStringArray() { expect('['); List<String> values = new ArrayList<>(); while (!at(']')) { values.add(readString()); consumeComma(); } expect(']'); return values; }
        private void expectField(String field) { skipWhitespace(); expect('"'); int start = position; while (json.charAt(position) != '"') position++; if (!json.substring(start, position).equals(field)) throw new IllegalArgumentException(); position++; skipWhitespace(); expect(':'); }
        private String readString() { skipWhitespace(); expect('"'); StringBuilder value = new StringBuilder(); while (position < json.length()) { char c = json.charAt(position++); if (c == '"') return value.toString(); if (c == '\\') { char e = json.charAt(position++); value.append(e == 'n' ? '\n' : e == 'r' ? '\r' : e); } else value.append(c); } throw new IllegalArgumentException(); }
        private String readNull() { position += 4; return null; }
        private boolean readBoolean() { skipWhitespace(); if (json.startsWith("true", position)) { position += 4; return true; } if (json.startsWith("false", position)) { position += 5; return false; } throw new IllegalArgumentException(); }
        private boolean atString(String value) { skipWhitespace(); return json.startsWith(value, position); }
        private boolean at(char c) { skipWhitespace(); return position < json.length() && json.charAt(position) == c; }
        private void consumeComma() { skipWhitespace(); if (position < json.length() && json.charAt(position) == ',') position++; }
        private void expect(char c) { skipWhitespace(); if (position >= json.length() || json.charAt(position++) != c) throw new IllegalArgumentException(); }
        private void skipWhitespace() { while (position < json.length() && Character.isWhitespace(json.charAt(position))) position++; }
    }
}
