package model;

public final class Subtask {
    private final String id;
    private String title;
    private boolean completed;

    public Subtask(String id, String title) {
        this(id, title, false);
    }

    public Subtask(String id, String title, boolean completed) {
        this.id = id;
        this.setTitle(title);
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Subtask title must not be empty");
        }
        this.title = title.trim();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
