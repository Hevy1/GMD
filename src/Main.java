import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Classe principale chargée de la création de la scène principale.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/ressources/fxml/Main.fxml"));
        Parent root = fxml.load();
        primaryStage.setTitle("Projet GMD");

        //DataBaseManager.getInstance().importAndResetCSV("resources/baseDeBase.csv");

        Scene primaryScene = new Scene(root, 1200, 800);
        primaryScene.getStylesheets().add("/ressources/style.css");
        primaryStage.setScene(primaryScene);

        primaryStage.show();
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
