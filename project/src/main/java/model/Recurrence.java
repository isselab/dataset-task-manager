package model;

import java.time.LocalDate;

public enum Recurrence {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month");

    private final String label;

    Recurrence(String label) {
        this.label = label;
    }

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