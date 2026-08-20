package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
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
        selector.getItems().add(new LabelOption(null, "Add label..."));
        labelService.getLabels().stream()
                .map(label -> new LabelOption(label.id(), label.name()))
                .forEach(selector.getItems()::add);
        selector.getSelectionModel().selectFirst();
        HBox labelChips = new HBox(4);
        labelService.getLabels().stream()
                .filter(label -> task.getLabelIds().contains(label.id()))
                .map(this::createLabelChip)
                .forEach(labelChips.getChildren()::add);
        selector.setOnAction(event -> {
            LabelOption option = selector.getValue();
            if (option.id() != null) {
                labelService.getLabels().stream()
                        .filter(label -> label.id().equals(option.id()))
                        .findFirst()
                        .ifPresent(label -> taskService.assignLabel(task, label));
                persistence.save(taskService, labelService);
                updateItem(task, false);
            }
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
        VBox labels = new VBox(4, labelChips, selector);
        HBox row = new HBox(12, taskTitle, completed, labels);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskTitle, Priority.ALWAYS);
        setGraphic(row);
    }

    private Label createLabelChip(model.Label label) {
        Label chip = new Label(label.name());
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web(label.color()), new CornerRadii(10), Insets.EMPTY)));
        chip.setTextFill(javafx.scene.paint.Color.WHITE);
        return chip;
    }

    private record LabelOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}

