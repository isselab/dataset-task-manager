package model;

public record Label(String id, String name, String color) {
    public Label(String id, String name) {
        this(id, name, "#4f46e5");
    }
}
