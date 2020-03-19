package Scenes;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfirmationBox {
    static boolean answer;

    /**
     * Private constructor to hinder the creation of the utility class
     */
    private ConfirmationBox() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates thes the confirmation box and displays it
     * @param title the title of the conformation box
     * @param message the massage that the confirmation box is going to display
     * @return the answer of the confirmation box
     */
    public static boolean display(String title, String message){
        Stage window = new Stage();

        window.getIcons().add(new Image("file:src/main/App/Images/Logo.png"));
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(250);

        Label lab = new Label(message);

        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");

        yesButton.setOnAction(s -> {
            answer = true;
            window.close();
        });

        noButton.setOnAction(s -> {
            answer = false;
            window.close();
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(lab, yesButton, noButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout);
        window.setScene(scene);

        window.showAndWait();

        return answer;
    }
}
