package taskmanager.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import taskmanager.model.Label;
import taskmanager.service.LabelService;

import java.util.Optional;

public class TaskDialogController {

    public record Result(String title, Label label) {
    }

    private final Dialog<Result> dialog = new Dialog<>();
    private final TextField titleField = new TextField();
    private final ComboBox<Label> labelCombo = new ComboBox<>();

    public TaskDialogController(LabelService labelService) {
        dialog.setTitle("Add Task");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // &begin[CreateTask]
        titleField.setPromptText("Task title");
        javafx.scene.control.Label titleCaption = new javafx.scene.control.Label("Title:");
        // &end[CreateTask]

        // &begin[AssignLabel]
        labelCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Label label) {
                return label == null ? "No label" : label.getName();
            }

            @Override
            public Label fromString(String string) {
                return null;
            }
        });
        labelCombo.getItems().add(null);
        labelCombo.getItems().addAll(labelService.getLabels());
        labelCombo.setValue(null);
        javafx.scene.control.Label labelCaption = new javafx.scene.control.Label("Label:");
        // &end[AssignLabel]

        // &begin[CreateLabel]
        Button newLabelButton = new Button("New Label...");
        newLabelButton.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Label");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Label name:");
            nameDialog.showAndWait()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .ifPresent(name -> {
                        Label created = labelService.createLabel(name);
                        if (!labelCombo.getItems().contains(created)) {
                            labelCombo.getItems().add(created);
                        }
                        labelCombo.setValue(created);
                    });
        });
        // &end[CreateLabel]

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(titleCaption, 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(labelCaption, 0, 1);
        grid.add(labelCombo, 1, 1);
        grid.add(newLabelButton, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Node addButtonNode = dialog.getDialogPane().lookupButton(addButtonType);
        addButtonNode.disableProperty().bind(titleField.textProperty().isEmpty());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButtonType) {
                return new Result(titleField.getText().trim(), labelCombo.getValue());
            }
            return null;
        });
    }

    public Optional<Result> showAndWait() {
        return dialog.showAndWait();
    }
}
