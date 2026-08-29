package controller;

import model.Task;
import model.TaskPriority;
import service.TagService;
import service.TaskService;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;

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
            return;
        }
        Label taskTitle = new Label(task.getTitle());
        taskTitle.getStyleClass().add("task-title");
        // &begin[TaskPriority]
        ChoiceBox<TaskPriority> prioritySelector = new ChoiceBox<>();
        prioritySelector.getItems().addAll(TaskPriority.values());
        prioritySelector.setValue(task.getPriority());
        prioritySelector.setOnAction(event -> {
            task.setPriority(prioritySelector.getValue());
            taskService.refreshTask(task);
            taskService.save();
            tagService.save();
        });
        // &end[TaskPriority]
        // &begin[AssignTaskTags]
        ChoiceBox<TagOption> selector = new ChoiceBox<>();
        selector.getItems().add(new TagOption(null, "Add tag..."));
        tagService.getTags().stream()
                .map(tag -> new TagOption(tag.id(), tag.name()))
                .forEach(selector.getItems()::add);
        selector.getSelectionModel().selectFirst();
        HBox tagChips = new HBox(4);
        tagService.getTags().stream()
                .filter(tag -> task.getTagIds().contains(tag.id()))
                .map(this::createTagChip)
                .forEach(tagChips.getChildren()::add);
        selector.setOnAction(event -> {
            TagOption option = selector.getValue();
            if (option.id() != null) {
                tagService.getTags().stream()
                        .filter(tag -> tag.id().equals(option.id()))
                        .findFirst()
                        .ifPresent(tag -> taskService.assignTag(task, tag));
                taskService.save();
                tagService.save();
                updateItem(task, false);
            }
        });
        // &end[AssignTaskTags]
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            taskService.refreshTask(task);
            taskService.save();
            tagService.save();
        });
        VBox tags = new VBox(4, tagChips, selector);
        // &begin[DeleteTasks]
        Button delete = new Button("Delete");
        delete.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete task \"" + task.getTitle() + "\"?", ButtonType.CANCEL, ButtonType.OK);
            confirmation.setTitle("Delete task");
            confirmation.setHeaderText("Delete this task?");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
                if (taskService.deleteTask(task)) {
                    taskService.save();
                    tagService.save();
                }
            });
        });
        // &end[DeleteTasks]
        HBox row = new HBox(12, taskTitle, prioritySelector, completed, tags); // &line[TaskPriority]
        row.getChildren().add(delete); // &line[DeleteTasks]
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskTitle, Priority.ALWAYS);
        setGraphic(row);
    }

    private Label createTagChip(model.Tag tag) {
        Label chip = new Label(tag.name());
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web(tag.color()), new CornerRadii(10), Insets.EMPTY)));
        chip.setTextFill(javafx.scene.paint.Color.WHITE);
        return chip;
    }

    private record TagOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
