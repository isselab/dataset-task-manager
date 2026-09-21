import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.UUID;

public class TaskService {

    // &begin[Tasks]
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public TaskService(List<Task> initialTasks) {
        tasks.addAll(initialTasks);
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }
    // &end[Tasks]

    // &begin[AddTask]
    public Task addTask(String title) {
        Task task = new Task(UUID.randomUUID().toString(), title);
        tasks.add(task);
        return task;
    }
    // &end[AddTask]

    // &begin[AssignLabel]
    public void setTaskLabel(Task task, Label label) {
        task.setLabelId(label == null ? null : label.getId());
    }
    // &end[AssignLabel]
}
