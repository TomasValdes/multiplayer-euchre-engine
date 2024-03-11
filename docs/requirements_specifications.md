# Requirements and Specification Document

## Full House of Badgers

### Project Abstract

The card game engine is a piece of software designed to cater to the players, offering a wide variety of card games. Players are welcomed into a dynamic virtual arena where they can engage in thrilling matches against friends or challengers from around the globe. The users will interact with a React frontend that communicates with our java backend card engine. The engine will connect to a MySQL database pod to store user logins as well as user scores and leaderboards.

### Customer

The customer for this software in general is anyone who wants to play card games like euchre socially.


### Tech Stack

* Frontend: React
* Backend: Java
* Database: MySQL

<!--A brief description of the customer for this software, both in general (the population who might eventually use such a system) and specifically for this document (the customer(s) who informed this document). Every project will have a customer from the CS506 instructional staff. Requirements should not be derived simply from discussion among team members. Ideally your customer should not only talk to you about requirements but also be excited later in the semester to use the system.-->

### User Requirements

<!--This section lists the behavior that the users see. This information needs to be presented in a logical, organized fashion. It is most helpful if this section is organized in outline form: a bullet list of major topics (e.g., one for each kind of user, or each major piece of system functionality) each with some number of subtopics.-->

| ID   | Description                                                  | Priority | Status |
| ---- | ------------------------------------------------------------ | -------- | ------ |
| #12  | Users should not have to sign into the client. | Med      | Open   |
| &1  | Users should be able to setup a game of euchre and play | High     | Open   |
| &5  | A user looking to delete their account should be able to do so | Med     | Open   |
| &4  | A user looking to see their lifetime statistics should be able to download them | Med      | Open   |
| &7  | A user looking to play a game with just their friends should be able to setup a private lobby | Low     | Open   |


### Use Cases & User Stories

<!--Use cases and user stories that support the user requirements in the previous section. The use cases should be based off user stories. Every major scenario should be represented by a use case, and every use case should say something not already illustrated by the other use cases. Diagrams (such as sequence charts) are encouraged. Ask the customer what are the most important use cases to implement by the deadline. You can have a total ordering, or mark use cases with “must have,” “useful,” or “optional.” For each use case you may list one or more concrete acceptance tests (concrete scenarios that the customer will try to see if the use case is implemented).-->

Our main user stories are as follows:
* As a player I want to be able to play Euchre
* As a player I want to be able to access and download my lifetime stats
* As a player I want to be able to change my name, delete my stats, and delete my account

With sub-stories currently being:
* As a player I want to interact with the cards
* As a player I want to be able to have an account
* As a player I want to have my statistics tracked
* As a player I want to be able to create a private lobby

![Use cases](Images/use-cases.png)

As for acceptance critera, they are as follows:
* A user must be able to log on or make an account (in all use cases)
* A user must be able to create or find a game of euchre and play it to completion
* A user must be able to access their statistics and be able to download a file of their lifetime statistics
* A user must be able to edit characteristics of their account such as their name and but able to delete their stored statistics and/or their account entirely

### User Interface Requirements

<!--Describes any customer user interface requirements including graphical user interface requirements as well as data exchange format requirements. This also should include necessary reporting and other forms of human readable input and output. This should focus on how the feature or product and user interact to create the desired workflow. Describing your intended interface as “easy” or “intuitive” will get you nowhere unless it is accompanied by details.-->

<!--NOTE: Please include illustrations or screenshots of what your user interface would look like -- even if they’re rough -- and interleave it with your description.-->

![Low-fidelity prototype](../frontend/interface-prototypes/Low-Fidelity-Prototype.jpg)
[High-fidelity prototype link](../frontend/interface-prototypes/high-fidelity.pdf)

The high-fidelity interface is centered around a smaller, simpler box at first (see: "Menu", "Profile", and "Settings" panels for reference) that then expands into a full board upon entering a game. The small box from before then becomes the center of the board and a space for in-game notifications (e.g. win message, lose message, and what the candidate suit is). An interactive prototype can be found via a Figma file link [here](https://www.figma.com/file/DkZ46YNauuJN5EXRmbBwsD/506-hfp?type=design&node-id=0%3A1&mode=design&t=gyZW5bYysmys202g-1) which is available to view without a Figma account and for free.

### Security Requirements

* Anyone accessing the backend should be authenticated before sending requests.

<!--Discuss what security requirements are necessary and why. Are there privacy or confidentiality issues? Is your system vulnerable to denial-of-service attacks?-->

### System Requirements

<!--List here all of the external entities, other than users, on which your system will depend. For example, if your system inter-operates with sendmail, or if you will depend on Apache for the web server, or if you must target both Unix and Windows, list those requirements here. List also memory requirements, performance/speed requirements, data capacity requirements, if applicable.-->

As of now, the interface is designed specifically for a 16:9 monitor.

## Specification

<!--A detailed specification of the system. UML, or other diagrams, such as finite automata, or other appropriate specification formalisms, are encouraged over natural language.-->

<!--Include sections, for example, illustrating the database architecture (with, for example, an ERD).-->

<!--Included below are some sample diagrams, including some example tech stack diagrams.-->

### Technology Stack

```mermaid
flowchart RL
subgraph Front End
	A(Javascript: React)
end
	
subgraph Back End
	B(Java)
end
	
subgraph Database
	C[(MySQL)]
end

A <-->|"REST API"| B
B <--> C
```

#### Class Diagram

```mermaid
---
title: Class Diagrams (Game, Card, Player, Round, Trick)
---
classDiagram
    class Game {
        - Long id
        - List -Player- players;
        - List -List-Card-- playersHands;
        - List -Card- allCards;
        - int currentPlayerIndex;
        - Round currentRound
        - List -Round- roundHistory
        + Long getId()
        + void setId(long id)
        + List -Player- getPlayers()
        + void setPlayers(List -Player- players)
        + List -Card- getCards()
        + void setCards(List -Card- cards)
        + int getCurrentPlayerIndex()
        + void setCurrentPlayerIndex(int index)
        + Round getCurrentRound()
        + void setCurrentRound(Round round)
        + List -Round- getRounds()
        + void setRounds(List -Round- rounds)

    }
    class Card {
        - Suit suit
        - Rank rank
        - Enum Rank(NINE, TEN, JACK, QUEEN, KING, ACE)
        - Enum Suit(HEARTS, DIAMONDS, CLUBS, SPADES)
        + Suit getSuit()
        + Rank getRank()
        + void setRank(Rank rank)
        + void setSuit(Suit suit)
    }
    class Player {
        - Long id
        - String playerName
        - List -Card- playerHand
        + void setPlayerName(String playerName)
        + void setPlayerId(Long id)
        + String getPlayerName()
        + Long getPlayerId()
        + void setPlayerHand(List -Card- playerHand)
        + List -Card- getPlayerHand()
    }
    class Round {
        - List -Card- deck;
        - List -Card- allCards;
        - List -Player- players;
        - Card.SUIT trump;
        - Trick currentTrick;
        - int[] trickCount;
        - List -Trick- trickHistory;
        - int dealer;
        - int callingTeam;
        - boolean isInPreGameState;
        - boolean isCardTurnedUp;
        - boolean isStickTheDealer;
        - boolean dealerNeedsToDiscard;
        - Card turnedUpCard;
        - Card.SUIT[] callableSuits;
        + List -Card- getDeck()
        + List -Card- getAllCards()
        + List -Player- getPlayers()
        + Card.Suit getTrump()
        + Trick getCurrentTrick()
        + int[] getTrickCount()
        + List -Trick- getTrickHistory()
        + int getDealer()
        + int getCallingTeam()
        + boolean isInPreGameState()
        + boolean isCardTurnedUp()
        + boolean isStickTheDealer()
        + boolean isDealerNeedsToDiscard()
        + Card getTurnedUpCard()
        + Card.Suit[] getCallableSuits()

        + void setDeck(List -Card- deck)
        + void setAllCards(List -Card- allCards)
        + void setPlayers(List -Player- players)
        + void setTrump(Card.Suit trump)
        + void setCurrentTrick(Trick currentTrick)
        + void setTrickCount(int[] trickCount)
        + void setTrickHistory(List -Trick- trickHistory)
        + void setDealer(int dealer)
        + void setCallingTeam(int callingTeam)
        + void setInPreGameState(boolean isInPreGameState)
        + void setCardTurnedUp(boolean isCardTurnedUp)
        + void setStickTheDealer(boolean isStickTheDealer)
        + void setDealerNeedsToDiscard(boolean dealerNeedsToDiscard)
        + void setTurnedUpCard(Card turnedUpCard)
        + void setCallableSuits(Card.Suit[] callableSuits)
    }
    class Trick {
        - int leadingPlayer;
        - Card.SUIT leadingSuit;
        - int currentPlayer;
        - int currentWinner;
        - Card currentWinningCard;
        - Card.SUIT trump;
        - List -Card- cardsPlayed;
        + int getLeadingPlayer()
        + void setLeadingPlayer(int leadingPlayer)
        + Card.SUIT getLeadingSuit()
        + void setLeadingSuit(Card.SUIT leadingSuit)
        + int getCurrentPlayer()
        + void setCurrentPlayer(int currentPlayer)
        + int getCurrentWinner()
        + void setCurrentWinner(int currentWinner)
        + Card getCurrentWinningCard()
        + void setCurrentWinningCard(Card card)
        + Card.SUIT getTrump()
        + void setTrump(Card.SUIT trump)
        + List -Card- getCardsPlayed()
        + void setCardsPlayed(List -Card- cardsPlayed)

    }
    Game <|-- Card
    Game <|-- Player
    Game <|-- Round
    Game <|-- Trick
```

#### Flowchart

```mermaid
---
title: Euchre Game Flowchart
---

graph TD;
    Start([Start]) --> Shuffle_Deck[/Shuffle Deck/];
    Shuffle_Deck --> Deal_Cards[Deal Cards];
    Deal_Cards --> Bid_Phase_1{Bid Phase 1};
    Bid_Phase_1 -->|Face-up Card| Trump_Face_Up[Select Face-up Card as Trump];
    Trump_Face_Up -->|Suit Named| Play_Phase{Play Phase};
    Trump_Face_Up -->|All Players Pass| Bid_Phase_2{Bid Phase 2};
    Bid_Phase_2 -->|Someone Names Suit| Trump_Named_Suit[Name Suit as Trump];
    Bid_Phase_2 -->|No Suit Named| End_Round[End Round No Points];
    Trump_Named_Suit --> Play_Phase;
    End_Round --> Deal_Cards;
    Play_Phase --> Play_Card[Play Card];
    Play_Card -->|Not Last Trick| Next_Player[Next Player];
    Play_Card -->|Last Trick| Score_Tricks[Score Tricks];
    Next_Player --> Play_Phase;
    Score_Tricks -->|Not Last Round| Next_Round[Next Round];
    Score_Tricks -->|Last Round| Score_Game[Score Game];
    Next_Round --> Deal_Cards;
    Score_Game --> End([End]);
```

#### Behavior

```mermaid
---
title: Euchre Game State Diagram
---
stateDiagram
    [*] --> ShuffleDeck
    ShuffleDeck --> DealCards : Shuffle Deck
    DealCards --> BidPhase1 : Deal Cards
    BidPhase1 --> TrumpFaceUp : Face-up Card
    TrumpFaceUp --> PlayPhase : Suit Named
    TrumpFaceUp --> BidPhase2 : All Players Pass
    BidPhase2 --> TrumpNamedSuit : Someone Names Suit
    BidPhase2 --> EndRound : No Suit Named
    TrumpNamedSuit --> PlayPhase : Trump Named
    EndRound --> DealCards : End Round No Points
    PlayPhase --> PlayCard : Play Card
    PlayCard --> NextPlayer : Not Last Trick
    PlayCard --> ScoreTricks : Last Trick
    NextPlayer --> PlayPhase : Next Player
    ScoreTricks --> NextRound : Not Last Round
    ScoreTricks --> ScoreGame : Last Round
    NextRound --> DealCards : Next Round
    ScoreGame --> End : Game Over
```

#### Sequence Diagram

```mermaid
sequenceDiagram

participant ReactFrontend
participant JavaBackend
participant MySQLDatabase

ReactFrontend ->> JavaBackend: HTTP Request (e.g., GET /api/data)
activate JavaBackend

JavaBackend ->> MySQLDatabase: Query (e.g., SELECT * FROM data_table)
activate MySQLDatabase

MySQLDatabase -->> JavaBackend: Result Set
deactivate MySQLDatabase

JavaBackend -->> ReactFrontend: JSON Response
deactivate JavaBackend
```

### Standards & Conventions

[Coding Standards - HTML](./Coding%20Standards/Coding_Standard_Document_HTML_Frontend.txt)

[Coding Standards - Javascript](./Coding%20Standards/Coding_Standard_Document_Javascript_Frontend.txt)

[Coding Standards - CSS](./Coding%20Standards/Coding_Standard_Document_CSS_Frontend.txt)

[Coding Standards - Java](./Coding%20Standards/Coding_Standard_Document_Java_Backend.txt)

### Architecture Diagram

[Gitlab Link to architecture diagram](Images/archDiagram.png)

[External Link to architecture diagram](https://drive.google.com/file/d/1oqKS26a37G5v7DhYKaEtYxw-6xVGIYjf/view?usp=sharing)

### Database

```mermaid
---
Database ERD for card game engine
---
erDiagram
    User ||--o{ EuchreStats : "created by"

    User {
        uuid user_id PK
        string user_name
        string email
    }

    EuchreStats {
        uuid user_id PK, FK
        int high_score
        date date_of_high_score
    }
```

#### [Lucid Design of Database](https://lucid.app/lucidchart/38c7a343-541e-40ad-a2ea-9f5dfa94c63b/edit?viewport_loc=-932%2C-242%2C1696%2C782%2C0_0&invitationId=inv_97f3b8ff-9664-466a-b975-71f93ab525e1)
![Lucid diagram](./Images/model_databases.png)