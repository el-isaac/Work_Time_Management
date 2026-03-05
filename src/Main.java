import javafx.application.Application;
import javafx.stage.Stage;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends Application{
    public void start(Stage primaryStage){
        Timing_Records time = new Timing_Records(primaryStage);
        time.show();
    }

    public static void main(String[] args) {
    launch();
    }
}