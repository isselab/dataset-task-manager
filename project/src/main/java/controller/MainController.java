package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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

        ListView<Task> taskList = new ListView<>(taskService.getFilteredTasks()); // &line[SearchTasksByKeyword]
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
        // &begin[SearchTasksByKeyword]
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search tasks");
        searchInput.textProperty().addListener((observable, oldValue, newValue) -> taskService.setSearchKeyword(newValue));
        // &end[SearchTasksByKeyword]

        // &begin[FilterTasksByStatus]
        ChoiceBox<TaskStatusFilter> statusInput = new ChoiceBox<>();
        statusInput.getItems().addAll(TaskStatusFilter.values());
        statusInput.setValue(TaskStatusFilter.ALL);
        statusInput.valueProperty().addListener((observable, oldValue, newValue) -> taskService.setStatusFilter(newValue));
        // &end[FilterTasksByStatus]

        HBox findForm = new HBox(8, searchInput, statusInput); // &line[SearchTasksByKeyword,FilterTasksByStatus]
        HBox.setHgrow(searchInput, Priority.ALWAYS); // &line[SearchTasksByKeyword]

        VBox content = new VBox(16, title, taskForm, findForm, new Label("Labels"), labelForm, taskList); // &line[SearchTasksByKeyword,FilterTasksByStatus]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }
}