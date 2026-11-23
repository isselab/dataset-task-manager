package repository;

import model.Label;
import java.util.List;

public final class JsonLabelRepository implements LabelRepository {
    private final JsonTaskStore store;

    public JsonLabelRepository(JsonTaskStore store) {
        this.store = store;
    }

    @Override
    public List<Label> findAll() {
        return store.loadLabels();
    }

    @Override
    public void saveAll(List<Label> labels) {
        store.saveLabels(labels);
    }
}
