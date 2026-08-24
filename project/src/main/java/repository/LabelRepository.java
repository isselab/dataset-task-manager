package repository;

import model.Label;
import java.util.List;

/** Persistence contract used by the label service. */
public interface LabelRepository {
    List<Label> load();

    void save(List<Label> labels);
}
