package model;

import javafx.beans.property.BooleanProperty; // &line[CompleteTasks]
import javafx.beans.property.SimpleBooleanProperty; // &line[CompleteTasks]

public final class Task {
    private final String id;
    private final String title;
    private String labelId;
    private final BooleanProperty completed = new SimpleBooleanProperty(); // &line[CompleteTasks]

    public Task(String id, String title) {
        this(id, title, null);
    }

    public Task(String id, String title, String labelId) {
        this.id = id;
        this.title = title;
        this.labelId = labelId;
    }

    // &begin[CompleteTasks]
    public Task(String id, String title, String labelId, boolean completed) {
        this(id, title, labelId);
        this.completed.set(completed);
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void setCompleted(boolean value) {
        completed.set(value);
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