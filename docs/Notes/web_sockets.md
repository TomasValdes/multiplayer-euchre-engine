## Websocket
- Each user forms a bidirectional channel with the server
- Unlike HTTPs user does not have to pull requests from server
- Multiplayer Games: A common pattern for multiplayer games is to have a server store a game state that serves as the source of truth. Players will take actions or make moves that are sent to the server, which updates the game state, and pushes it out to all players. With HTTP, each player needs to regularly request the game state. With WebSockets, each move is instantly relayed to all players.
- Guide for setting up websockets in current framework: https://spring.io/guides/gs/messaging-stomp-websocket


## Process
Player chooses game to play
HTTP request to show player in lobbies
Player is shown a lobby of games currently waiting for players
Player joins a lobby
New game object is created
Websocket connection request is made
Players added to group connection
Game starts