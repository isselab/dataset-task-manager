package service;

import model.Task;

import java.util.function.Predicate;

public final class TaskQuery implements Predicate<Task> {
    public static final String ALL_TASKS = "All tasks";
    public static final String OPEN_TASKS = "Open tasks";
    public static final String COMPLETED_TASKS = "Completed tasks";
    public static final String ARCHIVED_TASKS = "Archived tasks";

    private final String searchText;
    private final String status;

    public TaskQuery(String searchText, String status) {
        this.searchText = searchText == null ? "" : searchText.trim().toLowerCase();
        this.status = status;
    }

    @Override
    public boolean test(Task task) {
        boolean matchesSearch = searchText.isEmpty()
                || task.getTitle().toLowerCase().contains(searchText)
                || task.getDescription().toLowerCase().contains(searchText);
        // &begin[ShowArchivedTasks]
        boolean matchesArchive = ARCHIVED_TASKS.equals(status) ? task.isArchived() : !task.isArchived();
        boolean matchesStatus = ARCHIVED_TASKS.equals(status)
                || !OPEN_TASKS.equals(status) && !COMPLETED_TASKS.equals(status)
                || OPEN_TASKS.equals(status) && !task.isCompleted()
                || COMPLETED_TASKS.equals(status) && task.isCompleted();
        // &end[ShowArchivedTasks]
        return matchesSearch && matchesArchive && matchesStatus;
    }
}
