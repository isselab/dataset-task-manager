import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

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
        ChoiceBox<LabelOption> selector = new ChoiceBox<>();
        selector.getItems().add(new LabelOption(null, "No label"));
        labelService.getLabels().stream().map(label -> new LabelOption(label.id(), label.name())).forEach(selector.getItems()::add);
        selector.getSelectionModel().select(selector.getItems().stream()
                .filter(option -> java.util.Objects.equals(option.id(), task.getLabelId()))
                .findFirst().orElse(selector.getItems().getFirst()));
        selector.setOnAction(event -> {
            LabelOption option = selector.getValue();
            if (option.id() == null) taskService.removeLabel(task);
            else taskService.assignLabel(task, labelService.getLabels().stream().filter(label -> label.id().equals(option.id())).findFirst().orElse(null));
            persistence.save(taskService, labelService);
            updateItem(task, false);
        });
        // &begin[StatusFilter]
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            persistence.save(taskService, labelService);
        });
        // &end[StatusFilter]
        HBox row = new HBox(12, taskTitle, completed, selector);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskTitle, Priority.ALWAYS);
        setGraphic(row);
    }

    private record LabelOption(String id, String name) {
        @Override public String toString() { return name; }
    }
}
