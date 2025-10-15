import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server implements GameInterface {

    private GameState gameState;
    private BackupInterface backupStub; // RMI stub for the backup server

    /**
     * Default constructor for starting a brand new game server.
     */
    public Server() {
        this.gameState = new GameState();
        initializeGame();
        // NOTE: Connection to backup is now handled in the main method after startup.
        System.out.println("New Server instance created.");
    }

    /**
     * Constructor used when a backup server is promoted to primary.
     */
    public Server(GameState restoredState) {
        this.gameState = restoredState;
        // The newly promoted primary also needs to find the backup if one exists.
        connectToBackup();
        System.out.println("Server instance created from a restored state.");
    }

    // --- Game Logic Methods (No changes here) ---

    private void initializeGame() {
        createDeck();
        shuffleDeck();
    }

    private void createDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        for (String suit : suits) {
            for (String rank : ranks) {
                gameState.deck.add(rank + " of " + suit);
            }
        }
    }

    private void shuffleDeck() {
        Collections.shuffle(gameState.deck);
    }
    // ... Add your other helper methods like dealCard(), calculateHandValue(), etc. here ...
    // Make sure they all operate on the gameState object.


    // --- RMI Interface Methods ---

    @Override
    public GameState placeBet(int bet) throws RemoteException {
        System.out.println("Received bet: " + bet);
        gameState.playerBet = bet;
        gameState.statusMessage = "Bet of " + bet + " placed. Your turn.";
        syncWithBackup(); // Replicate state after the action
        return gameState;
    }

    @Override
    public GameState hit() throws RemoteException {
        System.out.println("Player hits.");
        // Your logic to deal a card to the player
        gameState.statusMessage = "Player hits. Current hand value is X.";
        syncWithBackup(); // Replicate state after the action
        return gameState;
    }

    @Override
    public GameState stand() throws RemoteException {
        System.out.println("Player stands.");
        gameState.isPlayerTurn = false;
        // Your logic for the dealer's turn and determining the winner
        gameState.statusMessage = "Player stands. Dealer's turn.";
        syncWithBackup(); // Replicate state after the action
        return gameState;
    }

    @Override
    public GameState getGameState() throws RemoteException {
        return this.gameState;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }


    // --- Replication Helper (FIXED) ---

    /**
     * Connects to the backup server, retrying a few times to handle startup delays.
     */
    private void connectToBackup() {
        // This method will now be called AFTER the server is bound to the registry.
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1000); // Wait 1 second before trying
                Registry registry = LocateRegistry.getRegistry("localhost");
                this.backupStub = (BackupInterface) registry.lookup("BlackjackBackup");
                System.out.println("✅ Successfully connected to Backup server.");
                return; // Exit the loop on success
            } catch (Exception e) {
                System.err.println("Attempt " + (i + 1) + ": Could not connect to backup server. Retrying...");
            }
        }
        System.err.println("Failed to connect to backup. Will operate without replication.");
        this.backupStub = null;
    }

    private void syncWithBackup() {
        if (this.backupStub != null) {
            try {
                this.backupStub.updateState(this.gameState);
            } catch (RemoteException e) {
                System.err.println("Failed to sync with backup. It may have gone down.");
                connectToBackup(); // Attempt to reconnect
            }
        }
    }

    /**
     * Main method specifically for the Primary/Backup fault tolerance test.
     */
    public static void main(String[] args) {
        String serverName = "BlackjackPrimary";
        try {
            Server server = new Server();
            GameInterface stub = (GameInterface) UnicastRemoteObject.exportObject(server, 0);

            // Step 1: Create the RMI registry
            Registry registry = LocateRegistry.createRegistry(1099);
            System.out.println("RMI Registry created by Primary Server.");

            // Step 2: Bind the primary server stub to the registry
            registry.bind(serverName, stub);
            System.out.println("Primary Server '" + serverName + "' is ready.");

            // Step 3: NOW, attempt to connect to the backup.
            // This ensures the primary is fully registered before it looks for the backup.
            server.connectToBackup();

        } catch (Exception e) {
            System.err.println("Primary Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
