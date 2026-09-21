import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.UUID;

public class LabelService {

    // &begin[Labels]
    private final ObservableList<Label> labels = FXCollections.observableArrayList();

    public LabelService(List<Label> initialLabels) {
        labels.addAll(initialLabels);
    }

    public ObservableList<Label> getLabels() {
        return labels;
    }
    // &end[Labels]

    // &begin[CreateLabel]
    public Label addLabel(String name) {
        Label label = new Label(UUID.randomUUID().toString(), name);
        labels.add(label);
        return label;
    }
    // &end[CreateLabel]
}
