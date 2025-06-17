package UI;

import Client.AuthServiceClient;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WelcomeScreen extends Application {

    private final AuthServiceClient authClient = new AuthServiceClient("localhost", 9090);
    @Override
    public void start(Stage primaryStage) {
        // טקסט פתיחה
        Text title = new Text("ברוכים הבאים למשחק קטאן");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setFill(Color.DARKSLATEBLUE);

        // לוגו המשחק (אם יש)
        ImageView logo = new ImageView(new Image("/Utils/catanLogo.png"));
        logo.setFitWidth(200);
        logo.setPreserveRatio(true);

        // כפתורים
        Button loginBtn = new Button("התחברות");
        Button registerBtn = new Button("הרשמה");
        loginBtn.setPrefWidth(200);
        registerBtn.setPrefWidth(200);

        // אנימציה בלחיצה
        addButtonAnimation(loginBtn);
        addButtonAnimation(registerBtn);

        // אירועים עם מעבר
        loginBtn.setOnAction(e -> {
            try {
                new LoginScreen(authClient).start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                showError("שגיאה בטעינת מסך התחברות");
                ex.printStackTrace();
            }
        });

        registerBtn.setOnAction(e -> {
            try {
                new RegisterScreen(authClient).start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                showError("שגיאה בטעינת מסך הרשמה");
                ex.printStackTrace();
            }
        });

        // סידור אנכי
        VBox root = new VBox(20, title, logo, loginBtn, registerBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #fefcea, #f1da36);");

        Scene scene = new Scene(root, 500, 600);
        primaryStage.setTitle("Catan - Welcome");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addButtonAnimation(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("שגיאה");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
