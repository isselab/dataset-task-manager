package taskmanager.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import taskmanager.model.Label;
import taskmanager.model.Task;

import java.util.List;

// &begin[TaskManagement]
public class TaskService {

    private final ObservableList<Task> tasks;
    private int nextId;

    public TaskService(List<Task> initialTasks) {
        this.tasks = FXCollections.observableArrayList(initialTasks);
        this.nextId = initialTasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }
    // &end[TaskManagement]

    // &begin[CreateTask]
    public Task createTask(String title, Label label) {
        Task task = new Task(nextId++, title);
        task.setLabel(label);
        tasks.add(task);
        return task;
    }
    // &end[CreateTask]

    // &begin[AssignLabel]
    public void assignLabel(Task task, Label label) {
        task.setLabel(label);
    }
    // &end[AssignLabel]
}
