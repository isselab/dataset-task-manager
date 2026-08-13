import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TaskManagerApplication extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Task Manager");
        title.getStyleClass().add("app-title");

        BorderPane root = new BorderPane(title);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());

        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
