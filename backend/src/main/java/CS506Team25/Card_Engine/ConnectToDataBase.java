package CS506Team25.Card_Engine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectToDataBase {
    private static final String url = "jdbc:mysql://localhost:53306/full_house_badger";
    private static final String databaseUsername = "root";
    private static final String password = "lucky_badger";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, databaseUsername, password);
    }
}
