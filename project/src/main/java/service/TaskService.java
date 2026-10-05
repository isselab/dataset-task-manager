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
    public static final String STATUS_ALL = "All tasks";
    public static final String STATUS_OPEN = "Open tasks";
    public static final String STATUS_COMPLETED = "Completed tasks";
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

    // &begin[TaskQuery]
    public Predicate<Task> createQuery(String searchText, String status) {
        // &begin[KeywordSearch]
        String keyword = searchText == null ? "" : searchText.trim().toLowerCase();
        Predicate<Task> matchesKeyword = task -> keyword.isEmpty()
                || task.getTitle().toLowerCase().contains(keyword)
                || task.getDescription().toLowerCase().contains(keyword);
        // &end[KeywordSearch]
        // &begin[StatusFilter]
        Predicate<Task> matchesStatus = task -> (!STATUS_OPEN.equals(status) || !task.isCompleted())
                && (!STATUS_COMPLETED.equals(status) || task.isCompleted());
        // &end[StatusFilter]
        return matchesKeyword.and(matchesStatus);
    }
    // &end[TaskQuery]

    // &begin[AssignTaskTags]
    public void assignTag(Task task, Tag tag) {
        if (tag != null) task.addTagId(tag.id());
    }

    public void removeTag(Task task, Tag tag) {
        if (tag != null) task.removeTagId(tag.id());
    }
    // &end[AssignTaskTags]
}
