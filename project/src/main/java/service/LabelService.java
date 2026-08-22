package service;

import model.Label;
import model.Task; // &line[RenameLabels]
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;

public final class LabelService {
    private final ObservableList<Label> labels = FXCollections.observableArrayList();

    public ObservableList<Label> getLabels() {
        return labels;
    }

    // &begin[CreateLabels]
    public Label createLabel(String name) {
        return createLabel(name, "#4f46e5");
    }

    public Label createLabel(String name, String color) {
        String normalized = name.trim();
        if (normalized.isEmpty() || labels.stream().anyMatch(label -> label.name().equalsIgnoreCase(normalized))) {
            return null;
        }
        Label label = new Label(UUID.randomUUID().toString(), normalized, color);
        labels.add(label);
        return label;
    }
    // &end[CreateLabels]

    // &begin[RenameLabels]
    public boolean renameLabel(Label label, String name, ObservableList<Task> tasks) {
        if (label == null || name == null) return false;
        String normalized = name.trim();
        if (normalized.isEmpty() || labels.stream()
                .anyMatch(existing -> existing != label && existing.name().equalsIgnoreCase(normalized))) {
            return false;
        }
        Label renamed = new Label(label.id(), normalized, label.color());
        int index = labels.indexOf(label);
        if (index < 0) return false;
        labels.set(index, renamed);
        return true;
    }
    // &end[RenameLabels]

    // &begin[DeleteLabels]
    public boolean deleteLabel(Label label, ObservableList<Task> tasks) {
        if (label == null || !labels.remove(label)) return false;
        if (tasks != null) {
            tasks.forEach(task -> task.removeLabelId(label.id()));
        }
        return true;
    }
    // &end[DeleteLabels]

    public void restore(Label label) {
        labels.add(label);
    }
}
