package de.idrinth.endlessspace2.modmerger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle(
            "Endless Space 2 Mod Merger | v" + new String(
                App.class.getResourceAsStream("version").readAllBytes()
            )
        );
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("initial.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}