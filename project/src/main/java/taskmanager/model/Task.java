package taskmanager.model;

// &begin[TaskManagement]
public class Task {

    private final int id;
    private String title;
    // &end[TaskManagement]

    // &begin[AssignLabel]
    private Label label;
    // &end[AssignLabel]

    // &begin[TaskManagement]
    public Task(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    // &end[TaskManagement]

    // &begin[AssignLabel]
    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }
    // &end[AssignLabel]
}
