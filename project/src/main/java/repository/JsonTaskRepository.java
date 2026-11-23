package repository;

import model.Task;
import java.util.List;

public final class JsonTaskRepository implements TaskRepository {
    private final JsonTaskStore store;

    public JsonTaskRepository(JsonTaskStore store) {
        this.store = store;
    }

    @Override
    public List<Task> findAll() {
        return store.loadTasks();
    }

    @Override
    public void saveAll(List<Task> tasks) {
        store.saveTasks(tasks);
    }
}
