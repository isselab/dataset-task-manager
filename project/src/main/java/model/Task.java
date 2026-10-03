package model;

import java.util.ArrayList;
import java.util.List;

public final class Task {
    private final String id;
    private final String title;
    private final String description;
    private final List<String> labelIds = new ArrayList<>();
    private boolean completed;

    public Task(String id, String title) {
        this(id, title, "", List.of());
    }

    public Task(String id, String title, String description, List<String> labelIds) {
        this(id, title, description, labelIds, false);
    }

    public Task(String id, String title, String description, List<String> labelIds, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        if (labelIds != null) this.labelIds.addAll(labelIds); // &line[AssignTaskLabels]
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

    // &begin[AssignTaskLabels]
    public List<String> getLabelIds() {
        return List.copyOf(labelIds);
    }
    // &end[AssignTaskLabels]

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // &begin[AssignTaskLabels]
    public void addLabelId(String labelId) {
        if (!labelIds.contains(labelId)) labelIds.add(labelId);
    }

    public void removeLabelId(String labelId) {
        labelIds.remove(labelId);
    }
    // &end[AssignTaskLabels]
}
