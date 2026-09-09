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
        Button saveDetails = new Button("Save changes");
        saveDetails.setOnAction(event -> {
            if (taskService.renameTask(task, title.getText())) {
                task.setDescription(description.getText());
                saveTask(task);
            }
        });
        details.getChildren().addAll(title, description, saveDetails);

        details.getChildren().add(sectionLabel("Status and scheduling"));
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        ChoiceBox<TaskPriority> priority = new ChoiceBox<>(); // &line[TaskPriorityLevels]
        priority.getItems().addAll(TaskPriority.values());
        priority.setValue(task.getPriority());
        DatePicker dueDate = new DatePicker(task.getDueDate()); // &line[TaskDueDates]
        dueDate.setPromptText("Due date");
        Button saveStatus = new Button("Save status and schedule");
        saveStatus.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            task.setPriority(priority.getValue());
            task.setDueDate(dueDate.getValue());
            saveTask(task);
        });
        details.getChildren().addAll(new HBox(8, completed, priority, dueDate), saveStatus);

        details.getChildren().add(sectionLabel("Tags"));
        HBox tagChips = new HBox(4);
        ChoiceBox<TagOption> tagSelector = new ChoiceBox<>(); // &line[AssignTaskTags]
        tagSelector.getItems().add(new TagOption(null, "Add tag..."));
        tagService.getTags().stream().map(tag -> new TagOption(tag.id(), tag.name()))
                .forEach(tagSelector.getItems()::add);
        tagSelector.getSelectionModel().selectFirst();
        Runnable refreshTags = () -> {
            tagChips.getChildren().clear();
            tagService.getTags().stream().filter(tag -> task.getTagIds().contains(tag.id()))
                    .map(tag -> new Label(tag.name())).forEach(tagChips.getChildren()::add);
        };
        tagSelector.setOnAction(event -> {
            TagOption option = tagSelector.getValue();
            if (option.id() != null) {
                tagService.getTags().stream().filter(tag -> tag.id().equals(option.id())).findFirst()
                        .ifPresent(tag -> taskService.assignTag(task, tag));
                saveTask(task);
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
        HBox actions = new HBox(8, delete);
        details.getChildren().add(actions);
        stage.setScene(new Scene(details, 560, 680));
        stage.show();
    }
    // &begin[Subtasks]
    private VBox createSubtasks(Task task) {
        VBox content = new VBox(4);
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
        return content;
    }

    private void rebuildSubtasks(VBox content, Task task) {
        content.getChildren().clear();
        VBox rebuilt = createSubtasks(task);
        content.getChildren().addAll(rebuilt.getChildren());
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
