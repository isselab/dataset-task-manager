package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
        ListView<Task> taskList = new ListView<>(filteredTasks);
        taskList.setCellFactory(view -> new TaskDialogController(taskService, labelService, persistence));
        VBox.setVgrow(taskList, Priority.ALWAYS);

        // &begin[CreateLabels]
        TextField labelInput = new TextField();
        labelInput.setPromptText("New label");
        Button addLabel = new Button("Create label");
        addLabel.setOnAction(event -> {
            if (labelService.createLabel(labelInput.getText()) != null) {
                labelInput.clear();
                persistence.save(taskService, labelService);
                taskList.refresh();
            }
        });
        labelInput.setOnAction(event -> addLabel.fire());
        // &end[CreateLabels]

        HBox taskForm = new HBox(8, taskInput, descriptionInput, addTask); // &line[CreateTasks]
        HBox.setHgrow(taskInput, Priority.ALWAYS);
        HBox.setHgrow(descriptionInput, Priority.ALWAYS);
        HBox labelForm = new HBox(8, labelInput, addLabel);
        HBox.setHgrow(labelInput, Priority.ALWAYS);
        HBox taskFilters = new HBox(8, searchInput, statusFilter); // &line[StatusFilter]
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        VBox content = new VBox(16, title, taskForm, taskFilters, new Label("Labels"), labelForm, taskList); // &line[KeywordSearch]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }
}
