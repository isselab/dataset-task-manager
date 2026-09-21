public class Task {

    private final String id;
    private String title;
    private String labelId; // &line[AssignLabel]

    public Task(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLabelId() { return labelId; } // &line[AssignLabel]

    public void setLabelId(String labelId) { this.labelId = labelId; } // &line[AssignLabel]
}
