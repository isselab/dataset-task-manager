package repository;

import model.Tag;
import java.util.List;

/** Persistence contract used by the tag service. */
public interface TagRepository {
    List<Tag> load();

    void save(List<Tag> tags);
}
