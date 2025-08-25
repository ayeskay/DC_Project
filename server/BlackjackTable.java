import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BlackjackTable {
    private String tableName;
    private List<Card> deck;
    private Map<Integer, List<Card>> playerHands;
    private List<Card> dealerHand;
    private Map<Integer, String> gameStatus;
    private Map<Integer, Integer> playerBets;
    private Map<Integer, Player> players;
    private int minBet;
    private int maxBet;
    private static final AtomicInteger sessionIdGenerator = new AtomicInteger(1000);
    private Set<Integer> activeSessions;
    
    public BlackjackTable(String tableName, int minBet, int maxBet) {
        this.tableName = tableName;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.playerHands = new ConcurrentHashMap<>();
        this.gameStatus = new ConcurrentHashMap<>();
        this.playerBets = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.activeSessions = ConcurrentHashMap.newKeySet();
        this.dealerHand = new ArrayList<>();
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public int getMinBet() {
        return minBet;
    }
    
    public int getMaxBet() {
        return maxBet;
    }
    
    public int getPlayerCount() {
        return activeSessions.size();
    }
    
    public synchronized int addPlayer(Player player) throws RemoteException {
        int sessionId = sessionIdGenerator.getAndIncrement();
        player.setSessionId(sessionId);
        players.put(sessionId, player);
        activeSessions.add(sessionId);
        gameStatus.put(sessionId, "Waiting for bet");
        return sessionId;
    }
    
    public synchronized void removePlayer(int sessionId) {
        activeSessions.remove(sessionId);
        playerHands.remove(sessionId);
        gameStatus.remove(sessionId);
        playerBets.remove(sessionId);
        players.remove(sessionId);
    }
    
    public List<String> getPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Player player : players.values()) {
            playerNames.add(player.getName() + " (" + player.getPoints() + " pts)");
        }
        return playerNames;
    }
    
    public synchronized void startNewGame(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        
        Player player = players.get(sessionId);
        if (player == null) {
            throw new RemoteException("Player not found");
        }
        
        // Check if player has placed a bet
        if (!playerBets.containsKey(sessionId) || playerBets.get(sessionId) <= 0) {
            gameStatus.put(sessionId, "Place your bet first");
            return;
        }
        
        // Check if player has enough points
        int bet = playerBets.get(sessionId);
        if (player.getPoints() < bet) {
            gameStatus.put(sessionId, "Not enough points for this bet");
            return;
        }
        
        // Deduct bet from player's points
        player.deductPoints(bet);
        
        // Create new deck if needed
        if (deck == null || deck.size() < 10) {
            createAndShuffleDeck();
        }
        
        // Initialize hands
        List<Card> playerHand = new ArrayList<>();
        dealerHand = new ArrayList<>();
        
        playerHand.add(deck.remove(0));
        playerHand.add(deck.remove(0));
        dealerHand.add(deck.remove(0));
        dealerHand.add(deck.remove(0));
        
        playerHands.put(sessionId, playerHand);
        gameStatus.put(sessionId, "Your turn. Hit or Stand?");
    }
    
    private void createAndShuffleDeck() {
        deck = new ArrayList<>();
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        int[] values = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};

        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                deck.add(new Card(suit, ranks[i], values[i]));
            }
        }
        Collections.shuffle(deck);
    }
    
    private int getHandValue(List<Card> hand) {
        int value = 0;
        int aceCount = 0;
        for (Card card : hand) {
            value += card.getValue();
            if (card.getValue() == 11) aceCount++;
        }
        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }
        return value;
    }
    
    public synchronized void placeBet(int sessionId, int amount) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        
        Player player = players.get(sessionId);
        if (player == null) {
            throw new RemoteException("Player not found");
        }
        
        if (amount < minBet || amount > maxBet) {
            throw new RemoteException("Bet must be between " + minBet + " and " + maxBet);
        }
        
        if (player.getPoints() < amount) {
            throw new RemoteException("Not enough points");
        }
        
        playerBets.put(sessionId, amount);
        gameStatus.put(sessionId, "Bet placed. Start game when ready.");
    }
    
    public synchronized void hit(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        
        String status = gameStatus.get(sessionId);
        if (status == null || !status.contains("Your turn")) {
            return;
        }
        
        List<Card> playerHand = playerHands.get(sessionId);
        if (playerHand == null) {
            throw new RemoteException("No active game");
        }
        
        playerHand.add(deck.remove(0));
        if (getHandValue(playerHand) > 21) {
            gameStatus.put(sessionId, "You busted! Dealer wins.");
            resolveGame(sessionId);
        }
    }
    
    public synchronized void stand(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        
        String status = gameStatus.get(sessionId);
        if (status == null || !status.contains("Your turn")) {
            return;
        }
        
        // Dealer plays
        while (getHandValue(dealerHand) < 17) {
            dealerHand.add(deck.remove(0));
        }
        
        resolveGame(sessionId);
    }
    
    private void resolveGame(int sessionId) {
        Player player = players.get(sessionId);
        if (player == null) return;
        
        int playerValue = getHandValue(playerHands.get(sessionId));
        int dealerValue = getHandValue(dealerHand);
        int bet = playerBets.get(sessionId);
        
        if (playerValue > 21) {
            // Player already busted
            gameStatus.put(sessionId, "You busted! Dealer wins.");
        } else if (dealerValue > 21) {
            gameStatus.put(sessionId, "Dealer busted! You win!");
            // Blackjack pays 3:2
            if (playerValue == 21 && playerHands.get(sessionId).size() == 2) {
                player.addPoints((int) (bet * 2.5));
            } else {
                player.addPoints(bet * 2);
            }
        } else if (playerValue > dealerValue) {
            gameStatus.put(sessionId, "You win!");
            // Blackjack pays 3:2
            if (playerValue == 21 && playerHands.get(sessionId).size() == 2) {
                player.addPoints((int) (bet * 2.5));
            } else {
                player.addPoints(bet * 2);
            }
        } else if (playerValue < dealerValue) {
            gameStatus.put(sessionId, "Dealer wins.");
        } else {
            gameStatus.put(sessionId, "It's a push (tie).");
            player.addPoints(bet); // Return bet
        }
    }
    
    public List<Card> getPlayerHand(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        return playerHands.get(sessionId);
    }
    
    public List<Card> getDealerHand(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        
        String status = gameStatus.get(sessionId);
        if (status != null && status.contains("Your turn")) {
            List<Card> hiddenHand = new ArrayList<>();
            if (!dealerHand.isEmpty()) {
                hiddenHand.add(dealerHand.get(0));
                hiddenHand.add(new Card("?", "Hidden", 0));
            }
            return hiddenHand;
        }
        return dealerHand;
    }
    
    public String getGameStatus(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            return "Invalid session";
        }
        return gameStatus.getOrDefault(sessionId, "Unknown status");
    }
    
    public int getPlayerPoints(int sessionId) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        Player player = players.get(sessionId);
        return player != null ? player.getPoints() : 0;
    }
    
    public void buyIn(int sessionId, int points) throws RemoteException {
        if (!activeSessions.contains(sessionId)) {
            throw new RemoteException("Invalid session");
        }
        Player player = players.get(sessionId);
        if (player != null) {
            player.addPoints(points);
        }
    }
}