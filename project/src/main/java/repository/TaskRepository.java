package repository;

import model.Task;
import java.util.List;

public interface TaskRepository {
    List<Task> findAll();

    void saveAll(List<Task> tasks);
}
