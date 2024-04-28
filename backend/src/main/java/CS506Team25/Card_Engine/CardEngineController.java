package CS506Team25.Card_Engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.sql.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
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

    /**
     * On start up make sure backend and database are synced
     */
    public CardEngineController (){
        addLiveLobbies();
        removeOnGoingGames();
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
     * {
     *     "user_id": Integer,
     *     "user_name": String,
     *     "date_joined": String
     * }
     */
    @PostMapping("/login")
    public ObjectNode login(@RequestParam String username) {
        try (Connection connection = ConnectToDataBase.connect();
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
        try (Connection connection = ConnectToDataBase.connect();
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
    public String createGame(int playerID, String gameName, @RequestParam(required = false) String gamePassword) {
        try (Connection connection = ConnectToDataBase.connect();
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
                        assignSeat(String.valueOf(gameID), playerID, 1);
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
     * @param gameID ID of game you're trying ot join
     * @param playerID your player ID
     * @param seatNumber which seat at the table you want to join 1-4
     * @return of all of a games attributes, note that player1 is seat 1 and so on. GameID also included. Return null if couldn't join game.
     */
    @PostMapping("games/euchre/{gameID}/select-seat")
    public ObjectNode assignSeat(@PathVariable String gameID, int playerID, int seatNumber) {
        String playerSeat = "player" + seatNumber + "_id";
        try (Connection connection = ConnectToDataBase.connect();
            PreparedStatement insertStatement = connection.prepareStatement("UPDATE euchre_game SET " + playerSeat + " = ? WHERE game_id = ?")) {

            insertStatement.setInt(1, playerID);
            insertStatement.setInt(2, Integer.parseInt(gameID));
            int rowsInserted = insertStatement.executeUpdate();
            if (rowsInserted > 0) {
                int gameIDInt = Integer.parseInt(gameID);
                GameManager.getLobby(gameIDInt).joinLobby(playerID, userIDToUsername(playerID), seatNumber);
                return getGameInfo(gameID);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return A JSON of all games that can be joined with key game name and values game id and number of players, null if an error occurs
     * {
     *   "{game-name}": JSON
     *   {
     *       "game_id": Integer,
     *       "number_players": Integer
     *   }
     * }
     */
    @GetMapping("games/euchre/open-games")
    public ObjectNode getOpenGames() {
        try (Connection connection = ConnectToDataBase.connect();
            PreparedStatement statement = connection.prepareStatement("SELECT game_id, game_name, player1_id, player2_id, player3_id, player4_id FROM euchre_game WHERE game_status = 'waiting_for_players'")) {
            return describeGame(statement);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return A JSON of all games that are currently ongoing, null if an error occurs
     * {
     *   "{game-name}": JSON
     *   {
     *       "game_id": Integer,
     *       "number_players": Integer
     *   }
     * }
     */
    @GetMapping("games/euchre/current-games")
    public ObjectNode getCurrentGames() {
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT game_id, game_name, player1_id, player2_id, player3_id, player4_id FROM euchre_game WHERE game_status = 'in_progress'")) {
            return describeGame(statement);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ObjectNode describeGame(PreparedStatement statement) throws SQLException {
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
    }

    /**
     * Get information about a game
     * @param gameID game's id
     * @return JSON of all of a games attributes, note that player1 is seat 1 and so on
     * {
     *     "game_id": Integer,
     *     "game_name": String,
     *     "player1_id": Integer,
     *     "player2_id": Integer,
     *     "player3_id": Integer,
     *     "player4_id": Integer,
     *     "player1_name": String,
     *     "player2_name": String,
     *     "player3_name": String,
     *     "player4_name": String,
     *     "game_status": String,
     *     "winner_1": Integer,
     *     "winner_2": Integer,
     *     "creation_date": String
     * }
     */
    @GetMapping("/games/euchre/{gameID}")
    public ObjectNode getGameInfo(@PathVariable String gameID){
        try (Connection connection = ConnectToDataBase.connect();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM euchre_game WHERE game_id = ?")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            statement.setInt(1, Integer.parseInt(gameID));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                json.put("game_id", resultSet.getInt("game_id"));
                json.put("game_name", resultSet.getString("game_name"));
                json.put("player1_id", resultSet.getInt("player1_id"));
                json.put("player2_id", resultSet.getInt("player2_id"));
                json.put("player3_id", resultSet.getInt("player3_id"));
                json.put("player4_id", resultSet.getInt("player4_id"));
                json.put("player1_name", userIDToUsername(resultSet.getInt("player1_id")));
                json.put("player2_name", userIDToUsername(resultSet.getInt("player2_id")));
                json.put("player3_name", userIDToUsername(resultSet.getInt("player3_id")));
                json.put("player4_name", userIDToUsername(resultSet.getInt("player4_id")));
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

    @GetMapping("/games/euchre/lobbies")
    public ObjectNode getAllLobbies(){
        return GameManager.getAllLobbies();
    }

    /**
     * Get information about a game
     * @param gameID game's id
     * @return id of deleted game if game is successfully deleted. -1 if game could not be deleted
     */
    @DeleteMapping("/games/euchre/{gameID}")
    public int deleteGame(@PathVariable String gameID){
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM euchre_game WHERE game_id = ?")) {

            deleteStatement.setInt(1, Integer.parseInt(gameID));
            int deletedRows = deleteStatement.executeUpdate();
            if (deletedRows > 0){
                return Integer.parseInt(gameID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * Used to delete a player's stored statistics in all games
     *
     * @param playerID the id of the player who wishes to reset their statistics
     * @return true if player stats successfully deleted, false otherwise
     */
    @DeleteMapping("player/{playerID}/stats")
    public Boolean resetPlayersStats(@PathVariable String playerID) {
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement statement = connection.prepareStatement("UPDATE euchre_game set winner_1 = 0 WHERE game_status = 'done' AND winner_1 = ?")) {
            statement.setInt(1, Integer.parseInt(playerID));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement statement = connection.prepareStatement("UPDATE euchre_game set winner_2 = 0 WHERE game_status = 'done' AND winner_2 = ?")) {
            statement.setInt(1, Integer.parseInt(playerID));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Retrieves the statistics for a player identified by the provided playerID.
     * The returned JSON contains information about the games in which the player participated.
     * {gameID}:
     *   - "player1": The username of player 1 in the game.
     *   - "player2": The username of player 2 in the game.
     *   - "player3": The username of player 3 in the game.
     *   - "player4": The username of player 4 in the game.
     *   - "date": The date of the game.
     *   - "winner1": The username of the first winner in the game, or "bot" if it's a bot player.
     *   - "winner2": The username of the second winner in the game, or "bot" if it's a bot player.
     *
     * @param playerID The ID of the player for whom statistics are to be retrieved.
     * @return An ObjectNode containing the player's statistics in JSON format. null otherwise.
     */
    @GetMapping("player/{playerID}/stats")
    public ObjectNode getPlayerStats(@PathVariable String playerID) {
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT game_id, player1_id, player2_id, player3_id, player4_id, creation_date, winner_1, winner_2 FROM euchre_game WHERE game_status = 'done' AND (player1_id = ? OR player2_id = ? OR player3_id = ? OR player4_id = ?)")) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            statement.setInt(1, Integer.parseInt(playerID));
            statement.setInt(2, Integer.parseInt(playerID));
            statement.setInt(3, Integer.parseInt(playerID));
            statement.setInt(4, Integer.parseInt(playerID));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ObjectNode game = objectMapper.createObjectNode();
                game.put("player1", userIDToUsername(resultSet.getInt(2)));
                game.put("player2", userIDToUsername(resultSet.getInt(3)));
                game.put("player3", userIDToUsername(resultSet.getInt(4)));
                game.put("player4", userIDToUsername(resultSet.getInt(5)));
                game.put("date", String.valueOf(resultSet.getDate(6)));
                int winner1 = resultSet.getInt(7);
                int winner2 = resultSet.getInt(8);
                game.put("winner1", winner1 == 0? "bot" : userIDToUsername(winner1));
                game.put("winner2", winner1 == 0? "bot" : userIDToUsername(winner2));


                json.set(String.valueOf(resultSet.getInt(1)), game);
            }
            return json;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

   /**
     * Changing username given the userid
     * @param playerID 	id of user
     * @param newUserName  new username
     * @return string successful or failed.
     */
    @PostMapping("/player/{playerID}/change-username")
    public String changeUsername(@PathVariable String playerID, String newUserName) {
        try (Connection connection = ConnectToDataBase.connect();
            PreparedStatement insertStatement = connection.prepareStatement("UPDATE users SET user_name = ? WHERE user_id = ?")) {

            insertStatement.setString(1, newUserName);
            insertStatement.setInt(2, Integer.parseInt(playerID));
            int rowsInserted = insertStatement.executeUpdate();
            if (rowsInserted > 0) {
                return "successful";
            } else {
                return "failed";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error";
        }
    }
    /**
     * Get information about a player
     * @param playerID player's id
     * @return JSON of player's id, username, and date joined
     * {
     *     "user_id": Integer,
     *     "user_name": String,
     *     "date_joined": String
     * }
     */
    @GetMapping("/player/{playerID}")
    public ObjectNode getPlayerInfo(@PathVariable String playerID){
        try (Connection connection = ConnectToDataBase.connect();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode json = objectMapper.createObjectNode();
            statement.setInt(1, Integer.parseInt(playerID));
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
     * @param playerID player's id
     * @return id of deleted player, -1 if failed
     */
    @DeleteMapping("/player/deleteplayer/{playerID}")
    public int deletePlayer(@PathVariable String playerID){
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {

            deleteStatement.setInt(1, Integer.parseInt(playerID));
            int deletedRows = deleteStatement.executeUpdate();
            if (deletedRows > 0){
                return Integer.parseInt(playerID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * @return A JSON of current users
     * {
     *     "{user-name}": JSON
     *     {
     *         "user_id": Integer,
     *         "date_joined": String
     *     }
     * }
     */
    @GetMapping("/player")
    public ObjectNode getAllUsers(){
        try (Connection connection = ConnectToDataBase.connect();
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

    private String userIDToUsername(int userID){
        if (userID != 0) {
            return getPlayerInfo(Integer.toString(userID)).get("user_name").asText();
        } else {
            return null;
        }
    }

    /**
     * Helper method to create JSON with info about joinable lobbies
     */
    private void addLiveLobbies(){
        ObjectNode json = getOpenGames();
        ArrayList<Integer> games = new ArrayList<>();
        Consumer<JsonNode> data = (JsonNode node) -> games.add(node.get("game_id").asInt());
        if (json != null){
            json.forEach(data);
        }
        for (Integer gameID:
             games) {
            ObjectNode gameJson = getGameInfo(Integer.toString(gameID));
            Player[] players = new Player[4];
            for (int i = 0; i < 4; i++) {
                String keyShortCut = "player" + (i+1);
                if (gameJson.get(keyShortCut + "_id").asInt() != 0){
                    players[i] = new Player(gameJson.get(keyShortCut + "_id").asInt(), gameJson.get(keyShortCut + "_name").asText());
                }
            }
            GameManager.putLobby(gameID, players);
        }
    }

    /**
     * Deletes all games that are currently marked as in_progress from DB.
     * Needed to keep backend and DB synced, recovering state of a in progress game is too difficult.
     */
    private void removeOnGoingGames(){
        try (Connection connection = ConnectToDataBase.connect();
             PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM euchre_game WHERE game_status = 'in_progress'")) {
            int deletedRows = deleteStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
