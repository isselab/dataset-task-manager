package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Label;
import model.Task;

import java.util.UUID;


public final class TaskService {
    // &begin[StatusFilter]
    public enum StatusFilter {
        ALL("All tasks"),
        OPEN("Open tasks"),
        COMPLETED("Completed tasks");

        private final String label;

        StatusFilter(String label) {
            this.label = label;
        }

        public boolean matches(Task task) {
            return switch (this) {
                case ALL -> true;
                case OPEN -> !task.isCompleted();
                case COMPLETED -> task.isCompleted();
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }
    // &end[StatusFilter]

    // The extractor re-evaluates filters when a task's completion changes.
    private final ObservableList<Task> tasks = FXCollections.observableArrayList(task -> new javafx.beans.Observable[] {task.completedProperty()}); // &line[StatusFilter]

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

    // &begin[KeywordSearch]
    public static boolean matchesKeyword(Task task, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return task.getTitle().toLowerCase().contains(keyword.trim().toLowerCase());
    }
    // &end[KeywordSearch]

    public void removeLabel(Task task) {
        task.setLabelId(null);
    }
}
