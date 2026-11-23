package model;

import java.time.LocalDate;

public enum TaskRecurrence {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

    TaskRecurrence(String label) {
        this.label = label;
    }

    public boolean isRecurring() {
        return this != NONE;
    }

    /** Returns the due date of the next occurrence, or null when there is no due date. */
    public LocalDate nextDueDate(LocalDate dueDate) {
        if (dueDate == null) return null;
        return switch (this) {
            case DAILY -> dueDate.plusDays(1);
            case WEEKLY -> dueDate.plusWeeks(1);
            case MONTHLY -> dueDate.plusMonths(1);
            case NONE -> dueDate;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
