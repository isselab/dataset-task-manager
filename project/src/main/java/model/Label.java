package model;

public record Label(String id, String name, String color) {
    public static final String DEFAULT_COLOR = "#3b82f6";

    // &begin[LabelColors]
    public Label {
        color = color == null || color.isBlank() ? DEFAULT_COLOR : color;
    }
    // &end[LabelColors]
}
