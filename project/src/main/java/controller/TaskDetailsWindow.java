package controller;

import model.Subtask;
import model.Task;
import model.TaskPriority;
import service.TagService;
import service.TaskService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class TaskDetailsWindow {
    private final TaskService taskService;
    private final TagService tagService;
    private final Task task;
    private final Stage stage = new Stage();
    private final VBox subtaskContent = new VBox(4); // &line[Subtasks]

    public TaskDetailsWindow(TaskService taskService, TagService tagService, Task task) {
        this.taskService = taskService;
        this.tagService = tagService;
        this.task = task;
    }

    public void show(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Task details - " + task.getTitle());

        VBox content = new VBox(16, createDetailsSection(), createStatusSection(), createTagSection(),
                createSubtasksSection(), createActionsSection(), createDeleteSection());
        content.setPadding(new Insets(20));
        stage.setScene(new Scene(content, 520, 720));
        stage.show();
    }

    private VBox createDetailsSection() {
        // &begin[RenameTasks]
        TextField titleInput = new TextField(task.getTitle());
        Button rename = new Button("Rename");
        Runnable updateRenameState = () -> rename.setDisable(titleInput.getText().trim().isEmpty());
        titleInput.textProperty().addListener((observable, oldValue, newValue) -> updateRenameState.run());
        rename.setOnAction(event -> {
            if (taskService.renameTask(task, titleInput.getText())) {
                taskService.save();
                stage.setTitle("Task details - " + task.getTitle());
            }
        });
        updateRenameState.run();
        HBox.setHgrow(titleInput, Priority.ALWAYS);
        // &end[RenameTasks]
        // &begin[EditTaskDescription]
        TextArea descriptionInput = new TextArea(task.getDescription());
        descriptionInput.setPromptText("Description");
        descriptionInput.setPrefRowCount(3);
        descriptionInput.setWrapText(true);
        Button saveDescription = new Button("Save description");
        saveDescription.setOnAction(event -> {
            task.setDescription(descriptionInput.getText());
            taskService.refreshTask(task);
            taskService.save();
        });
        // &end[EditTaskDescription]
        return section("Details", new Label("Title"), new HBox(8, titleInput, rename),
                new Label("Description"), descriptionInput, saveDescription);
    }

    private VBox createStatusSection() {
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
        Button clearDueDate = new Button("Clear date");
        clearDueDate.setDisable(task.getDueDate() == null);
        dueDatePicker.setOnAction(event -> {
            task.setDueDate(dueDatePicker.getValue());
            clearDueDate.setDisable(task.getDueDate() == null);
            taskService.refreshTask(task);
            taskService.save();
        });
        clearDueDate.setOnAction(event -> dueDatePicker.setValue(null));
        // &end[TaskDueDates]
        return section("Status, priority and due date", completed,
                new HBox(8, new Label("Priority"), prioritySelector), // &line[TaskPriorityLevels]
                new HBox(8, new Label("Due date"), dueDatePicker, clearDueDate)); // &line[TaskDueDates]
    }

    // &begin[AssignTaskTags]
    private VBox createTagSection() {
        VBox tagBoxes = new VBox(4);
        tagService.getTags().forEach(tag -> {
            CheckBox box = new CheckBox(tag.name());
            box.setSelected(task.getTagIds().contains(tag.id()));
            box.setOnAction(event -> {
                if (box.isSelected()) taskService.assignTag(task, tag);
                else taskService.removeTag(task, tag);
                taskService.refreshTask(task);
                taskService.save();
                tagService.save();
            });
            tagBoxes.getChildren().add(box);
        });
        return section("Tags", tagBoxes);
    }
    // &end[AssignTaskTags]

    // &begin[Subtasks]
    private VBox createSubtasksSection() {
        rebuildSubtasks();
        return section("Subtasks", subtaskContent);
    }

    private void rebuildSubtasks() {
        subtaskContent.getChildren().clear();
        TextField input = new TextField();
        input.setPromptText("New subtask");
        Button add = new Button("Add");
        Runnable addSubtask = () -> {
            if (taskService.addSubtask(task, input.getText()) != null) {
                taskService.save();
                rebuildSubtasks();
            }
        };
        add.setOnAction(event -> addSubtask.run());
        input.setOnAction(event -> addSubtask.run());
        HBox.setHgrow(input, Priority.ALWAYS);
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
                    rebuildSubtasks();
                }
            });
            Button delete = new Button("Delete");
            delete.setOnAction(event -> {
                if (taskService.deleteSubtask(task, subtask)) {
                    taskService.save();
                    rebuildSubtasks();
                }
            });
            HBox subtaskRow = new HBox(4, completed, title, rename, delete);
            HBox.setHgrow(title, Priority.ALWAYS);
            subtaskContent.getChildren().add(subtaskRow);
        }
        subtaskContent.getChildren().add(new HBox(4, input, add));
    }
    // &end[Subtasks]

    private VBox createActionsSection() {
        // &begin[ArchiveTasks]
        Button archive = new Button(task.isArchived() ? "Restore" : "Archive");
        archive.setOnAction(event -> {
            boolean changed = task.isArchived()
                    ? taskService.restoreTask(task)
                    : taskService.archiveTask(task);
            if (changed) {
                taskService.save();
                archive.setText(task.isArchived() ? "Restore" : "Archive");
            }
        });
        // &end[ArchiveTasks]
        return section("Actions", archive); // &line[ArchiveTasks]
    }

    // &begin[DeleteTasks]
    private VBox createDeleteSection() {
        Button delete = new Button("Delete task");
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
        VBox box = section("Danger zone", delete);
        box.setStyle("-fx-border-color: #b91c1c; -fx-border-radius: 4; -fx-padding: 8;");
        return box;
    }
    // &end[DeleteTasks]

    private static VBox section(String heading, javafx.scene.Node... children) {
        Label label = new Label(heading);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(6, label);
        box.getChildren().addAll(children);
        return box;
    }
}
