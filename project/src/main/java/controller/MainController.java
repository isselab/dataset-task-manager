package controller;

import model.Task;
import persistence.JsonTaskPersistence;
import service.LabelService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.scene.paint.Color;

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
        ColorPicker labelColor = new ColorPicker(Color.web(model.Label.DEFAULT_COLOR)); // &line[LabelColors]
        Button addLabel = new Button("Create label");
        addLabel.setOnAction(event -> {
            if (labelService.createLabel(labelInput.getText(), toHex(labelColor.getValue())) != null) { // &line[LabelColors]
                labelInput.clear();
                persistence.save(taskService, labelService);
                taskList.refresh();
            }
        });
        labelInput.setOnAction(event -> addLabel.fire());
        // &end[CreateLabels]

        // &begin[RenameLabels]
        ComboBox<model.Label> labelManager = new ComboBox<>(labelService.getLabels());
        labelManager.setPromptText("Select label");
        labelManager.setConverter(new StringConverter<>() {
            @Override public String toString(model.Label label) { return label == null ? "" : label.name(); }
            @Override public model.Label fromString(String string) { return null; }
        });
        TextField renameInput = new TextField();
        renameInput.setPromptText("New name");
        Button renameLabel = new Button("Rename label");
        renameLabel.setOnAction(event -> {
            model.Label selected = labelManager.getValue();
            if (selected != null) {
                model.Label renamed = labelService.renameLabel(selected, renameInput.getText());
                if (renamed != null) {
                    labelManager.setValue(renamed);
                    renameInput.clear();
                    persistence.save(taskService, labelService);
                    taskList.refresh();
                }
            }
        });
        // &end[RenameLabels]
        // &begin[DeleteLabels]
        Button deleteLabel = new Button("Delete label");
        deleteLabel.setOnAction(event -> {
            model.Label selected = labelManager.getValue();
            if (selected != null) {
                taskService.removeLabelFromAllTasks(selected);
                labelService.deleteLabel(selected);
                labelManager.setValue(null);
                persistence.save(taskService, labelService);
                taskList.refresh();
            }
        });
        // &end[DeleteLabels]

        HBox taskForm = new HBox(8, taskInput, descriptionInput, addTask); // &line[CreateTasks]
        HBox.setHgrow(taskInput, Priority.ALWAYS);
        HBox.setHgrow(descriptionInput, Priority.ALWAYS);
        HBox labelForm = new HBox(8, labelInput, labelColor, addLabel); // &line[LabelColors]
        HBox.setHgrow(labelInput, Priority.ALWAYS);
        HBox labelManageForm = new HBox(8, labelManager, renameInput, renameLabel, deleteLabel);
        HBox.setHgrow(renameInput, Priority.ALWAYS);
        HBox taskFilters = new HBox(8, searchInput, statusFilter); // &line[StatusFilter]
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        VBox content = new VBox(16, title, taskForm, taskFilters, new Label("Labels"), labelForm, labelManageForm, taskList); // &line[KeywordSearch]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }

    // &begin[LabelColors]
    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
    // &end[LabelColors]
}
