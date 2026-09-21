import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskManagerApplication extends Application {

    @Override
    public void start(Stage stage) {
        // &begin[Persistence]
        JsonTaskPersistence persistence = new JsonTaskPersistence();
        JsonTaskPersistence.Snapshot snapshot = persistence.load();
        // &end[Persistence]

        TaskService taskService = new TaskService(snapshot.tasks);
        LabelService labelService = new LabelService(snapshot.labels);
        MainController mainController = new MainController(taskService, labelService, persistence);

        Scene scene = mainController.createScene();

        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
