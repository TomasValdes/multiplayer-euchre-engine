package CS506Team25.Card_Engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.sql.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;


/**
 * The "CardEngineController" class contains all methods to run the euchre card engine.
 * 
 * It contains a test method (hello()), a method to login (login()), and a method to register (register()).
 * 
 */
@RestController
@CrossOrigin
public class CardEngineController {

    private final String url = "jdbc:mysql://db2:3306/full_house_badger";
    private final String databaseUsername = "root";
    private final String password = "lucky_badger";
    private GameManager gameManager;

    public CardEngineController (){
        getLiveLobbies();
    }


    /**
     * Test method to test functionality of @GetMapping("/")
     * 
     * @return "Hello World"
     */
    @GetMapping("/")
    public String hello() {
        return "Hello World";
    }

    /**
     * Login method connecting to the database through @PostMapping("/login")
     * 
     * Uses a prepared statement to find a user in the database (if they exist) and allow them to login
     * 
     * @param username username of player attempting to log in
     * @return JSON of player's id, username, and date joined. Returns null if not found for login failed
     */
    @PostMapping("/login")
    public ObjectNode login(@RequestParam String username) {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_name = ?")) {

            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ObjectNode user = getPlayerInfo(Integer.toString(resultSet.getInt(1)));
                    return user;
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Register method connecting to the database through @PostMapping("/register")
     * 
     * Uses a prepared statement to find if a desired username is already in use
     * If username is available, inserts username into database and registers user
     * 
     * @param username                                  Username attempting to be registered
     * @return "User already exists"                    Desired username already in use
     *         "User successfully registered"           Username registered and added to database
     */
    @PostMapping("/register")
    public String register(@RequestParam String username) {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_name = ?")) {

            statement.setString(1, username);

            // If user is already in the database, return "User already exists"
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "User already exists";
                }
            }

            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO users (user_name) VALUES (?)");

            insertStatement.setString(1, username);
            int rowsInserted = insertStatement.executeUpdate();

            // Check the number of rows inserted to the database
            // If >0, then username must've been added, thus return "User successfully registered"
            // Else, return "Failed to register user"
            if(rowsInserted > 0){
                return "User successfully registered";
            } else {
                return "Failed to register user";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred connecting to server.";
        }
    }

    /**
     * Creates a game that players can join
     * @param gameName name that will be displayed in lobby
     * @param gamePassword optional argument, creates a password for the game
     * @return Newly created game's ID
     */
    @PostMapping("games/euchre/create-game")
    public String createGame(@RequestParam String gameName, @RequestParam(required = false) String gamePassword) {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO euchre_game VALUES (DEFAULT, ?, ?, ?, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT , DEFAULT )", Statement.RETURN_GENERATED_KEYS)) {

            insertStatement.setString(1, gameName);
            if (gamePassword != null){
                insertStatement.setBoolean(2, true);
                insertStatement.setString(3, gameName);
            } else {
                insertStatement.setBoolean(2, false);
                insertStatement.setNull(3, Types.VARCHAR);
            }
            int rowsInserted = insertStatement.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet resultSet = insertStatement.getGeneratedKeys()){
                    if (resultSet.next()) {
                        int gameID = resultSet.getInt(1);
                        GameManager.putLobby(gameID);
                        return Integer.toString(gameID);
                    }
                }
            }

            return "Game could not be created";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred connecting to server.";
        }
    }

    /**
     * Allows a player to select their seat, is functionally how a player joins a game
     * @param id ID of game you're trying ot join
     * @param playerID your player ID
     * @param seatNumber which seat at the table you want to join 1-4
     * @return Response message
     */
    @PostMapping("games/euchre/{id}/select-seat")
    public String assignSeat(@PathVariable String id, int playerID, int seatNumber) {
        String playerSeat = "player" + seatNumber + "_id";
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
            PreparedStatement insertStatement = connection.prepareStatement("UPDATE euchre_game SET " + playerSeat + " = ? WHERE game_id = ?")) {

            insertStatement.setInt(1, playerID);
            insertStatement.setInt(2, Integer.parseInt(id));
            int rowsInserted = insertStatement.executeUpdate();
            if (rowsInserted > 0) {
                //TODO: add player to lobby and get them connected to websocket
                return "Successfully assigned to seat " + seatNumber;
            } else {
                return "Could not assign seat";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred connecting to server.";
        }
    }

    /**
     * @return A JSON of all games that can be joined with key game name and values game id and number of players, null if an error occurs
     */
    @GetMapping("games/euchre/open-games")
    public ObjectNode getOpenGames() {
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
            PreparedStatement statement = connection.prepareStatement("SELECT game_id, game_name, player1_id, player2_id, player3_id, player4_id FROM euchre_game WHERE game_status = 'waiting_for_players'")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ObjectNode game = objectMapper.createObjectNode();
                int players = 0;
                /*
                 *  Count number of player in lobby
                 */
                for (int x = 3; x < 7; x++){
                    if (resultSet.getInt(x) != 0){
                        players++;
                    }
                }
                game.put("game_id",resultSet.getInt(1));
                game.put("number_players", players);
                json.set(resultSet.getString(2), game);
            }
            return json;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get information about a game
     * @param id game's id
     * @return JSON of all of a games attributes, note that player1 is seat 1 and so on
     */
    @GetMapping("/games/euchre/{id}")
    public ObjectNode getGameInfo(@PathVariable String id){
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM euchre_game WHERE game_id = ?")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            statement.setInt(1, Integer.parseInt(id));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                json.put("game_id", resultSet.getInt("game_id"));
                json.put("game_name", resultSet.getString("game_name"));
                json.put("player1_id", resultSet.getInt("player1_id"));
                json.put("player2_id", resultSet.getInt("player2_id"));
                json.put("player3_id", resultSet.getInt("player3_id"));
                json.put("player4_id", resultSet.getInt("player4_id"));
                json.set("player1_name", userIDToUsername(resultSet.getInt("player1_id")));
                json.set("player2_name", userIDToUsername(resultSet.getInt("player2_id")));
                json.set("player3_name", userIDToUsername(resultSet.getInt("player3_id")));
                json.set("player4_name", userIDToUsername(resultSet.getInt("player4_id")));
                json.put("game_status", resultSet.getString("game_status"));
                json.put("winner_1", resultSet.getInt("winner_1"));
                json.put("winner_2", resultSet.getInt("winner_2"));
                json.put("creation_date", String.valueOf(resultSet.getDate("creation_date")));
                return json;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get information about a game
     * @param id game's id
     * @return id of deleted game if game is successfully deleted. -1 if game could not be deleted
     */
    @DeleteMapping("/games/euchre/{id}")
    public int deleteGame(@PathVariable String id){
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
             PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM euchre_game WHERE game_id = ?")) {

            deleteStatement.setInt(1, Integer.parseInt(id));
            int deletedRows = deleteStatement.executeUpdate();
            if (deletedRows > 0){
                return Integer.parseInt(id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * Get information about a player
     * @param id player's id
     * @return JSON of player's id, username, and date joined
     */
    @GetMapping("/player/{id}")
    public ObjectNode getPlayerInfo(@PathVariable String id){
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            statement.setInt(1, Integer.parseInt(id));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                json.put("user_id", resultSet.getInt(1));
                json.put("user_name", resultSet.getString(2));
                json.put("date_joined", String.valueOf(resultSet.getDate(3)));
                return json;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete player with specified id
     * @param id player's id
     * @return id of deleted player, -1 if failed
     */
    @DeleteMapping("/player/{id}")
    public int deletePlayer(@PathVariable String id){
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
             PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {

            deleteStatement.setInt(1, Integer.parseInt(id));
            int deletedRows = deleteStatement.executeUpdate();
            if (deletedRows > 0){
                return Integer.parseInt(id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    @GetMapping("/player")
    public ObjectNode getAllUsers(){
        try (Connection connection = DriverManager.getConnection(url, databaseUsername, password);
             PreparedStatement statement = connection.prepareStatement("SELECT user_id, user_name, date_joined FROM users")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ObjectNode user = objectMapper.createObjectNode();

                user.put("user_id", resultSet.getInt(1));
                user.put("date_joined", String.valueOf(resultSet.getDate(3)));
                json.set(resultSet.getString(2), user);
            }
            return json;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JsonNode userIDToUsername(int userID){
        if (userID != 0) {
            return getPlayerInfo(Integer.toString(userID)).get("user_name");
        } else {
            return null;
        }
    }

    private void getLiveLobbies(){
        ObjectNode json = getOpenGames();
        Consumer<JsonNode> data = (JsonNode node) -> GameManager.putLobby(node.get("game_id").asInt());
        if (data != null){
            json.forEach(data);
        }
    }
}
