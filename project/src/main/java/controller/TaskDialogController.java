package controller;

import model.Task;
import service.TagService;
import service.TaskService;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;

public final class TaskDialogController extends ListCell<Task> {
    private final TaskService taskService;
    private final TagService tagService;

    public TaskDialogController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setGraphic(null);
            setOnMouseClicked(null);
            return;
        }
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(task.isCompleted() ? "Completed" : "Open");
        Label priority = new Label(String.valueOf(task.getPriority())); // &line[TaskPriorityLevels]
        // &begin[TaskDueDates]
        Label dueDate = new Label(task.getDueDate() == null ? "No due date" : task.getDueDate().toString());
        Label overdue = new Label("OVERDUE");
        overdue.getStyleClass().add("overdue-label");
        overdue.setVisible(task.isOverdue());
        overdue.setManaged(task.isOverdue());
        // &end[TaskDueDates]
        // &begin[AssignTaskTags]
        HBox tagChips = new HBox(4);
        tagService.getTags().stream()
                .filter(tag -> task.getTagIds().contains(tag.id()))
                .map(this::createTagChip)
                .forEach(tagChips.getChildren()::add);
        // &end[AssignTaskTags]
        HBox row = new HBox(12, titleLabel, status, priority, dueDate, overdue, tagChips); // &line[TaskDueDates]
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        setGraphic(row);
        setOnMouseClicked(event -> new TaskDetailsWindow(taskService, tagService, task).show(getScene().getWindow())); // &line[TaskDetails]
    }

    private Label createTagChip(model.Tag tag) {
        Label chip = new Label(tag.name());
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web(tag.color()), new CornerRadii(10), Insets.EMPTY)));
        chip.setTextFill(javafx.scene.paint.Color.WHITE);
        return chip;
    }
}
