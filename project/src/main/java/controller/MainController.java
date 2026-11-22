package controller;

import javafx.collections.transformation.FilteredList; // &line[KeywordSearch]
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox; // &line[StatusFilter]
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;

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

        TextField taskInput = new TextField();
        taskInput.setPromptText("New task");
        Button addTask = new Button("Add task");
        addTask.setOnAction(event -> {
            if (!taskInput.getText().trim().isEmpty()) {
                taskService.createTask(taskInput.getText());
                taskInput.clear();
                persistence.save(taskService, labelService);
            }
        });
        taskInput.setOnAction(event -> addTask.fire());

        FilteredList<Task> filteredTasks = new FilteredList<>(taskService.getTasks()); // &line[KeywordSearch]
        // &begin[KeywordSearch]
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search tasks");
        // &end[KeywordSearch]

        // &begin[StatusFilter]
        ChoiceBox<TaskService.StatusFilter> statusFilter = new ChoiceBox<>();
        statusFilter.getItems().addAll(TaskService.StatusFilter.values());
        statusFilter.setValue(TaskService.StatusFilter.ALL);
        // &end[StatusFilter]

        // &begin[KeywordSearch]
        // &begin[StatusFilter]
        Runnable applyFilters = () -> filteredTasks.setPredicate(task ->
                TaskService.matchesKeyword(task, searchInput.getText()) && statusFilter.getValue().matches(task));
        searchInput.textProperty().addListener((obs, oldValue, newValue) -> applyFilters.run());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters.run());
        // &end[StatusFilter]
        // &end[KeywordSearch]

        ListView<Task> taskList = new ListView<>(filteredTasks); // &line[KeywordSearch]
        taskList.setCellFactory(view -> new TaskDialogController(taskService, labelService, persistence));
        VBox.setVgrow(taskList, Priority.ALWAYS);

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

        HBox taskForm = new HBox(8, taskInput, addTask);
        HBox.setHgrow(taskInput, Priority.ALWAYS);
        HBox labelForm = new HBox(8, labelInput, addLabel);
        HBox.setHgrow(labelInput, Priority.ALWAYS);
        HBox searchForm = new HBox(8, searchInput, statusFilter); // &line[KeywordSearch]
        HBox.setHgrow(searchInput, Priority.ALWAYS); // &line[KeywordSearch]
        VBox content =new VBox(16, title, taskForm, searchForm, new Label("Labels"), labelForm, taskList); // &line[KeywordSearch]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }
}