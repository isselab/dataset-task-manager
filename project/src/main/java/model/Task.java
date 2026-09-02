package model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public final class Task {
    private final String id;
    private String title;
    private final String description;
    private final List<String> tagIds;
    private boolean completed;
    private TaskPriority priority;
    private LocalDate dueDate;

    public Task(String id, String title) {
        this(id, title, "", null);
    }

    public Task(String id, String title, String tagId) {
        this(id, title, "", tagId);
    }

    public Task(String id, String title, String description, String tagId) {
        this(id, title, description, tagId, false);
    }

    public Task(String id, String title, String description, String tagId, boolean completed) {
        this(id, title, description, tagId == null ? List.of() : List.of(tagId), completed);
    }

    public Task(String id, String title, String description, List<String> tagIds, boolean completed) {
        this(id, title, description, tagIds, completed, TaskPriority.MEDIUM);
    }

    public Task(String id, String title, String description, List<String> tagIds,
                boolean completed, TaskPriority priority) {
        this(id, title, description, tagIds, completed, priority, null);
    }

    public Task(String id, String title, String description, List<String> tagIds,
                boolean completed, TaskPriority priority, LocalDate dueDate) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.tagIds = new ArrayList<>(tagIds == null ? List.of() : tagIds);
        this.completed = completed;
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
        this.dueDate = dueDate;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    // &begin[RenameTasks]
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title must not be empty");
        }
        this.title = title.trim();
    }
    // &end[RenameTasks]

    public String getDescription() {
        return description;
    }

    public List<String> getTagIds() {
        return List.copyOf(tagIds);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds.clear();
        if (tagIds != null) this.tagIds.addAll(tagIds);
    }

    public void addTagId(String tagId) {
        if (tagId != null && !tagIds.contains(tagId)) tagIds.add(tagId);
    }

    public void removeTagId(String tagId) {
        tagIds.remove(tagId);
    }
}
