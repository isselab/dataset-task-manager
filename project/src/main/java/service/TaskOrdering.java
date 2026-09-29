package service;

import model.Task;
import model.TaskPriority;

import java.util.Comparator;
import java.util.Map;

public final class TaskOrdering {
    private static final Map<TaskPriority, Integer> DISPLAY_RANK = Map.of(
            TaskPriority.HIGH, 0,
            TaskPriority.MEDIUM, 1,
            TaskPriority.LOW, 2
    );

    private TaskOrdering() {
    }

    public static Comparator<Task> comparator() {
        return Comparator.comparing(Task::isCompleted)
                .thenComparing(task -> DISPLAY_RANK.get(task.getPriority()));
    }
}
