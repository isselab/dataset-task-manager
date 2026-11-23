package model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public final class Task {
    private final String id;
    private String title;
    private String description;
    private final List<String> tagIds;
    private boolean completed;
    private TaskPriority priority;
    private LocalDate dueDate;
    private final List<Subtask> subtasks;
    private TaskRecurrence recurrence; // &line[RecurringTasks]
    private String seriesId; // &line[DeleteRecurringTasks]

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
        this(id, title, description, tagIds, completed, priority, dueDate, List.of());
    }

    public Task(String id, String title, String description, List<String> tagIds,
                boolean completed, TaskPriority priority, LocalDate dueDate, List<Subtask> subtasks) {
        this(id, title, description, tagIds, completed, priority, dueDate, subtasks, TaskRecurrence.NONE);
    }

    public Task(String id, String title, String description, List<String> tagIds,
                boolean completed, TaskPriority priority, LocalDate dueDate, List<Subtask> subtasks,
                TaskRecurrence recurrence) { // &line[RecurringTasks]
        this(id, title, description, tagIds, completed, priority, dueDate, subtasks, recurrence, null);
    }

    public Task(String id, String title, String description, List<String> tagIds,
                boolean completed, TaskPriority priority, LocalDate dueDate, List<Subtask> subtasks,
                TaskRecurrence recurrence, String seriesId) { // &line[DeleteRecurringTasks]
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.tagIds = new ArrayList<>(tagIds == null ? List.of() : tagIds);
        this.completed = completed;
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
        this.dueDate = dueDate;
        this.subtasks = new ArrayList<>(subtasks == null ? List.of() : subtasks);
        this.recurrence = recurrence == null ? TaskRecurrence.NONE : recurrence; // &line[RecurringTasks]
        this.seriesId = this.recurrence.isRecurring() ? seriesId : null; // &line[DeleteRecurringTasks]
    }

    // &begin[RecurringTasks]
    public TaskRecurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(TaskRecurrence recurrence) {
        this.recurrence = recurrence == null ? TaskRecurrence.NONE : recurrence;
        if (!this.recurrence.isRecurring()) this.seriesId = null; // &line[DeleteRecurringTasks]
    }
    // &end[RecurringTasks]

    // &begin[DeleteRecurringTasks]
    /** Identifier shared by all occurrences of a recurring series; defaults to this task's own id. */
    public String getSeriesId() {
        return seriesId == null ? id : seriesId;
    }

    public boolean isInSeriesOf(Task other) {
        return other != null && recurrence.isRecurring() && other.recurrence.isRecurring()
                && getSeriesId().equals(other.getSeriesId());
    }
    // &end[DeleteRecurringTasks]

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title must not be empty");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
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

    public List<Subtask> getSubtasks() {
        return List.copyOf(subtasks);
    }

    public void addSubtask(Subtask subtask) {
        if (subtask != null) subtasks.add(subtask);
    }

    public boolean removeSubtask(Subtask subtask) {
        return subtasks.remove(subtask);
    }
}
