package service;

import model.Task;
import model.TaskPriority;

import java.util.Comparator;

public final class TaskOrdering implements Comparator<Task> {
    private static final Comparator<Task> ORDER = Comparator.comparing(Task::isCompleted)
            .thenComparingInt(task -> rank(task.getPriority()));

    private static int rank(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    @Override
    public int compare(Task first, Task second) {
        return ORDER.compare(first, second);
    }
}
