package service;

import model.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;

public final class LabelService {
    private final ObservableList<Label> labels = FXCollections.observableArrayList();

    public ObservableList<Label> getLabels() {
        return labels;
    }
    // &begin[CreateLabels]
    public Label createLabel(String name, String color) {
        String normalized = name.trim();
        if (normalized.isEmpty() || labels.stream().anyMatch(label -> label.name().equalsIgnoreCase(normalized))) {
            return null;
        }
        Label label = new Label(UUID.randomUUID().toString(), normalized, color); // &line[LabelColors]
        labels.add(label);
        return label;
    }
    // &end[CreateLabels]

    // &begin[RenameLabels]
    public boolean renameLabel(Label label, String newName) {
        String normalized = newName.trim();
        int index = labels.indexOf(label);
        if (index < 0 || normalized.isEmpty()
                || labels.stream().anyMatch(other -> !other.id().equals(label.id()) && other.name().equalsIgnoreCase(normalized))) {
            return false;
        }
        labels.set(index, new Label(label.id(), normalized, label.color()));
        return true;
    }
    // &end[RenameLabels]

    // &begin[DeleteLabels]
    public void deleteLabel(Label label) {
        labels.remove(label);
    }
    // &end[DeleteLabels]

    public void restore(Label label) {
        labels.add(label);
    }
}
