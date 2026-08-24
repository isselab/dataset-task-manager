package repository;

import model.Label;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class JsonLabelRepository implements LabelRepository {
    private final JsonFileStore store;

    public JsonLabelRepository() { this(Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.json")); }
    JsonLabelRepository(Path file) { store = new JsonFileStore(file); }

    @Override public List<Label> load() { return store.read().labels(); }
    @Override public void save(List<Label> labels) { JsonFileStore.Snapshot snapshot = store.read(); store.write(labels, snapshot.tasks()); }
}
