package service;

import model.Label;
import model.Task;
import model.TaskPriority;
import repository.TaskRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;


public final class TaskService {
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final TaskRepository repository; // &line[PersistLabels]

    public TaskService(TaskRepository repository) {
        this.repository = repository; // &line[PersistLabels]
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    // &begin[CreateTasks]
    public Task createTask(String title) {
        return createTask(title, "");
    }

    public Task createTask(String title, String description) {
        Task task = new Task(UUID.randomUUID().toString(), title.trim(), description == null ? "" : description.trim(),
                null, false, TaskPriority.MEDIUM); // &line[TaskPriority]
        tasks.add(task);
        return task;
    }
    // &end[CreateTasks]

    // &begin[PersistLabels]
    public void load() {
        tasks.addAll(repository.findAll());
    }

    public void save() {
        repository.saveAll(tasks);
    }
    // &end[PersistLabels]

    public void refreshTask(Task task) {
        int index = tasks.indexOf(task);
        if (index >= 0) tasks.set(index, task);
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
