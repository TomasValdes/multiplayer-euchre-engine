package CS506Team25.Card_Engine.apitesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.net.URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogInWorkFlowTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private static final String testUsername = "Automated Testing User";
	private static final String localHost = "http://localhost:";

	@AfterAll
	public static void cleanUpCreatedUsers(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port){
		ObjectNode json = restTemplate.getForObject(localHost + port + "/player", ObjectNode.class);
		if (json.has(testUsername)){
			int userID = json.get(testUsername).get("user_id").asInt();
			restTemplate.delete(localHost + port + "/player/" + userID);
		}
	}

	@Test
	void registerUser() {
		ResponseEntity<String> result = restTemplate.postForEntity(localHost + port + "/register?username=" + testUsername, createHeaders(), String.class);

		assertEquals("User successfully registered", result.getBody());
	}

	@Test
	void registerExistingUser() {
		ResponseEntity<String> result = restTemplate.postForEntity(localHost + port + "/register?username=" + testUsername, createHeaders(), String.class);

		assertEquals("User already exists", result.getBody());
	}

	@Test
	void login() {
		ResponseEntity<ObjectNode> result = restTemplate.postForEntity(localHost + port + "/login?username=" + testUsername, createHeaders(), ObjectNode.class);

		assertEquals(testUsername, result.getBody().get("user_name").asText());
	}

	@Test
	void invalidLogin() {
		ResponseEntity<ObjectNode> result = restTemplate.postForEntity(localHost + port + "/login?username=", createHeaders(), ObjectNode.class);

        assertNull(result.getBody());
	}

	private HttpEntity<String> createHeaders(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
	}

}
