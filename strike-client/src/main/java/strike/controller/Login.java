package strike.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import strike.StrikeClient;

import java.io.IOException;

public class Login {

    private StrikeClient strikeClient;

    public void setStrikeClient(StrikeClient strikeClient) {
        this.strikeClient = strikeClient;
    }

    @FXML
    private TextField idUsername;

    @FXML
    private PasswordField idPassword;

    @FXML
    private ToggleGroup idAccountType;

    @FXML
    private void initialize() {
        /**
         * Initializes the controller class. This method is automatically called
         * after the fxml file has been loaded.
         */
        System.out.println("Login Controller Init...");
    }

    @FXML
    public void login(ActionEvent actionEvent) {
        String username = idUsername.getText();
        System.out.println(username);

        String password =  idPassword.getText();
        System.out.println(password);

        String accType = ((RadioButton) idAccountType.getSelectedToggle()).getText();
        System.out.println(accType);

        /*
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(strikeClient.getPrimaryStage());
        alert.setTitle("Login");
        alert.setHeaderText("Your Login Info");
        alert.setContentText("Username: " + username + ", Password: " + password + ", Account Type: " + accType);
        alert.showAndWait();
        */

        // Just show the chat window for now.
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.strikeClient.getClass().getResource("view/ChatWindow.fxml"));
            AnchorPane chatWindow = loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(chatWindow);
            strikeClient.getPrimaryStage().setScene(scene);
            strikeClient.getPrimaryStage().show();

            // Give the chat window controller any details it needs.
            ChatWindowController controller = loader.getController();
            controller.setStrikeClient(this.strikeClient);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}