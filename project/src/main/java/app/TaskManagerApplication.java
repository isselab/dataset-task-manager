package app;

import controller.LoginController;
import controller.MainController;
import service.AuthenticationService;
import service.UserSession;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskManagerApplication extends Application {
    private AuthenticationService authentication;
    private UserSession session;

    @Override
    public void start(Stage stage) {
        authentication = new AuthenticationService();
        session = new UserSession();
        showLogin(stage);
    }

    // &begin[Login]
    private void showLogin(Stage stage) {
        LoginController login = new LoginController(authentication, account -> {
            session.login(account);
            openTaskManager(stage);
        });
        Scene scene = new Scene(login.createView(stage), 420, 300);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        stage.setTitle("Task Manager - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
    // &end[Login]

    // &begin[Logout]
    private void openTaskManager(Stage stage) {
        MainController controller = new MainController(session.currentUser().orElseThrow(), () -> {
            session.logout();
            showLogin(stage);
        });
        Scene scene = new Scene(controller.createView(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.setResizable(true);
    }
    // &end[Logout]

    public static void main(String[] args) {
        launch(args);
    }
}
