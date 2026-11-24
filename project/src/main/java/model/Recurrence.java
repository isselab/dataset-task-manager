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

    public LocalDate next(LocalDate date) {
        if (date == null) return null;
        return switch (this) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case NONE -> date;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
