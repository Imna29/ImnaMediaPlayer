package mainPackage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;



public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("GUI.fxml"));
        primaryStage.setTitle("Imna Media Player");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        System.out.println("Started the program");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
