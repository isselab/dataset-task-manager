package taskmanager.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import taskmanager.model.Label;

import java.util.List;
import java.util.Optional;

// &begin[Labels]
public class LabelService {

    private final ObservableList<Label> labels;
    private int nextId;

    public LabelService(List<Label> initialLabels) {
        this.labels = FXCollections.observableArrayList(initialLabels);
        this.nextId = initialLabels.stream().mapToInt(Label::getId).max().orElse(0) + 1;
    }

    public ObservableList<Label> getLabels() {
        return labels;
    }
    // &end[Labels]

    // &begin[CreateLabel]
    public Label createLabel(String name) {
        Optional<Label> existing = labels.stream()
                .filter(label -> label.getName().equalsIgnoreCase(name))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        Label label = new Label(nextId++, name);
        labels.add(label);
        return label;
    }
    // &end[CreateLabel]
}
