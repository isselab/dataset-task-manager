package controller;

import model.Task;
import service.TagService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class TaskDialogController extends ListCell<Task> {
    private final TagService tagService;
    private final TaskService taskService;

    public TaskDialogController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setGraphic(null);
            return;
        }

        Label title = new Label(task.getTitle());
        title.getStyleClass().add("task-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label status = new Label(task.isCompleted() ? "Completed" : "Open");
        Label priority = new Label(task.getPriority().toString()); // &line[TaskPriorityLevels]
        Label dueDate = new Label(task.getDueDate() == null ? "No due date" : task.getDueDate().toString());

        HBox tags = new HBox(4);
        tagService.getTags().stream()
                .filter(tag -> task.getTagIds().contains(tag.id()))
                .map(this::createTagChip)
                .forEach(tags.getChildren()::add);

        // &begin[TaskDueDates]
        Label overdue = new Label("OVERDUE");
        overdue.getStyleClass().add("overdue-label");
        overdue.setVisible(task.isOverdue());
        overdue.setManaged(task.isOverdue());
        // &end[TaskDueDates]

        HBox row = new HBox(12, title, status, priority, dueDate, overdue, tags);
        row.setAlignment(Pos.CENTER_LEFT);
        setGraphic(row);
        row.setOnMouseClicked(event -> new TaskDetailsController(taskService, tagService).show(task)); // &line[TaskManagement]
    }

    // &begin[AssignTaskTags]
    private Label createTagChip(model.Tag tag) {
        Label chip = new Label(tag.name());
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web(tag.color()), new CornerRadii(10), Insets.EMPTY)));
        chip.setTextFill(javafx.scene.paint.Color.WHITE);
        return chip;
    }
    // &end[AssignTaskTags]
}
