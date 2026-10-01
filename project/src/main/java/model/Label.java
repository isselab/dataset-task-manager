package model;

public record Label(String id, String name, String color) { // &line[LabelColors]
    public static final String DEFAULT_COLOR = "#3b82f6"; // &line[LabelColors]
}
