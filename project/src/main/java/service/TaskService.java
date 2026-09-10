package service;

import model.Tag;
import model.Task;
import model.TaskPriority;
import model.Subtask;
import model.Recurrence;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import java.util.List;
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

    // &begin[RecurringTasks]
    public boolean setTaskCompleted(Task task, boolean completed) {
        if (task == null || !tasks.contains(task)) return false;
        boolean wasCompleted = task.isCompleted();
        task.setCompleted(completed);
        refreshTask(task);
        if (completed && !wasCompleted && task.getRecurrence() != Recurrence.NONE) {
            Task nextOccurrence = new Task(UUID.randomUUID().toString(), task.getTitle(), task.getDescription(),
                    task.getTagIds(), false, task.getPriority(),
                    task.getRecurrence().nextDueDate(task.getDueDate()), task.getRecurrence(), List.of());
            tasks.add(nextOccurrence);
        }
        return true;
    }
    // &end[RecurringTasks]

    // &begin[Subtasks]
    // &begin[AddSubtasks]
    public Subtask addSubtask(Task task, String title) {
        if (task == null || !tasks.contains(task) || title == null || title.trim().isEmpty()) return null;
        Subtask subtask = new Subtask(UUID.randomUUID().toString(), title);
        task.addSubtask(subtask);
        refreshTask(task);
        return subtask;
    }
    // &end[AddSubtasks]

    // &begin[RenameSubtasks]
    public boolean renameSubtask(Task task, Subtask subtask, String title) {
        if (task == null || subtask == null || !tasks.contains(task) || !task.getSubtasks().contains(subtask)
                || title == null || title.trim().isEmpty()) return false;
        subtask.setTitle(title);
        refreshTask(task);
        return true;
    }
    // &end[RenameSubtasks]

    // &begin[CompleteSubtasks]
    public boolean setSubtaskCompleted(Task task, Subtask subtask, boolean completed) {
        if (task == null || subtask == null || !tasks.contains(task) || !task.getSubtasks().contains(subtask))
            return false;
        subtask.setCompleted(completed);
        refreshTask(task);
        return true;
    }

    public boolean completeSubtask(Task task, Subtask subtask) {
        return setSubtaskCompleted(task, subtask, true);
    }
    // &end[CompleteSubtasks]

    // &begin[ReopenSubtasks]
    public boolean reopenSubtask(Task task, Subtask subtask) {
        return setSubtaskCompleted(task, subtask, false);
    }
    // &end[ReopenSubtasks]

    // &begin[DeleteSubtasks]
    public boolean deleteSubtask(Task task, Subtask subtask) {
        if (task == null || subtask == null || !tasks.contains(task)) return false;
        boolean removed = task.removeSubtask(subtask);
        if (removed) refreshTask(task);
        return removed;
    }
    // &end[DeleteSubtasks]
    // &end[Subtasks]

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
