package taskmanager.model;

public class Label {

    private final int id;
    private String name;

    public Label(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Label)) {
            return false;
        }
        return id == ((Label) other).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
