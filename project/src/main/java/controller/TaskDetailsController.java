package controller;

import model.Subtask;
import model.Task;
import model.TaskPriority;
import service.TagService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public final class TaskDetailsController {
    private static final Map<String, Stage> OPEN_WINDOWS = new HashMap<>();

    private final TaskService taskService;
    private final TagService tagService;
    private final Stage stage = new Stage();

    public TaskDetailsController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    public void show(Task task) {
        Stage existing = OPEN_WINDOWS.get(task.getId());
        if (existing != null) {
            existing.toFront();
            existing.requestFocus();
            return;
        }
        OPEN_WINDOWS.put(task.getId(), stage);
        stage.setOnHidden(event -> OPEN_WINDOWS.remove(task.getId()));
        stage.setTitle("Task details");
        render(task);
        stage.show();
    }

    private void render(Task task) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        // &begin[RenameTasks]
        TextField titleInput = new TextField(task.getTitle());
        titleInput.getStyleClass().add("task-title");
        Button rename = new Button("Rename");
        Runnable updateRenameState = () -> rename.setDisable(titleInput.getText().trim().isEmpty());
        titleInput.textProperty().addListener((observable, oldValue, newValue) -> updateRenameState.run());
        rename.setOnAction(event -> {
            if (taskService.renameTask(task, titleInput.getText())) {
                taskService.save();
                render(task);
            }
        });
        updateRenameState.run();
        // &end[RenameTasks]
        HBox.setHgrow(titleInput, Priority.ALWAYS);
        HBox titleRow = new HBox(8, titleInput, rename); // &line[RenameTasks]

        // &begin[EditTaskDescription]
        TextArea descriptionInput = new TextArea(task.getDescription());
        descriptionInput.setPromptText("Description (optional)");
        descriptionInput.setPrefRowCount(3);
        descriptionInput.setWrapText(true);
        Button saveDescription = new Button("Save description");
        saveDescription.setOnAction(event -> {
            if (taskService.updateDescription(task, descriptionInput.getText())) {
                taskService.save();
            }
        });
        // &end[EditTaskDescription]

        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            taskService.refreshTask(task);
            taskService.save();
            tagService.save();
        });

        // &begin[TaskPriorityLevels]
        ChoiceBox<TaskPriority> prioritySelector = new ChoiceBox<>();
        prioritySelector.getItems().addAll(TaskPriority.values());
        prioritySelector.setValue(task.getPriority());
        prioritySelector.setOnAction(event -> {
            task.setPriority(prioritySelector.getValue());
            taskService.refreshTask(task);
            taskService.save();
            tagService.save();
        });
        // &end[TaskPriorityLevels]

        // &begin[TaskDueDates]
        DatePicker dueDatePicker = new DatePicker(task.getDueDate());
        dueDatePicker.setPromptText("Due date");
        dueDatePicker.setOnAction(event -> {
            task.setDueDate(dueDatePicker.getValue());
            taskService.refreshTask(task);
            taskService.save();
            render(task);
        });
        Button clearDueDate = new Button("Clear date");
        clearDueDate.setDisable(task.getDueDate() == null);
        clearDueDate.setOnAction(event -> {
            task.setDueDate(null);
            taskService.refreshTask(task);
            taskService.save();
            render(task);
        });
        Label overdue = new Label("OVERDUE");
        overdue.getStyleClass().add("overdue-label");
        overdue.setVisible(task.isOverdue());
        overdue.setManaged(task.isOverdue());
        // &end[TaskDueDates]

        HBox statusRow = new HBox(12, completed, new Label("Priority"), prioritySelector); // &line[TaskPriorityLevels]
        statusRow.setAlignment(Pos.CENTER_LEFT);
        HBox dueRow = new HBox(8, new Label("Due"), dueDatePicker, clearDueDate, overdue); // &line[TaskDueDates]
        dueRow.setAlignment(Pos.CENTER_LEFT);

        // &begin[AssignTaskTags]
        ChoiceBox<TagOption> selector = new ChoiceBox<>();
        selector.getItems().add(new TagOption(null, "Add tag..."));
        tagService.getTags().stream()
                .filter(tag -> !task.getTagIds().contains(tag.id()))
                .map(tag -> new TagOption(tag.id(), tag.name()))
                .forEach(selector.getItems()::add);
        selector.getSelectionModel().selectFirst();
        HBox tagChips = new HBox(6);
        tagChips.setAlignment(Pos.CENTER_LEFT);
        tagService.getTags().stream()
                .filter(tag -> task.getTagIds().contains(tag.id()))
                .forEach(tag -> {
                    Button remove = new Button("x");
                    remove.setOnAction(event -> {
                        taskService.removeTag(task, tag);
                        taskService.refreshTask(task);
                        taskService.save();
                        tagService.save();
                        render(task);
                    });
                    tagChips.getChildren().add(new HBox(2, TaskDialogController.createTagChip(tag), remove));
                });
        selector.setOnAction(event -> {
            TagOption option = selector.getValue();
            if (option.id() != null) {
                tagService.getTags().stream()
                        .filter(tag -> tag.id().equals(option.id()))
                        .findFirst()
                        .ifPresent(tag -> taskService.assignTag(task, tag));
                taskService.refreshTask(task);
                taskService.save();
                tagService.save();
                render(task);
            }
        });
        // &end[AssignTaskTags]
        HBox tagRow = new HBox(8, tagChips, selector); // &line[AssignTaskTags]
        tagRow.setAlignment(Pos.CENTER_LEFT);

        // &begin[ArchiveTasks]
        Button archive = new Button(task.isArchived() ? "Restore" : "Archive");
        archive.setOnAction(event -> {
            boolean changed = task.isArchived()
                    ? taskService.restoreTask(task)
                    : taskService.archiveTask(task);
            if (changed) {
                taskService.save();
                render(task);
            }
        });
        // &end[ArchiveTasks]

        // &begin[DeleteTasks]
        Button delete = new Button("Delete task");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete task \"" + task.getTitle() + "\"?", ButtonType.CANCEL, ButtonType.OK);
            confirmation.setTitle("Delete task");
            confirmation.setHeaderText("Delete this task?");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
                if (taskService.deleteTask(task)) {
                    taskService.save();
                    tagService.save();
                    stage.close();
                }
            });
        });
        // &end[DeleteTasks]

        content.getChildren().addAll(
                section("Details", titleRow, descriptionInput, saveDescription),
                section("Status and schedule", statusRow, dueRow),
                section("Tags", tagRow),
                createSubtasks(task),
                section("Actions", archive),
                new Separator(),
                section("Danger zone", delete));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        Scene scene = new Scene(scroll, 560, 640);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    private VBox section(String heading, javafx.scene.Node... nodes) {
        Label label = new Label(heading);
        label.getStyleClass().add("section-heading");
        VBox box = new VBox(8, label);
        box.getChildren().addAll(nodes);
        return box;
    }

    // &begin[Subtasks]
    private VBox createSubtasks(Task task) {
        VBox content = section("Subtasks");
        TextField input = new TextField();
        input.setPromptText("New subtask");
        Button add = new Button("Add");
        Runnable addSubtask = () -> {
            if (taskService.addSubtask(task, input.getText()) != null) {
                input.clear();
                taskService.save();
                render(task);
            }
        };
        add.setOnAction(event -> addSubtask.run());
        input.setOnAction(event -> addSubtask.run());
        HBox.setHgrow(input, Priority.ALWAYS);
        HBox form = new HBox(4, input, add);
        for (Subtask subtask : task.getSubtasks()) {
            TextField title = new TextField(subtask.getTitle());
            CheckBox completed = new CheckBox();
            completed.setSelected(subtask.isCompleted());
            completed.setOnAction(event -> {
                taskService.setSubtaskCompleted(task, subtask, completed.isSelected());
                taskService.save();
            });
            Button rename = new Button("Rename");
            rename.setOnAction(event -> {
                if (taskService.renameSubtask(task, subtask, title.getText())) {
                    taskService.save();
                    render(task);
                }
            });
            Button delete = new Button("Delete");
            delete.setOnAction(event -> {
                if (taskService.deleteSubtask(task, subtask)) {
                    taskService.save();
                    render(task);
                }
            });
            HBox subtaskRow = new HBox(4, completed, title, rename, delete);
            HBox.setHgrow(title, Priority.ALWAYS);
            content.getChildren().add(subtaskRow);
        }
        content.getChildren().add(form);
        return content;
    }
    // &end[Subtasks]

    private record TagOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
