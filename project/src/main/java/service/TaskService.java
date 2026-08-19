package service;

import model.Label;
import model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
        Task task = new Task(UUID.randomUUID().toString(), title.trim(), description == null ? "" : description.trim(), null);
        tasks.add(task);
        return task;
    }
    // &end[CreateTasks]

    public void restore(Task task) {
        tasks.add(task);
    }

    // &begin[AssignTaskLabels]
    public void assignLabel(Task task, Label label) {
        task.setLabelId(label == null ? null : label.id());
    }
    // &end[AssignTaskLabels]

    public void removeLabel(Task task) {
        task.setLabelId(null);
    }
}
