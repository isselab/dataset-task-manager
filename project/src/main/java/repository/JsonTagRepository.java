package repository;

import model.Tag;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class JsonTagRepository implements TagRepository {
    private final JsonFileStore store;

    public JsonTagRepository() { this(Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.json")); }
    JsonTagRepository(Path file) { store = new JsonFileStore(file); }

    @Override public List<Tag> load() { return store.read().tags(); }
    @Override public void save(List<Tag> tags) { JsonFileStore.Snapshot snapshot = store.read(); store.write(tags, snapshot.tasks()); }
}
