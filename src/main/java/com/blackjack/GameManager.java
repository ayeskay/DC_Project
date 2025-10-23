package com.blackjack;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final File stateFile;

    public GameManager(File stateFile) {
        this.stateFile = stateFile;
        loadGames(); // Load existing games from file on startup
    }

    private void initializeDeck(GameState gs) {
        gs.deck.clear();
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};

        for (String suit : suits) {
            for (String rank : ranks) {
                gs.deck.add(new Card(suit, rank, getCardValue(rank)));
            }
        }
        Collections.shuffle(gs.deck);
    }

    private int getCardValue(String rank) {
        switch (rank) {
            case "Jack":
            case "Queen":
            case "King":
                return 10;
            case "Ace":
                return 11; // Handled as 1 or 11 in GameState.getHandValue
            default:
                return Integer.parseInt(rank);
        }
    }

    public GameState createNewGame() {
        GameState gs = new GameState();
        initializeDeck(gs);
        gs.isPlayerTurn = true; // Ready for bet
        gs.statusMessage = "New game started. Please place your bet.";
        games.put(gs.getId(), gs);
        saveGames(); // Persist new game
        return gs;
    }

    public GameState getGameState(String gameId) {
        return games.get(gameId);
    }

    public GameState placeBet(String gameId, int bet) {
        GameState gs = games.get(gameId);
        if (gs == null || bet <= 0) return null;
        gs.playerBet = bet;

        // Deal initial hands
        gs.playerHand.add(gs.deck.remove(0));
        gs.dealerHand.add(gs.deck.remove(0));
        gs.playerHand.add(gs.deck.remove(0));
        gs.dealerHand.add(gs.deck.remove(0));

        int playerValue = gs.getPlayerValue();
        if (playerValue == 21) {
            gs.statusMessage = "Blackjack! Player stands.";
            gs.isPlayerTurn = false; // Auto-stand
            return stand(gameId); // Proceed directly to dealer's turn
        } else {
            gs.statusMessage = "Bet placed: " + bet + ". Your turn. (hit/stand)";
            gs.isPlayerTurn = true;
        }
        saveGames();
        return gs;
    }

    public GameState hit(String gameId) {
        GameState gs = games.get(gameId);
        if (gs == null || !gs.isPlayerTurn) return gs; // Can't hit if not player's turn

        gs.playerHand.add(gs.deck.remove(0));
        int playerValue = gs.getPlayerValue();

        if (playerValue > 21) {
            gs.statusMessage = "Player busts! You lose. Final hand: " + gs.playerHand + " (Value: " + playerValue + ")";
            gs.isPlayerTurn = false; // Game over
        } else if (playerValue == 21) {
            gs.statusMessage = "Player has 21! Dealer's turn.";
            gs.isPlayerTurn = false; // Auto-stand
            saveGames();
            return stand(gameId); // Proceed to dealer's turn
        } else {
            gs.statusMessage = "Player hit. Hand: " + gs.playerHand + " (Value: " + playerValue + ")";
        }
        saveGames();
        return gs;
    }

    public GameState stand(String gameId) {
        GameState gs = games.get(gameId);
        if (gs == null) return null;

        // Prevent multiple 'stand' calls from re-playing dealer turn
        if (!gs.isPlayerTurn) {
             // If game is already over, just return the state
            if(gs.statusMessage.contains("wins") || gs.statusMessage.contains("lose") || gs.statusMessage.contains("push")) {
                return gs;
            }
        }

        gs.isPlayerTurn = false;
        int playerValue = gs.getPlayerValue();
        int dealerValue = gs.getDealerValue();

        // Dealer plays
        while (dealerValue < 17) {
            if (gs.deck.isEmpty()) {
                 gs.statusMessage = "Deck is empty! Cannot complete dealer's turn.";
                 break;
            }
            gs.dealerHand.add(gs.deck.remove(0));
            dealerValue = gs.getDealerValue();
        }

        // Determine winner
        gs.statusMessage = determineWinner(playerValue, dealerValue, gs.dealerHand);
        saveGames();
        return gs;
    }

    private String determineWinner(int playerValue, int dealerValue, List<Card> dealerHand) {
        String finalHand = ". Dealer hand: " + dealerHand + " (Value: " + dealerValue + ")";
        
        // Player already busted (this case is handled in hit())
        if (playerValue > 21) {
            return "Player busts! You lose" + finalHand;
        }
        // Dealer busts
        if (dealerValue > 21) {
            return "Dealer busts! You win!" + finalHand;
        }
        // Compare scores
        if (playerValue > dealerValue) {
            return "Player wins! " + playerValue + " to " + dealerValue + finalHand;
        } else if (dealerValue > playerValue) {
            return "Dealer wins! " + dealerValue + " to " + playerValue + finalHand;
        } else {
            return "It's a push (tie)! Both have " + playerValue + finalHand;
        }
    }

    /**
     * Called by ReplicationService on follower nodes to update the state.
     */
    public void setGameState(GameState state) {
        if (state != null) {
            games.put(state.getId(), state);
            saveGames(); // Persist the replicated state
        }
    }

    /**
     * Saves the entire map of games to the state file.
     */
    private synchronized void saveGames() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stateFile))) {
            oos.writeObject(games);
        } catch (Exception e) {
            System.err.println("CRITICAL: Could not save state file: " + e.getMessage());
        }
    }

    /**
     * Loads the game map from the state file on startup.
     */
    private void loadGames() {
        if (stateFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(stateFile))) {
                Object obj = ois.readObject();
                if (obj instanceof Map) {
                    // Type-safe cast
                    @SuppressWarnings("unchecked")
                    Map<String, GameState> loadedGames = (Map<String, GameState>) obj;
                    games.putAll(loadedGames);
                    System.out.println("[GameManager] Loaded " + games.size() + " existing games from " + stateFile.getName());
                }
            } catch (Exception e) {
                System.err.println("[GameManager] Could not load state file " + stateFile.getName() + ": " + e.getMessage());
            }
        }
    }
}