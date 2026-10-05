package service;

import model.Task;

import java.util.Comparator;

public final class TaskOrdering {
    private TaskOrdering() {
    }

    public static Comparator<Task> defaultComparator() {
        Comparator<Task> byDueDate = Comparator.comparing(Task::isOverdue, Comparator.reverseOrder())
                .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())); // &line[DueDates]
        return Comparator.comparing(Task::isCompleted)
                .thenComparing(Task::getPriority, Comparator.reverseOrder())
                .thenComparing(byDueDate); // &line[DueDates]
    }
}
