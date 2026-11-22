package repository.json;

import model.Label;
import repository.LabelRepository;
import java.util.List;

public final class JsonLabelRepository implements LabelRepository {
    private final JsonStorage storage;

    public JsonLabelRepository(JsonStorage storage) {
        this.storage = storage;
    }

    @Override
    public List<Label> findAll() {
        return storage.loadLabels();
    }

    @Override
    public void saveAll(List<Label> labels) {
        storage.writeLabels(labels);
    }
}
