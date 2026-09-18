package controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.AuthenticationService;

public final class RegistrationController {
    private final AuthenticationService authentication;

    public RegistrationController(AuthenticationService authentication) { this.authentication = authentication; }

    public void show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Create account");
        TextField username = new TextField();
        username.setPromptText("Username");
        TextField email = new TextField();
        email.setPromptText("Email address");
        PasswordField password = new PasswordField();
        password.setPromptText("Password (8+ characters)");
        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Confirm password");
        Label message = new Label();
        message.getStyleClass().add("error-label");
        Button create = new Button("Register");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            String result = authentication.register(username.getText(), email.getText(), password.getText(), confirmation.getText());
            if (result == null) stage.close();
            else message.setText(result);
        });
        VBox content = new VBox(12, new Label("Create your account"), username, email, password, confirmation, create, message);
        content.setPadding(new Insets(28));
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("auth-root");
        stage.setScene(new Scene(content, 380, 390));
        stage.show();
    }
}