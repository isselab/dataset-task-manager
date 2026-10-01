package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class TaskDialogController extends ListCell<Task> {
    private final TaskService taskService;
    private final LabelService labelService;
    private final JsonTaskPersistence persistence;

    public TaskDialogController(TaskService taskService, LabelService labelService, JsonTaskPersistence persistence) {
        this.taskService = taskService;
        this.labelService = labelService;
        this.persistence = persistence;
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
        // &begin[AssignTaskLabels]
        ChoiceBox<LabelOption> selector = new ChoiceBox<>();
        LabelOption placeholder = new LabelOption(null, "Labels...");
        selector.getItems().add(placeholder);
        for (model.Label label : labelService.getLabels()) {
            boolean assigned = task.getLabelIds().contains(label.id());
            selector.getItems().add(new LabelOption(label, (assigned ? "Remove: " : "Add: ") + label.name()));
        }
        selector.getSelectionModel().select(placeholder);
        selector.setOnAction(event -> {
            LabelOption option = selector.getValue();
            if (option == null || option.label() == null) return;
            if (task.getLabelIds().contains(option.label().id())) taskService.removeLabel(task, option.label());
            else taskService.assignLabel(task, option.label());
            persistence.save(taskService, labelService);
            updateItem(task, false);
        });
        // &end[AssignTaskLabels]
        // &begin[LabelColors]
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.setMinWidth(Region.USE_PREF_SIZE);
        for (model.Label label : labelService.getLabels()) {
            if (!task.getLabelIds().contains(label.id())) continue;
            Label chip = new Label(label.name());
            chip.getStyleClass().add("label-chip");
            chip.setStyle("-fx-text-fill: " + label.color() + "; -fx-border-color: " + label.color()
                    + "; -fx-border-radius: 10; -fx-font-weight: bold;");
            chip.setMinWidth(Region.USE_PREF_SIZE);
            chips.getChildren().add(chip);
        }
        // &end[LabelColors]
        // &begin[StatusFilter]
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            persistence.save(taskService, labelService);
        });
        // &end[StatusFilter]
        HBox row = new HBox(12, taskTitle, chips, completed, selector); // &line[LabelColors]
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskTitle, Priority.ALWAYS);
        setGraphic(row);
    }

    private record LabelOption(model.Label label, String name) {
        @Override public String toString() { return name; }
    }
}
