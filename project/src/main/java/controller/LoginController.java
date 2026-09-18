package controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.AuthenticationService;

public final class LoginController {
    private final AuthenticationService authentication;
    private final Runnable onLogin;

    public LoginController(AuthenticationService authentication, Runnable onLogin) {
        this.authentication = authentication;
        this.onLogin = onLogin;
    }

    public VBox createView(Stage loginStage) {
        Label title = new Label("Welcome back");
        title.getStyleClass().add("app-title");
        TextField identifier = new TextField();
        identifier.setPromptText("Username or email");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        Button login = new Button("Log in");
        Button register = new Button("Create an account");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            if (authentication.authenticate(identifier.getText(), password.getText()) == null) {
                error.setText("Incorrect username/email or password.");
                password.clear();
            } else {
                onLogin.run();
            }
        });
        register.setOnAction(event -> new RegistrationController(authentication).show(loginStage));
        VBox content = new VBox(12, title, identifier, password, login, register, error);
        content.setPadding(new Insets(28));
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("auth-root");
        return content;
    }
}
