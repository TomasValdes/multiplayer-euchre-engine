package CS506Team25.Card_Engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Game extends Thread{
    // Response from the user, game waits for this to be set when and then resets it to null
    private volatile String userResponse;
    // This game's ID in the database
    public int gameID;
    // The card that is turned up in the first round of bidding
    public Card upCard;
    // An array players that holds info about hands
    public Player[] players;
    // The player whose turn we're currently on
    public Player currentPlayer;
    // Used to build the outputs to players for debugging purposes
    public StringBuilder messageToOutput;
    // A list of inputs user can respond with
    public String[] optionsForPlayer;
    // The number of tricks each player has taken in the current round
    int[] trickCount;
    // The running score for each team, once someone reaches 10, they win the game
    public int[] scores;
    // The team who called trump for the current round
    public int callingTeam;
    // The player who leads for the current trick
    public int startingPlayerIndex;
    // The player who dealt the current round
    int dealerIndex;
    // The cards that have been played in the current trick
    public ArrayList<Card> currentTrick;
    // The whole Euchre deck of 28 cards
    ArrayList<Card> deck;
    // The suit named trump in the current round
    public Card.Suit trump;
    // The suit led with in the current trick
    Card.Suit ledSuit;
    // The player going alone, or null if nobody is going alone
    Player lonerPlayer;
    // Used to handle race conditions with threading, program should be waiting when
    // true
    public volatile boolean isWaitingForInput;
    // Marks whether a bot player is currently moving
    public boolean botIsPlaying;
    // The most recent move made by a player
    public String mostRecentMove;
    // What phase the game currently is in
    public GamePhase currentPhase;
    // Which team won the game
    public Player[] winningPlayers;

    private final Logger logger = LoggerFactory.getLogger(Game.class);


    /**
     * Creates a new game object
     * 
     * @param players used to pass in the websockets (may need to add another param
     *                for the game ID)
     * 
     */
    public Game(int gameID, Player[] players) {
        this.gameID = gameID;
        this.players = players;
        for (int seat = 0; seat < players.length; seat++) {
            if (players[seat] == null)
                players[seat] = new Player(-seat, "BOT_" + seat);
        }
    }

    /**
     * Runs a game of Euchre
     * After the game is over,  is set (0 for players with indexes 0 and 2, 1 for players 1 and 3)
     */
    public void run() {
        messageToOutput = new StringBuilder();
        isWaitingForInput = true;
        botIsPlaying = false;
        createDeck();
        // Set a random player to start
        startingPlayerIndex = (int) (Math.random() * 4);
        // Initialize the scores
        scores = new int[2];
        // Deal rounds until a team wins
        while (scores[0] < 10 && scores[1] < 10) {
            dealRound();
        }
        // Return the winning team
        if (scores[0] >= 10) {
            winningPlayers = new Player[]{players[0], players[2]};
            messageToOutput.append("Team 0 wins\n");
        } else {
            winningPlayers = new Player[]{players[1], players[3]};
            messageToOutput.append("Team 1 wins\n");
        }
        isWaitingForInput = true;

        // Have the thread sleep to make sure the game lingers long enough to get results
        try {
            sleep(10000);
        } catch (InterruptedException ignored) {

        }
    }

    /**
     * Initalizes the deck of cards that will be used for the game
     */
    public void createDeck() {
        deck = new ArrayList<Card>(28);
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
    }

    /**
     * Handles an entire round of a Euchre game, from shuffling the deck to
     * assigning score
     */
    public void dealRound() {
        // Shuffle the deck, initalizing each player's hand and the up card
        shuffle();

        // display up card
        messageToOutput.append(upCard.toString()).append(" is turned up\n");

        firstBiddingRound();

        // Give people the chance to choose any of the other 3 suits as trump, in the
        // same order
        if (trump == null) {
            secondBiddingRound();
        }

        // No trump was named, redeal
        if (trump == null) {
            messageToOutput.append("No trump named, redealing\n");
            return;
        }

        // The person to the left of the dealer starts the first trick
        startingPlayerIndex = (dealerIndex + 1) % 4;

        // Play all 5 tricks in the round
        for (int i = 0; i < 5; i++) {
            // Play a trick, and the starting player of the next trick is the winner of the
            // current trick
            startingPlayerIndex = playTrick();
        }

        scoreRound();
        messageToOutput.append("New scores: [").append(scores[0]).append(", ").append(scores[1]).append("]\n");

        // Rotate the dealer to the left
        dealerIndex = (dealerIndex + 1) % 4;
    }

    /**
     * Handles the first round of bidding where players can choose whether to order
     * up/pick up the up card
     */
    public void firstBiddingRound() {
        // Give people the chance to order up the up card, starting with left of dealer
        // and moving clockwise

        for (int i = 1; i < 5; i++) {
            currentPlayer = players[(dealerIndex + i) % 4];

            // A player can only pick up or order up the card if they have one of that suit
            // in their hand
            boolean canPickUp = false;
            for (Card card : currentPlayer.hand) {
                if (card.getSuit().equals(upCard.getSuit())) {
                    canPickUp = true;
                    break;
                }
            }
            if (canPickUp) {
                // Use websocket to ask playerIndex if they wish to pick up the card
                if (currentPlayer != players[dealerIndex]) {
                    messageToOutput.append("Player ").append(currentPlayer.username).append(", would you like player ")
                            .append(players[dealerIndex].username).append(" to pick up ").append(upCard.toString())
                            .append("?\n");
                } else {
                    messageToOutput.append("Player ").append(currentPlayer.username).append(", would you like to pick up ").append(upCard.toString())
                            .append("?\n");
                }
                // Wait for response
                String response = getInput(GamePhase.AGREE_TO_TRUMP,
                    new String[] {"Yes", "No"});

                // Parse whether response is Yes or No
                if (response.equals("Yes")) {

                    trump = upCard.getSuit();

                    chooseGoingAlone(currentPlayer);

                    // Alert players that the trump has been named
                    messageToOutput.append("Player ")
                            .append(currentPlayer.username)
                            .append(" has named ")
                            .append(trump)
                            .append(" as trump\n");

                    // Set the calling team
                    callingTeam = ((dealerIndex + i) % 4) % 2;

                    currentPlayer = players[dealerIndex];

                    // Dealer must pick up the card and choose a card to discard
                    currentPlayer.hand.add(upCard);
                    messageToOutput.append("Player ")
                            .append(currentPlayer.username)
                            .append(", discard one of your cards")
                            .append("\n");

                    String nameOfCardToDiscard = getInput(GamePhase.DISCARD_FOR_TRUMP, getStringOfCardsInCurrentPlayerHand());
                    int discard = Arrays.asList(getStringOfCardsInCurrentPlayerHand()).indexOf(nameOfCardToDiscard);

                    currentPlayer.hand.remove(discard);

                    currentPlayer = players[(dealerIndex + i) % 4];

                    break;
                }
            } else {
                messageToOutput.append("Player ")
                        .append(currentPlayer.username)
                        .append(", cannot pick up/order up this card\n");
            }

        }
    }

    /**
     * Handles the second round of bidding where players can name any suit except
     * for the rejected up card's suit as trump
     */
    public void secondBiddingRound() {

        for (int i = 1; i < 5; i++) {
            currentPlayer = players[(dealerIndex + i) % 4];
            ArrayList<String> nameableSuits = new ArrayList<>();

            // The player can name any suit they have in their hand except for the suit of
            // the rejected up card
            for (Card card : currentPlayer.hand) {
                if (!nameableSuits.contains(card.getSuit().name())) {
                    nameableSuits.add(card.getSuit().name());
                }
            }
            nameableSuits.remove(upCard.getSuit().name());
            nameableSuits.add("Pass");

            // Use websocket to ask playerIndex if they would like to name trump
            messageToOutput.append("Player ")
                    .append(currentPlayer.username)
                    .append(", would you like to name trump?")
                    .append("\n");

            // Wait for response
            String response = getInput(GamePhase.NAME_TRUMP, nameableSuits.toArray(new String[0]));

            // Parse response
            if (response.equals("Pass")) {
                continue;
            } else {
                // Set trump to what the player named, then end the loop
                try {
                    trump = Card.Suit.valueOf(response);

                    chooseGoingAlone(currentPlayer);

                    // Alert players that trump has been named
                    messageToOutput.append("Player ")
                            .append(currentPlayer.username)
                            .append(" has named ")
                            .append(trump)
                            .append(" as trump\n");

                    // Set the calling team
                    callingTeam = ((dealerIndex + i) % 4) % 2;

                    break;
                } catch (IllegalArgumentException e) { // This should not be needed after moving to websockets
                    messageToOutput.append("Invalid suit, passing\n");
                }
            }
        }
    }

    /**
     * Allows the player to choose whether they wish to go alone
     *
     * @param player, the player going alone
     */
    public void chooseGoingAlone(Player player) {
        currentPlayer = player;
        messageToOutput.append("Player ")
                .append(player.username)
                .append(", would you like to go alone?\n");
        String response = getInput(GamePhase.GO_ALONE,
            new String[] {"Yes", "No"});
        if (response.equals("Yes")) {
            lonerPlayer = player;
            messageToOutput
                    .append("Player ")
                    .append(players[(Arrays.asList(players).indexOf(currentPlayer) + 2) % 4].username)
                    .append(", your partner has chosen to go alone, you cannot play this hand\n");
        }
    }

    /**
     * Shuffles the deck, randomizing it and assigning cards to each player's hand
     * and the up card
     */
    public void shuffle() {
        Collections.shuffle(deck);

        // initalize hand-level data structures and variables
        trickCount = new int[2];
        lonerPlayer = null;
        trump = null;

        // Each of the 4 players gets 5 cards
        for (int i = 0; i < 4; i++) {
            players[i].hand = new ArrayList<>(deck.subList(5 * i, 5 * (i + 1)));

        }

        // The 21st card (index 20) gets turned upwards
        upCard = deck.get(20);

    }

    /**
     * Plays a trick of a round of Euchre, allowing each player to play their cards
     * in order and determining who won the trick
     * 
     * @return the index of the player who won the trick
     */
    public int playTrick() {
        currentTrick = new ArrayList<Card>(4);
        // Clear the ledSuit
        ledSuit = null;
        for (int i = 0; i < 4; i++) {
            // give each player the chance to play their cards
            playCard(players[(i + startingPlayerIndex) % 4]);
        }
        // determine who wins the trick
        int winnerIndex = (startingPlayerIndex + scoreHand()) % 4;

        // Add a trick won to the winning team
        trickCount[winnerIndex % 2]++;

        // Alert users who won the trick
        messageToOutput
                .append("Player ")
                .append(players[winnerIndex].username)
                .append(" won with ")
                .append(currentTrick.get((4 + winnerIndex - startingPlayerIndex) % 4))
                .append("\n");
        return winnerIndex; // return the index of the player who won the trick
    }

    /**
     * Allows a player to play a valid card
     * 
     * @param player the player who will play the card
     */
    public void playCard(Player player) {
        currentPlayer = player;
        if (lonerPlayer != null && player == players[(Arrays.asList(players).indexOf(lonerPlayer) + 2) % 4]) {
            // This player's partner is going alone, so the player cannot play any cards
            // this hand
            currentTrick.add(null);
            return;
        }
        ArrayList<Card> hand = player.hand;
        // send valid cards to frontend
        messageToOutput.append("Player ")
                .append(player.username)
                .append(", play a card")
                .append("\n");
        // Let the player choose their card
        String playersChosenCard;
        playersChosenCard = getInput(GamePhase.PLAY_CARD, getPlayableCardsInHand());
        int indexOfPlayedCard = Arrays.asList(getStringOfCardsInCurrentPlayerHand()).indexOf(playersChosenCard);
        Card playedCard = hand.get(indexOfPlayedCard);
        // If the player led, update the ledSuit to enforce following suit
        if (ledSuit == null) {
            ledSuit = playedCard.getSuit(trump);
        }
        // remove played card from hand
        hand.remove(playedCard);
        messageToOutput.append("Player ")
                .append(player.username)
                .append(" played ")
                .append(playedCard.toString())
                .append("\n");
        currentTrick.add(playedCard);
    }

    /**
     * Helper method to determine which cards in a hand can be played
     * 
     * @param hand the hand of cards to select from
     * @return the subset of cards which are eligible to be played
     */
    public ArrayList<Card> getValidCards(ArrayList<Card> hand) {
        if (trump == null) {
            logger.debug("Can't get playable cards due to trump not being set yet");
            return null;
        }

        // Check to see if the player can follow suit
        boolean canFollowSuit = false;
        for (Card card : hand) {
            if (card.getSuit(trump).equals(ledSuit)) {
                canFollowSuit = true;
                break;
            }
        }

        // If the player cannot follow suit, all cards are valid
        if (!canFollowSuit) {
            return hand;
        }

        // If the player has cards of the led suit, they must play one of them
        ArrayList<Card> validCards = new ArrayList<>();
        for (Card card : hand) {
            if (card.getSuit(trump).equals(ledSuit)) {
                validCards.add(card);
            }
        }
        return validCards;
    }

    /**
     * Determines which of the 4 cards played in a trick wins
     * 
     * @return the index of the winning card (in play order) in the trick
     */
    public int scoreHand() {
        // using the trump and card ranks, determine who played the highest-ranking card
        int winningCardIndex;
        // If the first card 'played' was null, skip it
        if (currentTrick.get(0) == null) {
            winningCardIndex = 1;
        } else {
            winningCardIndex = 0;
        }
        // The winner of the hand is:
        // a) the person who played the highest trump
        // Or b) the person who played the highest card of the led suit if no trump were
        // played

        // Check to see if any of the 3 other players can beat the leader
        for (int i = 1; i < 4; i++) {
            if (winningCardIndex == i) {
                // Only happens if the first card 'played' was null
                continue;
            }
            // The only cards that can beat a card are a higher card of the led suit or a
            // trump
            Card card = currentTrick.get(i);
            if (card == null) {
                // this 'card' was added as a result of a player being skipped due to their
                // partner going alone, skip it
                continue;
            }
            if (card.getSuit(trump).equals(ledSuit) || card.isTrump(trump)) {
                if (card.getRanking(trump) > currentTrick.get(winningCardIndex).getRanking(trump)) {
                    winningCardIndex = i;
                }
            }
        }
        return winningCardIndex;
    }

    /**
     * Scores the round based on the values stored in callingTeam and trickCount,
     * and updates the score array
     */
    public void scoreRound() {
        if (trickCount[callingTeam] < 3) {
            // Calling team got euchred, add 2 points to the other team
            scores[(callingTeam + 1)%2] += 2;
            messageToOutput.append("Team ")
                    .append(callingTeam)
                    .append(" got euchred, opposing team gets 2 points\n");
        } else if (trickCount[callingTeam] < 5) {
            // Calling team took 3-4 tricks, and get 1 point
            scores[callingTeam] += 1;
            messageToOutput.append("Team ")
                    .append(callingTeam)
                    .append(" took ")
                    .append(trickCount[callingTeam])
                    .append(" tricks, and get 1 point\n");
        } else if(lonerPlayer != null){
            //Calling team took all tricks and went alone, and get 4 points
            scores[callingTeam] += 4;
            messageToOutput.append("Team ")
                    .append(callingTeam)
                    .append(" went alone and took all tricks, and get 4 points\n");
        } else {
            // Calling team took all tricks, and get 2 points
            scores[callingTeam] += 2;
            messageToOutput.append("Team ")
                    .append(callingTeam)
                    .append(" took all tricks, and get 2 points\n");
        }
    }

    /**
     * @param move   The move to be made represented as a string
     * @param userID ID of the user making the move
     * @return true if successful, false otherwise
     */
    public boolean makeMove(String move, int userID) {
        if (userID != currentPlayer.playerID) {
            logger.debug("Player other then current player trying ot make a move");
            return false;
        }
        userResponse = move;
        return true;
    }

    /**
     * @return number of cards currently in the deck
     */
    public int getCardsInDeck() {
        return deck.size();
    }

    /**
     * Helper method to get the names of the playable cards
     *
     * @return A string array of the names of cards that can be played
     */
    private String[] getPlayableCardsInHand(){
        ArrayList<Card> validCards = getValidCards(currentPlayer.hand);
        String[] numberedIndexes = new String[validCards.size()];
        for (int cardIndex = 0; cardIndex < validCards.size(); cardIndex++) {
            numberedIndexes[cardIndex] = validCards.get(cardIndex).toString();
        }
        return numberedIndexes;
    }

    /**
     * @return the cards the current player has in their hand as a string array
     */
    private String[] getStringOfCardsInCurrentPlayerHand(){
        ArrayList<Card> hand = currentPlayer.hand;
        String[] handStrings = new String[hand.size()];

        for (int i = 0; i < hand.size(); i++) {
            handStrings[i] = hand.get(i).toString();
        }

        return handStrings;
    }

    /**
     * @param input the string value of the move player is trying to make
     */
    private void setMostRecentMove(String input){
        try {
            mostRecentMove = currentPlayer.hand.get(Integer.parseInt(input)).toString();

        } catch (NumberFormatException e) {
            mostRecentMove = input;
        }
    }

    /**
     * Handles when the code needs player input. Locks the thread until input is received via the websocket
     * @return The passed message
     */
    private String getInput(GamePhase newPhase, String[] newOptions){
        String result;
        currentPhase = newPhase;
        optionsForPlayer = newOptions;

        isWaitingForInput = true;

        if (currentPlayer.playerID < 0){
            botIsPlaying = true;

            switch (currentPhase){
                case NAME_TRUMP -> userResponse = botNameTrump(currentPlayer);
                case AGREE_TO_TRUMP -> userResponse = botPickUpCard(currentPlayer);
                case DISCARD_FOR_TRUMP -> userResponse = botDiscardCard(currentPlayer);
                case GO_ALONE -> userResponse = botChooseAlone(currentPlayer);
                case PLAY_CARD -> userResponse = botPlayCard(currentPlayer);
                default -> userResponse = "";
            }
        }

        while (isWaitingForInput || userResponse == null) {
            Thread.onSpinWait();
        }
        setMostRecentMove(userResponse);
        result = userResponse;
        userResponse = null;
        messageToOutput = new StringBuilder();
        return result;
    }

    public enum GamePhase {
        NAME_TRUMP,
        DISCARD_FOR_TRUMP,
        AGREE_TO_TRUMP,
        GO_ALONE,
        PLAY_CARD
    }

    /*
     * Bot method to determine if a bot player wants to pick up/order up the upCard
     */
    public String botPickUpCard(Player bot) {
        int currentPlayerIndex = Arrays.asList(players).indexOf(bot);
        int index = (dealerIndex + currentPlayerIndex) % 4 + 1;
        int strength = getHandStrength(bot.hand, upCard.getSuit());
        int upCardStrength = upCard.getPower(upCard.getSuit());
        if (index == 4) {
            // The bot is the dealer
            int maxStrength = 0;
            ArrayList<Card> handWithCard = bot.hand;
            handWithCard.add(upCard);
            for (int i = 0; i < 6; i++) {
                ArrayList<Card> handWithoutCard = new ArrayList<>(handWithCard);
                handWithoutCard.remove(i);
                int potentialStrength = getHandStrength(handWithoutCard, upCard.getSuit());
                if (potentialStrength > maxStrength) {
                    maxStrength = potentialStrength;
                }
            }
            if (maxStrength > 100) {
                return "Yes";
            } else {
                return "No";
            }
        } else if (index == 2) {
            // The bot's partner is the dealer
            if (strength + upCardStrength / 2 > 100) {
                return "Yes";
            } else {
                return "No";
            }
        } else {
            // The other team has the dealer
            if (strength - upCardStrength / 2 > 100) {
                return "Yes";
            } else {
                return "No";
            }
        }
    }

    public String botChooseAlone(Player bot) {
        if (getHandStrength(bot.hand, trump) > 130) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public String botDiscardCard(Player bot) {
        int maxStrength = 0;
        int discard = 0;
        for (int i = 0; i < 6; i++) {
            ArrayList<Card> handWithoutCard = new ArrayList<>(bot.hand);
            handWithoutCard.remove(i);
            int strength = getHandStrength(handWithoutCard, trump);
            if (strength > maxStrength) {
                maxStrength = strength;
                discard = i;
            }
        }
        return getStringOfCardsInCurrentPlayerHand()[discard];
    }

    public String botNameTrump(Player bot) {
        int bestStrength = 0;
        Card.Suit bestSuit = Card.Suit.CLUBS;
        ArrayList<Card.Suit> potentials = new ArrayList<Card.Suit>();
        for (Card card : bot.hand) {
            if (!potentials.contains(card.getSuit())) {
                potentials.add(card.getSuit());
            }
        }
        potentials.remove(upCard.getSuit());
        for (Card.Suit trump : potentials) {
            if (trump.equals(upCard.getSuit())) {
                continue;
            }
            int strength = getHandStrength(bot.hand, trump);
            if (strength > bestStrength) {
                bestStrength = strength;
                bestSuit = trump;
            }
        }
        if (bestStrength < 100) {
            return "Pass";
        } else {
            return bestSuit.toString();
        }
    }

    public String botPlayCard(Player bot) {
        // TODO
        return getPlayableCardsInHand()[0];
    }

    /**
     * Helper method for the bot that returns the currently-winning card in the
     * trick (TODO)
     *
     * @return the card that is currently winning the trick
     */
    // private Card winningCard() {
    //     // for(Card card)
    //     return null
    // }

    /**
     * Helper method that analyzes a hand to get a rough estimate of how strong it
     * is given a trump suit, used for determining bot moves
     *
     * @param hand the hand to analyze
     * @return An integer representing the strength of the given hand
     *
     */
    private int getHandStrength(ArrayList<Card> hand, Card.Suit trump) {
        int strength = 0;
        int numSuits = 0;
        ArrayList<Card.Suit> suits = new ArrayList<Card.Suit>();

        for (Card card : hand) {
            strength += card.getPower(trump);
            if (!suits.contains(card.getSuit(trump))) {
                suits.add(card.getSuit(trump));
                numSuits++;
            }
        }
        switch (numSuits) {
            case 1:
                strength += 15;
            case 2:
                strength += 10;
            case 3:
                strength += 5;
            case 4:
                strength += 0;
        }
        return strength;
    }
}
