package taskmanager.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import taskmanager.model.Label;
import taskmanager.model.Task;
import taskmanager.service.LabelService;
import taskmanager.service.TaskService;

public class MainController {

    private final TaskService taskService;
    private final LabelService labelService;
    private final Runnable onChange;

    public MainController(TaskService taskService, LabelService labelService, Runnable onChange) {
        this.taskService = taskService;
        this.labelService = labelService;
        this.onChange = onChange;
    }

    public Parent getView() {
        javafx.scene.control.Label title = new javafx.scene.control.Label("Task Manager");
        title.getStyleClass().add("app-title");

        // &begin[TaskList]
        ListView<Task> taskListView = new ListView<>(taskService.getTasks());
        taskListView.setCellFactory(list -> new TaskCell());
        VBox.setVgrow(taskListView, Priority.ALWAYS);
        // &end[TaskList]

        // &begin[CreateTask]
        Button addTaskButton = new Button("Add Task");
        addTaskButton.setOnAction(e -> handleAddTask());
        HBox actionsBox = new HBox(8, addTaskButton);
        // &end[CreateTask]

        VBox centerBox = new VBox(12, actionsBox, taskListView);
        centerBox.setPadding(new Insets(16, 0, 0, 0));

        BorderPane root = new BorderPane();
        root.setTop(title);
        root.setCenter(centerBox);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("app-root");
        return root;
    }

    // &begin[CreateTask]
    private void handleAddTask() {
        TaskDialogController dialog = new TaskDialogController(labelService);
        dialog.showAndWait().ifPresent(result -> {
            taskService.createTask(result.title(), result.label());
            onChange.run();
        });
    }
    // &end[CreateTask]

    // &begin[TaskList]
    private class TaskCell extends ListCell<Task> {
        private final javafx.scene.control.Label titleLabel = new javafx.scene.control.Label();
        private final ComboBox<Label> labelCombo = new ComboBox<>();
        private final HBox container = new HBox(12, titleLabel, labelCombo);

        TaskCell() {
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            // &begin[ShowTaskLabel]
            labelCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(Label label) {
                    return label == null ? "No label" : label.getName();
                }

                @Override
                public Label fromString(String string) {
                    return null;
                }
            });
            // &end[ShowTaskLabel]

            // &begin[AssignLabel]
            labelCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
                Task task = getItem();
                if (task == null || task.getLabel() == newValue) {
                    return;
                }
                taskService.assignLabel(task, newValue);
                onChange.run();
            });
            // &end[AssignLabel]
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }

            titleLabel.setText(task.getTitle());

            // &begin[ShowTaskLabel]
            ObservableList<Label> options = FXCollections.observableArrayList();
            options.add(null);
            options.addAll(labelService.getLabels());
            labelCombo.setItems(options);
            labelCombo.setValue(task.getLabel());
            // &end[ShowTaskLabel]

            setGraphic(container);
        }
    }
    // &end[TaskList]
}
