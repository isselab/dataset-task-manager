import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class MainController {

    private final TaskService taskService;
    private final LabelService labelService;
    private final JsonTaskPersistence persistence;

    public MainController(TaskService taskService, LabelService labelService, JsonTaskPersistence persistence) {
        this.taskService = taskService;
        this.labelService = labelService;
        this.persistence = persistence;
    }

    public Scene createScene() {
        javafx.scene.control.Label title = new javafx.scene.control.Label("Task Manager");
        title.getStyleClass().add("app-title");

        VBox taskSection = buildTaskSection();
        VBox labelSection = buildLabelSection();

        HBox content = new HBox(24, taskSection, labelSection);
        HBox.setHgrow(taskSection, Priority.ALWAYS);

        VBox root = new VBox(16, title, content);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        return scene;
    }

    // &begin[AddTask]
    private VBox buildTaskSection() {
        javafx.scene.control.Label heading = new javafx.scene.control.Label("Tasks");
        heading.getStyleClass().add("section-title");

        Button addTaskButton = new Button("Add Task");
        addTaskButton.setOnAction(e -> {
            TaskDialogController dialog = new TaskDialogController(labelService.getLabels());
            Optional<TaskDialogController.Result> result = dialog.showAndWait(null);
            result.ifPresent(r -> {
                if (!r.title.isEmpty()) {
                    Task task = taskService.addTask(r.title);
                    taskService.setTaskLabel(task, r.label);
                    persistAll();
                }
            });
        });

        ListView<Task> taskListView = new ListView<>(taskService.getTasks());
        taskListView.setCellFactory(list -> new TaskListCell());
        VBox.setVgrow(taskListView, Priority.ALWAYS);

        VBox section = new VBox(8, heading, addTaskButton, taskListView);
        HBox.setHgrow(section, Priority.ALWAYS);
        return section;
    }
    // &end[AddTask]

    // &begin[CreateLabel]
    private VBox buildLabelSection() {
        javafx.scene.control.Label heading = new javafx.scene.control.Label("Labels");
        heading.getStyleClass().add("section-title");

        TextField labelInput = new TextField();
        labelInput.setPromptText("New label name");

        Button addLabelButton = new Button("Add Label");
        addLabelButton.setOnAction(e -> {
            String nameText = labelInput.getText().trim();
            if (!nameText.isEmpty()) {
                labelService.addLabel(nameText);
                labelInput.clear();
                persistAll();
            }
        });

        HBox inputRow = new HBox(8, labelInput, addLabelButton);
        HBox.setHgrow(labelInput, Priority.ALWAYS);

        ListView<Label> labelListView = new ListView<>(labelService.getLabels());
        VBox.setVgrow(labelListView, Priority.ALWAYS);

        VBox section = new VBox(8, heading, inputRow, labelListView);
        return section;
    }
    // &end[CreateLabel]

    // &begin[Persistence]
    private void persistAll() {
        persistence.save(taskService.getTasks(), labelService.getLabels());
    }
    // &end[Persistence]

    // &begin[AssignLabel]
    private class TaskListCell extends ListCell<Task> {
        private final javafx.scene.control.Label titleLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Label labelChip = new javafx.scene.control.Label();
        private final Button editButton = new Button("Edit Label");
        private final HBox container = new HBox(12, titleLabel, labelChip, editButton);

        TaskListCell() {
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            container.setAlignment(Pos.CENTER_LEFT);
            labelChip.getStyleClass().add("label-chip");

            editButton.setOnAction(e -> {
                Task task = getItem();
                if (task == null) {
                    return;
                }
                TaskDialogController dialog = new TaskDialogController(labelService.getLabels());
                Optional<TaskDialogController.Result> result = dialog.showAndWait(task);
                result.ifPresent(r -> {
                    task.setTitle(r.title);
                    taskService.setTaskLabel(task, r.label);
                    getListView().refresh();
                    persistAll();
                });
            });
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }

            titleLabel.setText(task.getTitle());

            Label current = labelService.getLabels().stream()
                    .filter(l -> l.getId().equals(task.getLabelId()))
                    .findFirst()
                    .orElse(null);
            labelChip.setText(current == null ? "No Label" : current.getName());

            setGraphic(container);
        }
    }
    // &end[AssignLabel]
}
