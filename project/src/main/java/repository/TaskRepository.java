package repository;

import model.Task;
import java.util.List;

/** Persistence contract used by the task service. */
public interface TaskRepository {
    List<Task> load();

    void save(List<Task> tasks);
}
