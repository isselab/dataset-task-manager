package controller;

import java.util.ArrayList;
import java.util.List;
import model.Subtask;
import model.Task;
import model.TaskPriority;
import model.TaskRecurrence;
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
import javafx.stage.Stage;


public final class TaskDetailsController {
    private final TaskService taskService;
    private final TagService tagService;

    public TaskDetailsController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    public void show(Task task) {
        Stage stage = new Stage();
        stage.setTitle("Task details");

        VBox details = new VBox(12);
        details.setPadding(new Insets(20));
        details.getChildren().add(sectionLabel("Task"));

        TextField title = new TextField(task.getTitle()); // &line[RenameTasks]
        title.setPromptText("Title");
        TextArea description = new TextArea(task.getDescription());
        description.setPromptText("Description");
        description.setPrefRowCount(3);
        details.getChildren().addAll(title, description);

        details.getChildren().add(sectionLabel("Status and scheduling"));
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        ChoiceBox<TaskPriority> priority = new ChoiceBox<>(); // &line[TaskPriorityLevels]
        priority.getItems().addAll(TaskPriority.values());
        priority.setValue(task.getPriority());
        DatePicker dueDate = new DatePicker(task.getDueDate()); // &line[TaskDueDates]
        dueDate.setPromptText("Due date");
        ChoiceBox<TaskRecurrence> recurrence = new ChoiceBox<>(); // &line[RecurringTasks]
        recurrence.getItems().addAll(TaskRecurrence.values()); // &line[RecurringTasks]
        recurrence.setValue(task.getRecurrence()); // &line[RecurringTasks]
        details.getChildren().add(new HBox(8, completed, priority, dueDate, new Label("Repeats:"), recurrence)); // &line[RecurringTasks]

        details.getChildren().add(sectionLabel("Tags"));
        HBox tagChips = new HBox(4);
        ChoiceBox<TagOption> tagSelector = new ChoiceBox<>(); // &line[AssignTaskTags]
        tagSelector.getItems().add(new TagOption(null, "Add tag..."));
        tagService.getTags().stream().map(tag -> new TagOption(tag.id(), tag.name()))
                .forEach(tagSelector.getItems()::add);
        tagSelector.getSelectionModel().selectFirst();
        // Tags chosen in this window; applied to the task when the global Save button is pressed
        List<String> pendingTagIds = new ArrayList<>(task.getTagIds()); // &line[AssignTaskTags]
        Runnable refreshTags = () -> {
            tagChips.getChildren().clear();
            tagService.getTags().stream().filter(tag -> pendingTagIds.contains(tag.id()))
                    .map(tag -> new Label(tag.name())).forEach(tagChips.getChildren()::add);
        };
        tagSelector.setOnAction(event -> {
            TagOption option = tagSelector.getValue();
            if (option != null && option.id() != null) {
                if (!pendingTagIds.contains(option.id())) pendingTagIds.add(option.id());
                refreshTags.run();
                tagSelector.getSelectionModel().selectFirst();
            }
        });
        refreshTags.run();
        details.getChildren().addAll(tagChips, tagSelector);

        details.getChildren().add(sectionLabel("Subtasks"));
        VBox subtasks = createSubtasks(task);
        details.getChildren().add(subtasks);

        details.getChildren().add(sectionLabel("Task actions"));
        Button delete = new Button("Delete"); // &line[DeleteTasks]
        delete.setOnAction(event -> {
            if (task.getRecurrence().isRecurring()) { // &begin[DeleteRecurringTasks]
                ButtonType occurrenceOnly = new ButtonType("This occurrence");
                ButtonType wholeSeries = new ButtonType("Entire series");
                Alert choice = new Alert(Alert.AlertType.CONFIRMATION,
                        "\"" + task.getTitle() + "\" is a recurring task. Delete only this occurrence "
                                + "(future occurrences continue) or the complete recurring series?",
                        occurrenceOnly, wholeSeries, ButtonType.CANCEL);
                choice.setTitle("Delete recurring task");
                choice.setHeaderText("Delete this recurring task?");
                choice.showAndWait().ifPresent(button -> {
                    boolean deleted = button == occurrenceOnly ? taskService.deleteOccurrence(task)
                            : button == wholeSeries && taskService.deleteSeries(task);
                    if (deleted) {
                        saveTask(task);
                        stage.close();
                    }
                });
                return;
            } // &end[DeleteRecurringTasks]
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete task \"" + task.getTitle() + "\"?", ButtonType.CANCEL, ButtonType.OK);
            confirmation.setTitle("Delete task");
            confirmation.setHeaderText("Delete this task?");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
                if (taskService.deleteTask(task)) {
                    saveTask(task);
                    stage.close();
                }
            });
        });
        Button save = new Button("Save"); // &line[PersistTasks]
        save.setOnAction(event -> {
            if (!taskService.renameTask(task, title.getText())) {
                new Alert(Alert.AlertType.WARNING, "The task title must not be empty.", ButtonType.OK).showAndWait();
                return;
            }
            task.setDescription(description.getText());
            task.setPriority(priority.getValue());
            task.setDueDate(dueDate.getValue());
            task.setRecurrence(recurrence.getValue()); // &line[RecurringTasks]
            taskService.setTaskCompleted(task, completed.isSelected()); // &line[RecurringTasks]
            tagService.getTags().stream().filter(tag -> pendingTagIds.contains(tag.id()))
                    .forEach(tag -> taskService.assignTag(task, tag));
            saveTask(task);
        });
        HBox actions = new HBox(8, save, delete);
        details.getChildren().add(actions);
        stage.setScene(new Scene(details, 560, 680));
        stage.show();
    }
    // &begin[Subtasks]
    private VBox createSubtasks(Task task) {
        VBox content = new VBox(4);
        populateSubtasks(content, task);
        return content;
    }

    private void populateSubtasks(VBox content, Task task) {
        content.getChildren().clear();
        TextField input = new TextField(); // &line[AddSubtasks]
        input.setPromptText("New subtask");
        Button add = new Button("Add");
        Runnable addSubtask = () -> {
            if (taskService.addSubtask(task, input.getText()) != null) {
                input.clear();
                saveTask(task);
                rebuildSubtasks(content, task);
            }
        };
        add.setOnAction(event -> addSubtask.run());
        input.setOnAction(event -> addSubtask.run());
        content.getChildren().add(new HBox(4, input, add));
        for (Subtask subtask : task.getSubtasks()) {
            TextField title = new TextField(subtask.getTitle()); // &line[RenameSubtasks]
            CheckBox completed = new CheckBox(); // &line[CompleteSubtasks]
            completed.setSelected(subtask.isCompleted());
            completed.setOnAction(event -> {
                taskService.setSubtaskCompleted(task, subtask, completed.isSelected());
                saveTask(task);
            });
            Button rename = new Button("Rename");
            rename.setOnAction(event -> {
                if (taskService.renameSubtask(task, subtask, title.getText())) {
                    saveTask(task);
                    rebuildSubtasks(content, task);
                }
            });
            Button delete = new Button("Delete"); // &line[DeleteSubtasks]
            delete.setOnAction(event -> {
                if (taskService.deleteSubtask(task, subtask)) {
                    saveTask(task);
                    rebuildSubtasks(content, task);
                }
            });
            HBox row = new HBox(4, completed, title, rename, delete);
            HBox.setHgrow(title, Priority.ALWAYS);
            content.getChildren().add(row);
        }
    }

    private void rebuildSubtasks(VBox content, Task task) {
        populateSubtasks(content, task);
    }
    // &end[Subtasks]

    // &begin[PersistTasks]
    private void saveTask(Task task) {
        taskService.refreshTask(task);
        taskService.save();
        tagService.save();
    }
    // &end[PersistTasks]

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("details-section");
        return label;
    }

    private record TagOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
