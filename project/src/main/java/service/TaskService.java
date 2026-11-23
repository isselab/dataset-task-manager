package service;

import model.Subtask; // &line[Subtasks]
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

    // &begin[AddSubtasks]
    public Subtask addSubtask(Task task, String title) {
        if (task == null || !tasks.contains(task) || title == null || title.trim().isEmpty()) return null;
        Subtask subtask = new Subtask(UUID.randomUUID().toString(), title.trim(), false);
        task.addSubtask(subtask);
        refreshTask(task);
        return subtask;
    }
    // &end[AddSubtasks]

    // &begin[RenameSubtasks]
    public boolean renameSubtask(Task task, Subtask subtask, String title) {
        if (task == null || subtask == null || !task.getSubtasks().contains(subtask)
                || title == null || title.trim().isEmpty()) {
            return false;
        }
        subtask.setTitle(title);
        refreshTask(task);
        return true;
    }
    // &end[RenameSubtasks]

    // &begin[CompleteSubtasks]
    public boolean setSubtaskCompleted(Task task, Subtask subtask, boolean completed) {
        if (task == null || subtask == null || !task.getSubtasks().contains(subtask)) return false;
        subtask.setCompleted(completed);
        refreshTask(task);
        return true;
    }
    // &end[CompleteSubtasks]

    // &begin[DeleteSubtasks]
    public boolean deleteSubtask(Task task, Subtask subtask) {
        if (task == null || subtask == null || !task.removeSubtask(subtask)) return false;
        refreshTask(task);
        return true;
    }
    // &end[DeleteSubtasks]

    // &begin[AssignTaskTags]
    public void assignTag(Task task, Tag tag) {
        if (tag != null) task.addTagId(tag.id());
    }

    public void removeTag(Task task, Tag tag) {
        if (tag != null) task.removeTagId(tag.id());
    }
    // &end[AssignTaskTags]
}
