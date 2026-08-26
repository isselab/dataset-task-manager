package model;

public record Tag(String id, String name, String color) {
    public Tag(String id, String name) {
        this(id, name, "#4f46e5");
    }
}
