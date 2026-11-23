package model;

public enum TaskStatusFilter {
    ALL("All tasks"),
    OPEN("Open tasks"),
    COMPLETED("Completed tasks");

    private final String displayName;

    TaskStatusFilter(String displayName) {
        this.displayName = displayName;
    }

    public boolean accepts(Task task) {
        return switch (this) {
            case ALL -> true;
            case OPEN -> !task.isCompleted();
            case COMPLETED -> task.isCompleted();
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
