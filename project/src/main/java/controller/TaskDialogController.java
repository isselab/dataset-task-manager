package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

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
        HBox chips = new HBox(4);
        chips.setAlignment(Pos.CENTER_LEFT);
        ChoiceBox<LabelOption> selector = new ChoiceBox<>();
        LabelOption placeholder = new LabelOption(null, false, "Labels...");
        selector.getItems().add(placeholder);
        for (model.Label label : labelService.getLabels()) {
            boolean assigned = task.getLabelIds().contains(label.id());
            selector.getItems().add(new LabelOption(label, assigned, (assigned ? "Remove: " : "Add: ") + label.name()));
            if (assigned) {
                Label chip = new Label(label.name());
                chip.setTextFill(Color.web(textColorFor(label.color()))); // &line[LabelColors]
                chip.setBackground(new Background(new BackgroundFill(Color.web(label.color()), new CornerRadii(10), Insets.EMPTY))); // &line[LabelColors]
                chip.setPadding(new Insets(2, 8, 2, 8));
                chip.setMinWidth(Region.USE_PREF_SIZE);
                chips.getChildren().add(chip);
            }
        }
        selector.getSelectionModel().select(placeholder);
        selector.setOnAction(event -> {
            LabelOption option = selector.getValue();
            if (option == null || option.label() == null) return;
            if (option.assigned()) taskService.removeLabel(task, option.label());
            else taskService.assignLabel(task, option.label());
            persistence.save(taskService, labelService);
            updateItem(task, false);
        });
        // &end[AssignTaskLabels]
        // &begin[StatusFilter]
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            persistence.save(taskService, labelService);
        });
        // &end[StatusFilter]
        HBox row = new HBox(12, taskTitle, chips, completed, selector); // &line[AssignTaskLabels]
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskTitle, Priority.ALWAYS);
        setGraphic(row);
    }

    private record LabelOption(model.Label label, boolean assigned, String text) {
        @Override public String toString() { return text; }
    }

    // &begin[LabelColors]
    private static String textColorFor(String color) {
        Color background = Color.web(color);
        double luminance = 0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue();
        return luminance > 0.6 ? "#000000" : "#FFFFFF";
    }
    // &end[LabelColors]
}
