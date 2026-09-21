import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDialogController {

    public static class Result {
        public final String title;
        public final Label label;

        public Result(String title, Label label) {
            this.title = title;
            this.label = label;
        }
    }

    private final Label noLabel = new Label(null, "No Label");
    private final ObservableList<Label> labels;

    public TaskDialogController(ObservableList<Label> labels) {
        this.labels = labels;
    }

    // &begin[AssignLabel]
    private ComboBox<Label> buildLabelCombo(Task existingTask) {
        ComboBox<Label> combo = new ComboBox<>();
        List<Label> items = new ArrayList<>();
        items.add(noLabel);
        items.addAll(labels);
        combo.getItems().setAll(items);

        Label current = noLabel;
        if (existingTask != null) {
            current = labels.stream()
                    .filter(l -> l.getId().equals(existingTask.getLabelId()))
                    .findFirst()
                    .orElse(noLabel);
        }
        combo.setValue(current);
        return combo;
    }
    // &end[AssignLabel]

    // &begin[AddTask]
    public Optional<Result> showAndWait(Task existingTask) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle(existingTask == null ? "Add Task" : "Edit Task");

        TextField titleField = new TextField(existingTask == null ? "" : existingTask.getTitle());
        ComboBox<Label> labelCombo = buildLabelCombo(existingTask);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new javafx.scene.control.Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new javafx.scene.control.Label("Label:"), 0, 1);
        grid.add(labelCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            Label selected = labelCombo.getValue();
            Label chosen = (selected == null || selected.getId() == null) ? null : selected;
            return new Result(titleField.getText().trim(), chosen);
        });

        return dialog.showAndWait();
    }
    // &end[AddTask]
}
