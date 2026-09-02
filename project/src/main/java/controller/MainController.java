package controller;

import model.Task;
import repository.JsonTagRepository;
import repository.JsonTaskRepository;
import service.TagService;
import service.TaskQuery;
import service.TaskOrdering;
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

public final class MainController {
    private final TaskService taskService;
    private final TagService tagService;

    public MainController() {
        taskService = new TaskService(new JsonTaskRepository());
        tagService = new TagService(new JsonTagRepository());
        tagService.load();
        taskService.load();
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
                taskService.save();
                tagService.save();
            }
        });
        taskInput.setOnAction(event -> addTask.fire());
        // &end[CreateTasks]

        // &begin[TaskQuery]
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search tasks by title or description");
        FilteredList<Task> filteredTasks = new FilteredList<>(taskService.getTasks());
        ComboBox<String> statusSelection = new ComboBox<>();
        statusSelection.getItems().addAll(TaskQuery.ALL_TASKS, TaskQuery.OPEN_TASKS, TaskQuery.COMPLETED_TASKS);
        statusSelection.getSelectionModel().selectFirst();
        Runnable applyTaskQuery = () -> {
            filteredTasks.setPredicate(new TaskQuery(searchInput.getText(), statusSelection.getValue()));
        };
        // &end[TaskQuery]
        statusSelection.valueProperty().addListener((observable, oldValue, newValue) -> applyTaskQuery.run());
        searchInput.textProperty().addListener((observable, oldValue, newValue) -> applyTaskQuery.run());
        // &begin[TaskOrdering]
        SortedList<Task> sortedTasks = new SortedList<>(filteredTasks);
        sortedTasks.setComparator(TaskOrdering.defaultComparator());
        // &end[TaskOrdering]
        ListView<Task> taskList = new ListView<>(sortedTasks);
        taskList.setCellFactory(view -> new TaskDialogController(taskService, tagService));
        VBox.setVgrow(taskList, Priority.ALWAYS);

        // &begin[CreateTags]
        TextField tagInput = new TextField();
        tagInput.setPromptText("New tag");
        ColorPicker tagColor = new ColorPicker(javafx.scene.paint.Color.web("#4f46e5"));
        Button addTag = new Button("Create tag");
        addTag.setOnAction(event -> {
            if (tagService.createTag(tagInput.getText(), tagColor.getValue().toString()) != null) {
                tagInput.clear();
                tagService.save();
                taskService.save();
                taskList.refresh();
            }
        });
        tagInput.setOnAction(event -> addTag.fire());
        // &end[CreateTags]

        // &begin[RenameTags]
        ComboBox<model.Tag> tagSelector = new ComboBox<>(tagService.getTags());
        tagSelector.setPromptText("Select tag");
        tagSelector.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(model.Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                setText(empty || tag == null ? null : tag.name());
            }
        });
        tagSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(model.Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                setText(empty || tag == null ? null : tag.name());
            }
        });
        TextField renameInput = new TextField();
        renameInput.setPromptText("New tag name");
        Button rename = new Button("Rename");
        rename.setOnAction(event -> {
            model.Tag selected = tagSelector.getValue();
            if (tagService.renameTag(selected, renameInput.getText(), taskService.getTasks())) {
                tagService.save();
                taskService.save();
                tagService.getTags().stream()
                        .filter(tag -> tag.id().equals(selected.id()))
                        .findFirst()
                        .ifPresent(tag -> tagSelector.getSelectionModel().select(tag));
                taskList.refresh();
            }
        });
        tagSelector.valueProperty().addListener((observable, oldValue, newValue) ->
                renameInput.setText(newValue == null ? "" : newValue.name()));
        // &end[RenameTags]

        // &begin[DeleteTags]
        Button delete = new Button("Delete");
        delete.setOnAction(event -> {
            model.Tag selected = tagSelector.getValue();
            if (tagService.deleteTag(selected, taskService.getTasks())) {
                taskService.save();
                tagService.save();
                tagSelector.getSelectionModel().clearSelection();
                renameInput.clear();
                taskList.refresh();
            }
        });
        // &end[DeleteTags]

        HBox taskForm = new HBox(8, taskInput, descriptionInput, addTask); // &line[CreateTasks]
        HBox.setHgrow(taskInput, Priority.ALWAYS);
        HBox.setHgrow(descriptionInput, Priority.ALWAYS);
        HBox tagForm = new HBox(8, tagInput, tagColor, addTag);
        HBox.setHgrow(tagInput, Priority.ALWAYS);
        HBox tagManagement = new HBox(8, tagSelector, renameInput, rename, delete); // &line[RenameTags]
        HBox.setHgrow(tagSelector, Priority.ALWAYS);
        HBox.setHgrow(renameInput, Priority.ALWAYS);
        HBox taskFilters = new HBox(8, searchInput, statusSelection); // &line[TaskQuery]
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        VBox content = new VBox(16, title, taskForm, taskFilters, new Label("Tags"), tagForm, tagManagement, taskList); // &line[TaskQuery]
        content.setPadding(new Insets(24));
        content.getStyleClass().add("app-root");
        return new BorderPane(content);
    }
}
