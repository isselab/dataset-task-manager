package service;

import model.Tag;
import model.Task;
import model.TaskPriority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import java.util.function.Predicate;
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
                null, false, TaskPriority.MEDIUM); // &line[TaskPriority]
        tasks.add(task);
        return task;
    }
    // &end[CreateTasks]

    // &begin[TaskQuery]
    public static final String ALL_TASKS = "All tasks"; // &line[StatusFilter]
    public static final String OPEN_TASKS = "Open tasks"; // &line[StatusFilter]
    public static final String COMPLETED_TASKS = "Completed tasks"; // &line[StatusFilter]

    public static Predicate<Task> taskQuery(String searchText, String status) {
        String keyword = searchText == null ? "" : searchText.trim().toLowerCase();
        return task -> (keyword.isEmpty()
                || task.getTitle().toLowerCase().contains(keyword)
                || task.getDescription().toLowerCase().contains(keyword))
                && (!OPEN_TASKS.equals(status) || !task.isCompleted())
                && (!COMPLETED_TASKS.equals(status) || task.isCompleted());
    }
    // &end[TaskQuery]

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

    // &begin[AssignTaskTags]
    public void assignTag(Task task, Tag tag) {
        if (tag != null) task.addTagId(tag.id());
    }

    public void removeTag(Task task, Tag tag) {
        if (tag != null) task.removeTagId(tag.id());
    }
    // &end[AssignTaskTags]
}
