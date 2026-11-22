package service;

import model.Tag;
import model.Task; // &line[RenameTags]
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import repository.TagRepository;

public final class TagService {
    private final TagRepository repository;
    private final ObservableList<Tag> tags = FXCollections.observableArrayList();

    public TagService(TagRepository repository) {
        this.repository = repository;
    }

    public ObservableList<Tag> getTags() {
        return tags;
    }

    // &begin[CreateTags]
    public Tag createTag(String name) {
        return createTag(name, "#4f46e5");
    }

    public Tag createTag(String name, String color) {
        String normalized = name.trim();
        if (normalized.isEmpty() || tags.stream().anyMatch(tag -> tag.name().equalsIgnoreCase(normalized))) {
            return null;
        }
        Tag tag = new Tag(UUID.randomUUID().toString(), normalized, color);
        tags.add(tag);
        return tag;
    }
    // &end[CreateTags]

    // &begin[RenameTags]
    public boolean renameTag(Tag tag, String name, ObservableList<Task> tasks) {
        if (tag == null || name == null) return false;
        String normalized = name.trim();
        if (normalized.isEmpty() || tags.stream()
                .anyMatch(existing -> existing != tag && existing.name().equalsIgnoreCase(normalized))) {
            return false;
        }
        Tag renamed = new Tag(tag.id(), normalized, tag.color());
        int index = tags.indexOf(tag);
        if (index < 0) return false;
        tags.set(index, renamed);
        return true;
    }
    // &end[RenameTags]

    // &begin[DeleteTags]
    public boolean deleteTag(Tag tag, ObservableList<Task> tasks) {
        if (tag == null || !tags.remove(tag)) return false;
        if (tasks != null) {
            tasks.forEach(task -> task.removeTagId(tag.id()));
        }
        return true;
    }
    // &end[DeleteTags]

    public void restore(Tag tag) {
        tags.add(tag);
    }

    public void load() {
        repository.load().forEach(this::restore);
    }

    public void save() {
        repository.save(tags);
    }
}
