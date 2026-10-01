package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        MenuButton selector = new MenuButton("Labels");
        for (model.Label label : labelService.getLabels()) {
            CheckMenuItem item = new CheckMenuItem(label.name());
            item.setSelected(task.getLabelIds().contains(label.id()));
            item.setOnAction(event -> {
                if (item.isSelected()) taskService.assignLabel(task, label);
                else taskService.removeLabel(task, label);
                persistence.save(taskService, labelService);
                updateItem(task, false);
            });
            selector.getItems().add(item);
            if (item.isSelected()) {
                Label chip = new Label(label.name());
                chip.setStyle("-fx-background-color: " + label.color() + "; -fx-text-fill: " + textColorFor(label.color()) // &line[LabelColors]
                        + "; -fx-padding: 2 8 2 8; -fx-background-radius: 10;");
                chips.getChildren().add(chip);
            }
        }
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

    // &begin[LabelColors]
    private static String textColorFor(String color) {
        Color background = Color.web(color);
        double luminance = 0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue();
        return luminance > 0.6 ? "#000000" : "#FFFFFF";
    }
    // &end[LabelColors]
}
