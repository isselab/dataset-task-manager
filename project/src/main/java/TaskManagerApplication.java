import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import taskmanager.persistence.JsonTaskPersistence;
import taskmanager.service.LabelService;
import taskmanager.service.TaskService;
import taskmanager.ui.MainController;

public class TaskManagerApplication extends Application {

    @Override
    public void start(Stage stage) {
        // &begin[Persistence]
        JsonTaskPersistence.LoadResult loaded = JsonTaskPersistence.load();
        LabelService labelService = new LabelService(loaded.labels());
        TaskService taskService = new TaskService(loaded.tasks());
        Runnable onChange = () -> JsonTaskPersistence.save(labelService.getLabels(), taskService.getTasks());
        // &end[Persistence]

        MainController mainController = new MainController(taskService, labelService, onChange);

        Scene scene = new Scene(mainController.getView(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());

        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
