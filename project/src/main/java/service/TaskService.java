package service;

import model.Tag;
import model.Task;
import model.TaskPriority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import repository.TaskRepository;


public final class TaskService {
    private final TaskRepository repository;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public TaskService(TaskRepository repository) {
        this.repository = repository;
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
                null, false, TaskPriority.MEDIUM); // &line[TaskPriorityLevels]
        tasks.add(task);
        return task;
    }
    // &end[CreateTasks]

    public void restore(Task task) {
        tasks.add(task);
    }

    public void load() {
        repository.load().forEach(this::restore);
    }

    public void save() {
        repository.save(tasks);
    }

    public void refreshTask(Task task) {
        int index = tasks.indexOf(task);
        if (index >= 0) tasks.set(index, task);
    }

    // &begin[RenameTasks]
    public boolean renameTask(Task task, String title) {
        if (task == null || !tasks.contains(task) || title == null || title.trim().isEmpty()) {
            return false;
        }
        task.setTitle(title);
        refreshTask(task);
        return true;
    }
    // &end[RenameTasks]

    // &begin[DeleteTasks]
    public boolean deleteTask(Task task) {
        if (task == null || !tasks.contains(task)) return false;
        task.setTagIds(java.util.List.of());
        return tasks.remove(task);
    }
    // &end[DeleteTasks]

    // &begin[AssignTaskTags]
    public void assignTag(Task task, Tag tag) {
        if (tag != null) task.addTagId(tag.id());
    }

    public void removeTag(Task task, Tag tag) {
        if (tag != null) task.removeTagId(tag.id());
    }
    // &end[AssignTaskTags]
}
