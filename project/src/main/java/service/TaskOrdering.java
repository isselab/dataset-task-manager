package service;

import model.Task;

import java.util.Comparator;

public final class TaskOrdering {
    private TaskOrdering() {
    }

    public static Comparator<Task> defaultComparator() {
        return Comparator.comparing(Task::isCompleted)
                .thenComparing(Task::getPriority, Comparator.reverseOrder())
                .thenComparing(Task::isOverdue, Comparator.reverseOrder())
                .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
