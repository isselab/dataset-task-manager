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

    public void restore(Label label) {
        labels.add(label);
    }
}
