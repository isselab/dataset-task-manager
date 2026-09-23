package service;

import model.Label;
import model.Priority; // &line[TaskPriority]
import model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.UUID;


public final class TaskService {
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    // &begin[CreateTasks]
    public Task createTask(String title) {
        return createTask(title, "");
    }

    public Task createTask(String title, String description) {
        return createTask(title, description, Priority.MEDIUM); // &line[TaskPriority]
    }
    // &end[CreateTasks]

    // &begin[TaskPriority]
    public Task createTask(String title, String description, Priority priority) {
        Task task = new Task(UUID.randomUUID().toString(), title.trim(), description == null ? "" : description.trim(),
                List.of(), false, priority);
        tasks.add(task);
        return task;
    }
    // &end[TaskPriority]

    public void restore(Task task) {
        tasks.add(task);
    }

    // &begin[AssignTaskLabels]
    public void assignLabel(Task task, Label label) {
        if (label != null) task.addLabelId(label.id());
    }

    public void removeLabel(Task task, Label label) {
        if (label != null) task.removeLabelId(label.id());
    }
    // &end[AssignTaskLabels]
}
