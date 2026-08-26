package repository;

import model.Task;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class JsonTaskRepository implements TaskRepository {
    private final JsonFileStore store;

    public JsonTaskRepository() { this(Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.json")); }
    JsonTaskRepository(Path file) { store = new JsonFileStore(file); }

    @Override public List<Task> load() { return store.read().tasks(); }
    @Override public void save(List<Task> tasks) { JsonFileStore.Snapshot snapshot = store.read(); store.write(snapshot.tags(), tasks); }
}
