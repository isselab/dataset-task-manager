package repository;

import model.Label;
import java.util.List;

public interface LabelRepository {
    List<Label> findAll();

    void saveAll(List<Label> labels);
}
