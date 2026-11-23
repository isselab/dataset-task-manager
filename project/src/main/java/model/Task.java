package model;

public final class Task {
    private final String id;
    private final String title;
    private String labelId;
    private boolean completed;

    public Task(String id, String title) {
        this(id, title, null);
    }

    public Task(String id, String title, String labelId) {
        this(id, title, labelId, false);
    }

    public Task(String id, String title, String labelId, boolean completed) {
        this.id = id;
        this.title = title;
        this.labelId = labelId;
        this.completed = completed;
    }

    // &begin[CompleteTasks]
    public boolean isCompleted() {
        return completed;
    }
    // &end[CompleteTasks]

    // &begin[CompleteTasks]
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    // &end[CompleteTasks]

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLabelId() {
        return labelId;
    }

    public void setLabelId(String labelId) {
        this.labelId = labelId;
    }
}