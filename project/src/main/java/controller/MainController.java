package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.Comparator;

public final class MainController {
    private final TaskService taskService = new TaskService();
    private final LabelService labelService = new LabelService();
    private final JsonTaskPersistence persistence = new JsonTaskPersistence();

    public MainController() {
        persistence.load(taskService, labelService);
    }

    public BorderPane createView() {
        Label title = new Label("Task Manager");
        title.getStyleClass().add("app-title");

        // &begin[CreateTasks]
        TextField taskInput = new TextField();
        taskInput.setPromptText("New task");
        TextField descriptionInput = new TextField();
        descriptionInput.setPromptText("Description (optional)");
        Button addTask = new Button("Add task");
        addTask.setOnAction(event -> {
            if (!taskInput.getText().trim().isEmpty()) {
                taskService.createTask(taskInput.getText(), descriptionInput.getText());
                taskInput.clear();
                descriptionInput.clear();
                persistence.save(taskService, labelService);
            }
        });
        taskInput.setOnAction(event -> addTask.fire());
        // &end[CreateTasks]

        // &begin[KeywordSearch]
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search tasks by title or description");
        FilteredList<Task> filteredTasks = new FilteredList<>(taskService.getTasks());
        // &end[KeywordSearch]
        // &begin[StatusFilter]
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All tasks", "Open tasks", "Completed tasks");
        statusFilter.getSelectionModel().selectFirst();
        Runnable applyTaskFilters = () -> {
            String keyword = searchInput.getText().trim().toLowerCase();
            String status = statusFilter.getValue();
            filteredTasks.setPredicate(task -> (keyword.isEmpty()
                    || task.getTitle().toLowerCase().contains(keyword)
                    || task.getDescription().toLowerCase().contains(keyword))
                    && (!"Open tasks".equals(status) || !task.isCompleted())
                    && (!"Completed tasks".equals(status) || task.isCompleted()));
        };
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyTaskFilters.run());
        searchInput.textProperty().addListener((observable, oldValue, newValue) -> {
            applyTaskFilters.run();
        });
        // &end[StatusFilter]
        // &begin[TaskPriority]
        SortedList<Task> sortedTasks = new SortedList<>(filteredTasks);
        sortedTasks.setComparator(Comparator.comparing(Task::isCompleted)
                .thenComparing(Task::getPriority, Comparator.reverseOrder()));
        // &end[TaskPriority]
        ListView<Task> taskList = new ListView<>(sortedTasks);
        taskList.setCellFactory(view -> new TaskDialogController(taskService, labelService, persistence));
        VBox.setVgrow(taskList, Priority.ALWAYS);

        // &begin[CreateLabels]
        TextField labelInput = new TextField();
        labelInput.setPromptText("New label");
        ColorPicker labelColor = new ColorPicker(javafx.scene.paint.Color.web("#4f46e5"));
        Button addLabel = new Button("Create label");
        addLabel.setOnAction(event -> {
            if (labelService.createLabel(labelInput.getText(), labelColor.getValue().toString()) != null) {
                labelInput.clear();
                persistence.save(taskService, labelService);
                taskList.refresh();
            }
        });
        labelInput.setOnAction(event -> addLabel.fire());
        // &end[CreateLabels]

        // &begin[RenameLabels]
        ComboBox<model.Label> labelSelector = new ComboBox<>(labelService.getLabels());
        labelSelector.setPromptText("Select label");
        labelSelector.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(model.Label label, boolean empty) {
                super.updateItem(label, empty);
                setText(empty || label == null ? null : label.name());
            }
        });
        labelSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(model.Label label, boolean empty) {
                super.updateItem(label, empty);
                setText(empty || label == null ? null : label.name());
            }
        });
        TextField renameInput = new TextField();
        renameInput.setPromptText("New label name");
        Button rename = new Button("Rename");
        rename.setOnAction(event -> {
            model.Label selected = labelSelector.getValue();
            if (labelService.renameLabel(selected, renameInput.getText(), taskService.getTasks())) {
                persistence.save(taskService, labelService);
                labelService.getLabels().stream()
                        .filter(label -> label.id().equals(selected.id()))
                        .findFirst()
                        .ifPresent(label -> labelSelector.getSelectionModel().select(label));
                taskList.refresh();
            }
        });
        labelSelector.valueProperty().addListener((observable, oldValue, newValue) ->
                renameInput.setText(newValue == null ? "" : newValue.name()));
        // &end[RenameLabels]

        // &begin[DeleteLabels]
        Button delete = new Button("Delete");
        delete.setOnAction(event -> {
            model.Label selected = labelSelector.getValue();
            if (labelService.deleteLabel(selected, taskService.getTasks())) {
                persistence.save(taskService, labelService);
                labelSelector.getSelectionModel().clearSelection();
                renameInput.clear();
                taskList.refresh();
            }
        });
        // &end[DeleteLabels]

        HBox taskForm = new HBox(8, taskInput, descriptionInput, addTask); // &line[CreateTasks]
        HBox.setHgrow(taskInput, Priority.ALWAYS);
        HBox.setHgrow(descriptionInput, Priority.ALWAYS);
        HBox labelForm = new HBox(8, labelInput, labelColor, addLabel);
        HBox.setHgrow(labelInput, Priority.ALWAYS);
        HBox labelManagement = new HBox(8, labelSelector, renameInput, rename, delete); // &line[RenameLabels]
        HBox.setHgrow(labelSelector, Priority.ALWAYS);
        HBox.setHgrow(renameInput, Priority.ALWAYS);
        HBox taskFilters = new HBox(8, searchInput, statusFilter); // &line[StatusFilter]
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        VBox content = new VBox(16, title, taskForm, taskFilters, new Label("Labels"), labelForm, labelManagement, taskList); // &line[KeywordSearch]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }
}
