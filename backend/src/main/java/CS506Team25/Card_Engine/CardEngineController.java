package CS506Team25.Card_Engine;

import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.StringWriter;
import java.io.PrintWriter;

@RestController
public class CardEngineController {

    private final String url = "jdbc:mysql://localhost:53346/CardEngine";
    private final String databaseUsername = "root";
    private final String password = "lucky_badger";

    @GetMapping("/")
    public String hello() {
        return "Hello World";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username) {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM Users WHERE username = ?")) {

            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "Logged in";
                } else {
                    return "User does not exist";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred connecting to server.";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String username) {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM Users WHERE username = ?")) {

            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "User already exists";
                }
            }
            
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO Users (username) VALUES (?)");
            insertStatement.setString(1, username);
            int rowsInserted = insertStatement.executeUpdate();
            if(rowsInserted > 0){
                return "User successfully registered";
            }else{
                return "Failed to register user";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred connecting to server.";
        }
    }
}
