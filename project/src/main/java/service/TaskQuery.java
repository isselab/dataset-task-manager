package service;

import model.Task;

import java.util.function.Predicate;

public final class TaskQuery implements Predicate<Task> {
    public static final String ALL_TASKS = "All tasks";
    public static final String OPEN_TASKS = "Open tasks";
    public static final String COMPLETED_TASKS = "Completed tasks";

    private final String searchText;
    private final String status;

    private final boolean showArchived; // &line[ArchiveTasks]

    public TaskQuery(String searchText, String status) {
        this(searchText, status, false);
    }

    public TaskQuery(String searchText, String status, boolean showArchived) {
        this.searchText = searchText == null ? "" : searchText.trim().toLowerCase();
        this.status = status;
        this.showArchived = showArchived; // &line[ArchiveTasks]
    }

    @Override
    public boolean test(Task task) {
        boolean matchesSearch = searchText.isEmpty()
                || task.getTitle().toLowerCase().contains(searchText)
                || task.getDescription().toLowerCase().contains(searchText);
        boolean matchesStatus = !OPEN_TASKS.equals(status) && !COMPLETED_TASKS.equals(status)
                || OPEN_TASKS.equals(status) && !task.isCompleted()
                || COMPLETED_TASKS.equals(status) && task.isCompleted();
        boolean matchesArchive = showArchived || !task.isArchived(); // &line[ArchiveTasks]
        return matchesSearch && matchesStatus && matchesArchive; // &line[ArchiveTasks]
    }
}
