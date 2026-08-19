public final class Task {
    private final String id;
    private final String title;
    private final String description;
    private String labelId;
    private boolean completed;

    public Task(String id, String title) {
        this(id, title, "", null);
    }

    public Task(String id, String title, String labelId) {
        this(id, title, "", labelId);
    }

    public Task(String id, String title, String description, String labelId) {
        this(id, title, description, labelId, false);
    }

    public Task(String id, String title, String description, String labelId, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.labelId = labelId;
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLabelId() {
        return labelId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setLabelId(String labelId) {
        this.labelId = labelId;
    }
}
