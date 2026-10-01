package model;

public record Label(String id, String name, String color) {
    public static final String DEFAULT_COLOR = "#4A90D9";

    public Label(String id, String name) {
        this(id, name, DEFAULT_COLOR);
    }

    public Label {
        color = color == null || color.isBlank() ? DEFAULT_COLOR : color;
    }
}
