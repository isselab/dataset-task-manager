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
        String normalized = name.trim();
        if (normalized.isEmpty() || labels.stream().anyMatch(label -> label.name().equalsIgnoreCase(normalized))) {
            return null;
        }
        Label label = new Label(UUID.randomUUID().toString(), normalized);
        labels.add(label);
        return label;
    }
    // &end[CreateLabels]

    public void restore(Label label) {
        labels.add(label);
    }
}
