package taskmanager.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import taskmanager.model.Label;
import taskmanager.model.Task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTaskPersistence {

    private static final Path DATA_FILE = Path.of(System.getProperty("user.home"), ".taskmanager", "data.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTaskPersistence() {
    }

    public record LoadResult(List<Label> labels, List<Task> tasks) {
    }

    private record LabelDto(int id, String name) {
    }

    private record TaskDto(int id, String title, Integer labelId) {
    }

    private record FileContent(List<LabelDto> labels, List<TaskDto> tasks) {
    }

    public static LoadResult load() {
        if (!Files.exists(DATA_FILE)) {
            return new LoadResult(new ArrayList<>(), new ArrayList<>());
        }
        try {
            FileContent content = MAPPER.readValue(DATA_FILE.toFile(), FileContent.class);

            List<Label> labels = new ArrayList<>();
            Map<Integer, Label> labelById = new HashMap<>();
            for (LabelDto dto : content.labels()) {
                Label label = new Label(dto.id(), dto.name());
                labels.add(label);
                labelById.put(label.getId(), label);
            }

            List<Task> tasks = new ArrayList<>();
            for (TaskDto dto : content.tasks()) {
                Task task = new Task(dto.id(), dto.title());
                if (dto.labelId() != null) {
                    task.setLabel(labelById.get(dto.labelId()));
                }
                tasks.add(task);
            }

            return new LoadResult(labels, tasks);
        } catch (IOException | RuntimeException e) {
            return new LoadResult(new ArrayList<>(), new ArrayList<>());
        }
    }

    public static void save(List<Label> labels, List<Task> tasks) {
        try {
            Files.createDirectories(DATA_FILE.getParent());

            List<LabelDto> labelDtos = labels.stream()
                    .map(label -> new LabelDto(label.getId(), label.getName()))
                    .toList();
            List<TaskDto> taskDtos = tasks.stream()
                    .map(task -> new TaskDto(task.getId(), task.getTitle(),
                            task.getLabel() == null ? null : task.getLabel().getId()))
                    .toList();

            MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(DATA_FILE.toFile(), new FileContent(labelDtos, taskDtos));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
