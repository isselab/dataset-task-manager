package model;

import java.util.ArrayList;
import java.util.List;

public final class Task {
    private final String id;
    private final String title;
    private final String description;
    private final List<String> labelIds;
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
        this(id, title, description, labelId == null ? List.of() : List.of(labelId), completed);
    }

    public Task(String id, String title, String description, List<String> labelIds, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.labelIds = new ArrayList<>(labelIds == null ? List.of() : labelIds);
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

    public List<String> getLabelIds() {
        return List.copyOf(labelIds);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setLabelIds(List<String> labelIds) {
        this.labelIds.clear();
        if (labelIds != null) this.labelIds.addAll(labelIds);
    }

    public void addLabelId(String labelId) {
        if (labelId != null && !labelIds.contains(labelId)) labelIds.add(labelId);
    }

    public void removeLabelId(String labelId) {
        labelIds.remove(labelId);
    }
}
