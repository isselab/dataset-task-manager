package app;

import controller.LoginController;
import controller.MainController;
import service.AuthenticationService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskManagerApplication extends Application {
    @Override
    public void start(Stage stage) {
        // &begin[Authentication]
        AuthenticationService authentication = new AuthenticationService();
        LoginController login = new LoginController(authentication, () -> openTaskManager(stage));
        Scene scene = new Scene(login.createView(stage), 420, 300);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        stage.setTitle("Task Manager - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        // &end[Authentication]
    }

    // &begin[Login]
    private void openTaskManager(Stage stage) {
        MainController controller = new MainController();
        Scene scene = new Scene(controller.createView(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.setResizable(true);
    }
    // &end[Login]

    public static void main(String[] args) {
        launch(args);
    }
}
