import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the entire state of a single Blackjack game.
 * Must be Serializable to be sent across the network between servers.
 */
public class GameState implements Serializable {
    // A unique ID for serialization compatibility
    private static final long serialVersionUID = 1L;

    public List<String> deck;
    public List<String> playerHand;
    public List<String> dealerHand;
    public int playerBet;
    public boolean isPlayerTurn;
    public String statusMessage;

    // Constructor to initialize a new game state
    public GameState() {
        this.deck = new ArrayList<>();
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
        this.playerBet = 0;
        this.isPlayerTurn = true; // Player always starts
        this.statusMessage = "New game started. Please place your bet.";
    }
}
