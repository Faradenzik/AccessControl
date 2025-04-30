package by.farad.accesscontrol;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/jejugothic.ttf"), 14);
        openAuthWindow(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void openAuthWindow(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(Main.class.getResource("auth_window.fxml")));

        Scene scene = new Scene(root, 800, 390);

        primaryStage.setTitle("Авторизация");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}