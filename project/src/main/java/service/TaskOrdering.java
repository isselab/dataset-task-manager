package service;

import model.Task;
import model.TaskPriority;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

public final class TaskOrdering {
    private static final Map<TaskPriority, Integer> PRIORITY_RANK = new EnumMap<>(TaskPriority.class);

    static {
        PRIORITY_RANK.put(TaskPriority.HIGH, 0);
        PRIORITY_RANK.put(TaskPriority.MEDIUM, 1);
        PRIORITY_RANK.put(TaskPriority.LOW, 2);
    }

    private TaskOrdering() {
    }

    public static Comparator<Task> byCompletionThenPriority() {
        return Comparator.comparing(Task::isCompleted)
                .thenComparing(task -> PRIORITY_RANK.get(task.getPriority()));
    }
}
