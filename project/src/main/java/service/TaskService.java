package service;

import model.Label;
import model.Task;
import model.TaskStatusFilter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.UUID;


public final class TaskService {
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final FilteredList<Task> filteredTasks = new FilteredList<>(tasks);
    private String searchKeyword = "";
    private TaskStatusFilter statusFilter = TaskStatusFilter.ALL;

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    // &begin[CreateTasks]
    public Task createTask(String title) {
        Task task = new Task(UUID.randomUUID().toString(), title.trim());
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

    // &begin[SearchTasksByKeyword,FilterTasksByStatus]
    public FilteredList<Task> getFilteredTasks() {
        return filteredTasks;
    }

    private void updateFilter() {
        filteredTasks.setPredicate(task -> matchesKeyword(task, searchKeyword) && statusFilter.accepts(task));
    }
    // &end[SearchTasksByKeyword,FilterTasksByStatus]

    // &begin[SearchTasksByKeyword]
    public void setSearchKeyword(String keyword) {
        searchKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        updateFilter();
    }

    static boolean matchesKeyword(Task task, String normalizedKeyword) {
        if (normalizedKeyword.isEmpty()) {
            return true;
        }
        return task.getTitle() != null && task.getTitle().toLowerCase().contains(normalizedKeyword);
    }
    // &end[SearchTasksByKeyword]

    // &begin[FilterTasksByStatus]
    public void setStatusFilter(TaskStatusFilter filter) {
        statusFilter = filter == null ? TaskStatusFilter.ALL : filter;
        updateFilter();
    }
    // &end[FilterTasksByStatus]

    // &begin[CompleteTasks]
    public void setTaskCompleted(Task task, boolean completed) {
        task.setCompleted(completed);
        updateFilter(); // &line[FilterTasksByStatus]
    }
    // &end[CompleteTasks]
}
