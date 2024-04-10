package CS506Team25.Card_Engine.apitesting;

import CS506Team25.Card_Engine.GameManager;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JoinGameWorkFlowTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private static int gameID = 0;
	private static final String testUsername = "Automated Testing User";
	private static final String testGameName = "_testgame_";
	private static final String localHost = "http://localhost:";

	@BeforeAll
	public static void createUsers(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port){
		restTemplate.postForEntity(localHost + port + "/register?username=" + testUsername, createHeaders(), String.class);
	}

	@AfterAll
	public static void cleanUpCreatedUsersAndGames(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port){
		ObjectNode json = restTemplate.getForObject(localHost + port + "/player", ObjectNode.class);
		if (json.has(testUsername)){
			int userID = json.get(testUsername).get("user_id").asInt();
			restTemplate.delete(localHost + port + "/player/" + userID);
		}

		if (gameID != 0){
			restTemplate.delete(localHost + port + "/games/euchre/" + gameID);
		}
	}

	@Test
	void createGame() {
		ResponseEntity<String> result = restTemplate.postForEntity(localHost + port + "/games/euchre/create-game?gameName=" + testGameName, createHeaders(), String.class);
		assertNotNull(result.getBody());
		gameID = Integer.parseInt(result.getBody());
		assert(gameID > 0);

		ObjectNode response = restTemplate.getForObject(localHost + port + "/games/euchre/" + gameID, ObjectNode.class);
		assertEquals("waiting_for_players", response.get("game_status").asText());

		for (int i =1; i < 5; i++) {
			String curPlayer = "player" + i + "_id";
			assertEquals(0, response.get(curPlayer).asInt());
		}
		assertNotNull(GameManager.getLobby(gameID));
	}

	private static HttpEntity<String> createHeaders(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<String>(headers);
	}

}
