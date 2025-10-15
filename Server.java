import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;

public class Server implements GameInterface {

    private GameState gameState;
    private BackupInterface backupStub;

    public Server() {
        this.gameState = new GameState();
        initializeGame();
        System.out.println("New Server instance created.");
    }

    public Server(GameState restoredState) {
        this.gameState = restoredState;
        System.out.println("Server instance created from a restored state.");
    }

    // --- Game Logic (No changes) ---
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

    // --- RMI Methods (No changes) ---
    @Override
    public GameState placeBet(int bet) throws RemoteException {
        System.out.println("Received bet: " + bet);
        gameState.playerBet = bet;
        gameState.statusMessage = "Bet of " + bet + " placed. Your turn.";
        syncWithBackup();
        return gameState;
    }
    @Override
    public GameState hit() throws RemoteException {
        System.out.println("Player hits.");
        gameState.statusMessage = "Player hits. Current hand value is X.";
        syncWithBackup();
        return gameState;
    }
    @Override
    public GameState stand() throws RemoteException {
        System.out.println("Player stands.");
        gameState.isPlayerTurn = false;
        gameState.statusMessage = "Player stands. Dealer's turn.";
        syncWithBackup();
        return gameState;
    }
    @Override
    public GameState getGameState() throws RemoteException { return this.gameState; }
    @Override
    public boolean ping() throws RemoteException { return true; }

    // --- Replication Helper ---
    private void connectToBackup() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            this.backupStub = (BackupInterface) registry.lookup("BlackjackBackup");
            System.out.println("✅ Successfully connected to Backup server.");
        } catch (Exception e) {
            System.err.println("Could not connect to backup server. It may not be running.");
            this.backupStub = null;
        }
    }
    private void syncWithBackup() {
        if (this.backupStub != null) {
            try {
                this.backupStub.updateState(this.gameState);
            } catch (RemoteException e) {
                System.err.println("Failed to sync with backup.");
            }
        }
    }

    // --- Main Method (FIXED) ---
    public static void main(String[] args) {
        // This is for the Primary/Backup fault tolerance test.
        String serverName = "BlackjackPrimary";
        try {
            Server server = new Server();
            GameInterface stub = (GameInterface) UnicastRemoteObject.exportObject(server, 0);

            // FIXED: We now GET the registry, we DO NOT create it.
            Registry registry = LocateRegistry.getRegistry("localhost");
            registry.bind(serverName, stub);
            System.out.println("Primary Server '" + serverName + "' is ready.");

            // Connect to backup after successfully registering.
            server.connectToBackup();

        } catch (Exception e) {
            System.err.println("Primary Server exception: " + e.getMessage());
            System.err.println("Ensure rmiregistry is running in the project directory!");
            e.printStackTrace();
        }
    }
}
