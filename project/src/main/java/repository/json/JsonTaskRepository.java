package repository.json;

import model.Task;
import repository.TaskRepository;
import java.util.List;

public final class JsonTaskRepository implements TaskRepository {
    private final JsonStorage storage;

    public JsonTaskRepository(JsonStorage storage) {
        this.storage = storage;
    }

    @Override
    public List<Task> findAll() {
        return storage.loadTasks();
    }

    @Override
    public void saveAll(List<Task> tasks) {
        storage.writeTasks(tasks);
    }
}
