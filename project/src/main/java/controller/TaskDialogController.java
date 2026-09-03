package controller;

import model.Task;
import model.Subtask;
import model.TaskPriority;
import service.TagService;
import service.TaskService;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;

public final class TaskDialogController extends ListCell<Task> {
    private final TaskService taskService;
    private final TagService tagService;

    public TaskDialogController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setGraphic(null);
            return;
        }
        // &begin[RenameTasks]
        TextField titleInput = new TextField(task.getTitle());
        titleInput.getStyleClass().add("task-title");
        Button rename = new Button("Rename");
        Runnable updateRenameState = () -> rename.setDisable(titleInput.getText().trim().isEmpty());
        titleInput.textProperty().addListener((observable, oldValue, newValue) -> updateRenameState.run());
        rename.setOnAction(event -> {
            if (taskService.renameTask(task, titleInput.getText())) {
                taskService.save();
                updateItem(task, false);
            }
        });
        updateRenameState.run();
        // &end[RenameTasks]
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
        // &begin[AssignTaskTags]
        ChoiceBox<TagOption> selector = new ChoiceBox<>();
        selector.getItems().add(new TagOption(null, "Add tag..."));
        tagService.getTags().stream()
                .map(tag -> new TagOption(tag.id(), tag.name()))
                .forEach(selector.getItems()::add);
        selector.getSelectionModel().selectFirst();
        HBox tagChips = new HBox(4);
        tagService.getTags().stream()
                .filter(tag -> task.getTagIds().contains(tag.id()))
                .map(this::createTagChip)
                .forEach(tagChips.getChildren()::add);
        selector.setOnAction(event -> {
            TagOption option = selector.getValue();
            if (option.id() != null) {
                tagService.getTags().stream()
                        .filter(tag -> tag.id().equals(option.id()))
                        .findFirst()
                        .ifPresent(tag -> taskService.assignTag(task, tag));
                taskService.save();
                tagService.save();
                updateItem(task, false);
            }
        });
        // &end[AssignTaskTags]
        // &begin[TaskDueDates]
        DatePicker dueDatePicker = new DatePicker(task.getDueDate());
        dueDatePicker.setPromptText("Due date");
        dueDatePicker.setOnAction(event -> {
            task.setDueDate(dueDatePicker.getValue());
            taskService.refreshTask(task);
            taskService.save();
            updateItem(task, false);
        });
        Button clearDueDate = new Button("Clear date");
        clearDueDate.setDisable(task.getDueDate() == null);
        clearDueDate.setOnAction(event -> {
            task.setDueDate(null);
            taskService.refreshTask(task);
            taskService.save();
            updateItem(task, false);
        });
        Label overdue = new Label("OVERDUE");
        overdue.getStyleClass().add("overdue-label");
        overdue.setVisible(task.isOverdue());
        overdue.setManaged(task.isOverdue());
        // &end[TaskDueDates]
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(task.isCompleted());
        completed.setOnAction(event -> {
            task.setCompleted(completed.isSelected());
            taskService.refreshTask(task);
            taskService.save();
            tagService.save();
        });
        VBox tags = new VBox(4, tagChips, selector);
        VBox subtasks = createSubtasks(task);
        // &begin[DeleteTasks]
        Button delete = new Button("Delete");
        delete.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete task \"" + task.getTitle() + "\"?", ButtonType.CANCEL, ButtonType.OK);
            confirmation.setTitle("Delete task");
            confirmation.setHeaderText("Delete this task?");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
                if (taskService.deleteTask(task)) {
                    taskService.save();
                    tagService.save();
                }
            });
        });
        // &end[DeleteTasks]
        HBox row = new HBox(12, titleInput, prioritySelector, completed, dueDatePicker, clearDueDate, overdue, tags, subtasks); // &line[TaskDueDates]
        row.getChildren().add(delete); // &line[DeleteTasks]
        row.getChildren().add(rename); // &line[RenameTasks]
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleInput, Priority.ALWAYS);
        setGraphic(row);
    }

    // &begin[Subtasks]
    private VBox createSubtasks(Task task) {
        VBox content = new VBox(4);
        Label heading = new Label("Subtasks");
        content.getChildren().add(heading);
        TextField input = new TextField();
        input.setPromptText("New subtask");
        Button add = new Button("Add");
        Runnable addSubtask = () -> {
            if (taskService.addSubtask(task, input.getText()) != null) {
                input.clear();
                taskService.save();
                updateItem(task, false);
            }
        };
        add.setOnAction(event -> addSubtask.run());
        input.setOnAction(event -> addSubtask.run());
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
                    updateItem(task, false);
                }
            });
            Button delete = new Button("Delete");
            delete.setOnAction(event -> {
                if (taskService.deleteSubtask(task, subtask)) {
                    taskService.save();
                    updateItem(task, false);
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

    private Label createTagChip(model.Tag tag) {
        Label chip = new Label(tag.name());
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web(tag.color()), new CornerRadii(10), Insets.EMPTY)));
        chip.setTextFill(javafx.scene.paint.Color.WHITE);
        return chip;
    }

    private record TagOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
